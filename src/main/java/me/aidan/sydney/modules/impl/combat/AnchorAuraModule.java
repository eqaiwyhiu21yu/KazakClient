package me.aidan.sydney.modules.impl.combat;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.events.SubscribeEvent;
import me.aidan.sydney.events.impl.PlayerUpdateEvent;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import me.aidan.sydney.settings.impl.BooleanSetting;
import me.aidan.sydney.settings.impl.NumberSetting;
import me.aidan.sydney.utils.minecraft.InventoryUtils;
import me.aidan.sydney.utils.minecraft.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "AnchorAura", description = "Places and detonates respawn anchors near enemies.", category = Module.Category.COMBAT)
public class AnchorAuraModule extends Module {
    public NumberSetting range = new NumberSetting("Range", "Maximum distance to target.", 5.0, 1.0, 7.0);
    public NumberSetting delay = new NumberSetting("Delay", "Ticks between cycles.", 3, 0, 10);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Rotates towards placements.", true);
    public BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Disables when no target found.", false);
    public BooleanSetting place = new BooleanSetting("Place", "Places anchors if none exist near target.", true);
    public BooleanSetting charge = new BooleanSetting("Charge", "Charges anchors with glowstone.", true);

    private int ticks = 0;
    private enum Phase { PLACE_ANCHOR, CHARGE, DETONATE, DONE }
    private Phase phase = Phase.DONE;
    private BlockPos anchorPos = null;
    private PlayerEntity currentTarget = null;
    private int charges = 0;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { setToggled(false); return; }
        ticks = 0; phase = Phase.DONE; anchorPos = null; currentTarget = null; charges = 0;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.world.getDimension().hasFixedTime()) return;

        PlayerEntity target = getTarget();
        if (target == null) { phase = Phase.DONE; currentTarget = null;
            if (autoDisable.getValue()) setToggled(false); return; }

        if (ticks < delay.getValue().intValue()) { ticks++; return; }
        ticks = 0;

        if (phase == Phase.DONE || currentTarget != target) {
            currentTarget = target;
            anchorPos = findAnchorPos(target);
            if (anchorPos == null) {
                if (place.getValue()) anchorPos = findPlacePos(target);
                if (anchorPos == null) return;
                phase = Phase.PLACE_ANCHOR;
            } else {
                charges = mc.world.getBlockState(anchorPos).get(RespawnAnchorBlock.CHARGES);
                if (charges == 0 && charge.getValue()) phase = Phase.CHARGE;
                else phase = Phase.DETONATE;
            }
        }

        switch (phase) {
            case PLACE_ANCHOR -> {
                if (anchorPos == null) { phase = Phase.DONE; return; }
                int slot = InventoryUtils.find(Items.RESPAWN_ANCHOR, 0, 8);
                if (slot == -1) { phase = Phase.DONE; return; }
                int prev = mc.player.getInventory().selectedSlot;
                InventoryUtils.switchSlot("Silent", slot, prev);
                Direction dir = WorldUtils.getClosestDirection(anchorPos, true);
                WorldUtils.placeBlock(anchorPos, dir, Hand.MAIN_HAND, rotate.getValue(), false, false);
                InventoryUtils.switchBack("Silent", slot, prev);
                phase = charge.getValue() ? Phase.CHARGE : Phase.DETONATE;
            }
            case CHARGE -> {
                if (anchorPos == null) { phase = Phase.DONE; return; }
                int gSlot = InventoryUtils.find(Items.GLOWSTONE, 0, 8);
                if (gSlot == -1) { phase = Phase.DETONATE; return; }
                int prev = mc.player.getInventory().selectedSlot;
                InventoryUtils.switchSlot("Silent", gSlot, prev);
                interactBlock(anchorPos);
                InventoryUtils.switchBack("Silent", gSlot, prev);
                charges++;
                if (charges >= 1) phase = Phase.DETONATE;
            }
            case DETONATE -> {
                if (anchorPos == null) { phase = Phase.DONE; return; }
                if (mc.player.getMainHandStack().isEmpty()) return;
                interactBlock(anchorPos);
                phase = Phase.DONE;
            }
            case DONE -> {}
        }
    }

    private void interactBlock(BlockPos pos) {
        float[] rots = getRotations(Vec3d.ofCenter(pos));
        if (rotate.getValue()) Sydney.ROTATION_MANAGER.packetRotate(rots[0], rots[1]);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
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

    private BlockPos findPlacePos(PlayerEntity target) {
        BlockPos feet = BlockPos.ofFloored(target.getPos());
        BlockPos[] checks = {feet.down(), feet.north(), feet.south(), feet.east(), feet.west(), feet};
        for (BlockPos pos : checks) {
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MathHelper.square(range.getValue().doubleValue())) continue;
            if (mc.world.getBlockState(pos).isSolid() && mc.world.getBlockState(pos.up()).isAir()) return pos.up();
            if (pos.getY() > mc.world.getBottomY() && mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.down()).isSolid()) return pos;
        }
        return null;
    }

    private BlockPos findAnchorPos(PlayerEntity target) {
        BlockPos feet = BlockPos.ofFloored(target.getPos());
        BlockPos[] checks = {feet, feet.up(), feet.north(), feet.south(), feet.east(), feet.west(), feet.down()};
        for (BlockPos pos : checks) {
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MathHelper.square(range.getValue().doubleValue())) continue;
            if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) return pos;
        }
        return null;
    }

    private float[] getRotations(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);
        return new float[]{(float) Math.toDegrees(Math.atan2(dz, dx)) - 90f, (float) -Math.toDegrees(Math.atan2(dy, hd))};
    }
}
