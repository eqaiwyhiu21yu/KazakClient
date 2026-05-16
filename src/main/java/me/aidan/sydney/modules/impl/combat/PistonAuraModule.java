package me.aidan.sydney.modules.impl.combat;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.events.SubscribeEvent;
import me.aidan.sydney.events.impl.PlayerUpdateEvent;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import me.aidan.sydney.settings.impl.BooleanSetting;
import me.aidan.sydney.settings.impl.NumberSetting;
import me.aidan.sydney.utils.minecraft.InventoryUtils;
import me.aidan.sydney.utils.minecraft.NetworkUtils;
import me.aidan.sydney.utils.minecraft.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "PistonAura", description = "Pushes crystals into enemies using pistons.", category = Module.Category.COMBAT)
public class PistonAuraModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "Max target distance.", 5.0, 1.0, 7.0);
    public NumberSetting delay = new NumberSetting("Delay", "Ticks between cycles.", 2, 0, 10);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Rotate towards placements.", true);
    public BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disable when no target.", false);

    private int ticks = 0;
    private enum Phase { FIND, PLACE_CRYSTAL, PLACE_PISTON, PLACE_REDSTONE, BREAK_CRYSTAL, DONE }
    private Phase phase = Phase.DONE;
    private BlockPos targetObsidian = null;
    private BlockPos pistonPos = null;
    private Direction pistonFace = null;
    private BlockPos crystalPos = null;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { setToggled(false); return; }
        ticks = 0; phase = Phase.FIND;
        targetObsidian = null; pistonPos = null; pistonFace = null; crystalPos = null;
    }

    @Override
    public void onDisable() { phase = Phase.DONE; }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        PlayerEntity target = getTarget();
        if (target == null) { phase = Phase.DONE; if (autoDisable.getValue()) setToggled(false); return; }
        if (ticks < delay.getValue().intValue()) { ticks++; return; }
        ticks = 0;

        Vec3d targetEye = target.getEyePos();
        BlockPos targetBlock = BlockPos.ofFloored(target.getPos());

        if (phase == Phase.FIND) {
            BlockPos[] dirs = {
                    targetBlock.north(), targetBlock.south(), targetBlock.east(), targetBlock.west(),
                    targetBlock.add(1,0,1), targetBlock.add(-1,0,1), targetBlock.add(1,0,-1), targetBlock.add(-1,0,-1)
            };
            for (BlockPos obsidianPos : dirs) {
                if (mc.player.squaredDistanceTo(Vec3d.ofCenter(obsidianPos)) > MathHelper.square(range.getValue().doubleValue())) continue;
                if (mc.world.getBlockState(obsidianPos).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(obsidianPos).getBlock() != Blocks.BEDROCK) continue;
                if (!mc.world.getBlockState(obsidianPos.up()).isAir()) continue;
                Direction toTarget = getFacing(obsidianPos, targetBlock);
                if (toTarget == null) continue;
                BlockPos pPos = obsidianPos.offset(toTarget.getOpposite());
                if (!WorldUtils.isPlaceable(pPos)) continue;
                if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pPos)) > MathHelper.square(range.getValue().doubleValue())) continue;
                targetObsidian = obsidianPos;
                pistonPos = pPos;
                pistonFace = toTarget;
                crystalPos = obsidianPos.up();
                phase = Phase.PLACE_CRYSTAL;
                break;
            }
            if (phase == Phase.FIND) phase = Phase.DONE;
            return;
        }

        switch (phase) {
            case PLACE_CRYSTAL -> {
                int slot = InventoryUtils.find(Items.END_CRYSTAL, 0, 8);
                if (slot == -1) { phase = Phase.DONE; return; }
                int prev = mc.player.getInventory().selectedSlot;
                InventoryUtils.switchSlot("Silent", slot, prev);
                Direction dir = WorldUtils.getClosestDirection(targetObsidian, true);
                Vec3d hitVec = WorldUtils.getHitVector(targetObsidian, dir);
                float[] rots = getRotations(Vec3d.ofCenter(targetObsidian));
                if (rotate.getValue()) Sydney.ROTATION_MANAGER.packetRotate(rots[0], rots[1]);
                NetworkUtils.sendSequencedPacket(seq -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(hitVec, dir, targetObsidian, false), seq));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                InventoryUtils.switchBack("Silent", slot, prev);
                phase = Phase.PLACE_PISTON;
            }
            case PLACE_PISTON -> {
                if (!WorldUtils.isPlaceable(pistonPos)) { phase = Phase.DONE; return; }
                int slot = InventoryUtils.find(Items.PISTON, 0, 8);
                if (slot == -1) slot = InventoryUtils.find(Items.STICKY_PISTON, 0, 8);
                if (slot == -1) { phase = Phase.DONE; return; }
                int prev = mc.player.getInventory().selectedSlot;
                InventoryUtils.switchSlot("Silent", slot, prev);
                Direction placeDir = pistonFace.getOpposite();
                BlockPos against = pistonPos.offset(placeDir);
                WorldUtils.placeBlock(pistonPos, placeDir, Hand.MAIN_HAND, rotate.getValue(), false, false);
                InventoryUtils.switchBack("Silent", slot, prev);
                phase = Phase.PLACE_REDSTONE;
            }
            case PLACE_REDSTONE -> {
                BlockPos rsPos = pistonPos.offset(pistonFace.getOpposite());
                if (!mc.world.getBlockState(rsPos).isAir() || mc.player.squaredDistanceTo(Vec3d.ofCenter(rsPos)) > MathHelper.square(range.getValue().doubleValue())) {
                    phase = Phase.BREAK_CRYSTAL;
                    return;
                }
                int rsSlot = InventoryUtils.find(Items.REDSTONE_BLOCK, 0, 8);
                if (rsSlot == -1) { phase = Phase.BREAK_CRYSTAL; return; }
                if (WorldUtils.isPlaceable(rsPos)) {
                    int prev = mc.player.getInventory().selectedSlot;
                    InventoryUtils.switchSlot("Silent", rsSlot, prev);
                    Direction rsFace = getFacing(rsPos, pistonPos);
                    if (rsFace != null) {
                        WorldUtils.placeBlock(rsPos, rsFace.getOpposite(), Hand.MAIN_HAND, rotate.getValue(), false, false);
                    }
                    InventoryUtils.switchBack("Silent", rsSlot, prev);
                }
                phase = Phase.BREAK_CRYSTAL;
            }
            case BREAK_CRYSTAL -> {
                for (Entity entity : mc.world.getEntities()) {
                    if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive()) continue;
                    if (crystal.getBlockPos().down().equals(targetObsidian)) {
                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        break;
                    }
                }
                phase = Phase.DONE;
            }
            case DONE -> {}
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (Sydney.FRIEND_MANAGER.contains(p.getName().getString())) continue;
            double d = mc.player.squaredDistanceTo(p);
            if (d > MathHelper.square(range.getValue().doubleValue())) continue;
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    private Direction getFacing(BlockPos from, BlockPos to) {
        Vec3d diff = Vec3d.of(to).subtract(Vec3d.of(from));
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ax >= ay && ax >= az) return diff.x > 0 ? Direction.EAST : Direction.WEST;
        if (az >= ay) return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
        return diff.y > 0 ? Direction.UP : Direction.DOWN;
    }

    private float[] getRotations(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);
        return new float[]{(float) Math.toDegrees(Math.atan2(dz, dx)) - 90f, (float) -Math.toDegrees(Math.atan2(dy, hd))};
    }
}
