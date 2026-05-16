package me.aidan.sydney.modules.impl.player;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.events.SubscribeEvent;
import me.aidan.sydney.events.impl.PlayerUpdateEvent;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import me.aidan.sydney.settings.impl.BooleanSetting;
import me.aidan.sydney.settings.impl.NumberSetting;
import me.aidan.sydney.utils.minecraft.InventoryUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "AutoLavaCaster", description = "Creates lavacast mountains by pouring lava and water down staircases.", category = Module.Category.PLAYER)
public class AutoLavaCasterModule extends Module {
    public NumberSetting lavaFlowSeconds = new NumberSetting("LavaFlowSeconds", "How many seconds to let lava flow before pickup.", 30, 5, 300);
    public NumberSetting waterDelayTicks = new NumberSetting("WaterDelayTicks", "Ticks between lava pickup and water placement.", 65, 10, 200);
    public NumberSetting pickupDelayTicks = new NumberSetting("PickupDelayTicks", "Ticks between water placement and pickup.", 30, 10, 100);
    public NumberSetting reach = new NumberSetting("Reach", "Your reach distance for interacting.", 4.5, 2.0, 6.0);
    public BooleanSetting increaseY = new BooleanSetting("IncreaseY", "Places blocks and moves up after each layer.", true);
    public BooleanSetting autoPickupWater = new BooleanSetting("AutoPickupWater", "Picks up water after each layer.", true);
    public BooleanSetting sneak = new BooleanSetting("Sneak", "Holds sneak key while casting.", true);

    private enum Phase { IDLE, PLACE_LAVA, WAIT_LAVA, PICKUP_LAVA, WAIT_WATER_PLACE, PLACE_WATER, WAIT_WATER_PICKUP, PICKUP_WATER, BUILD_UP }
    private Phase phase = Phase.IDLE;
    private int ticks = 0;
    private int lavaSecondsLeft = 0;
    private BlockPos castPos = null;
    private int layer = 0;
    private int buildStep = 0;
    private float targetYaw, targetPitch;
    private static final BlockPos[] PLUS_SHAPE = {
            new BlockPos(0, 1, 0),
            new BlockPos(1, 1, 0),
            new BlockPos(-1, 1, 0),
            new BlockPos(0, 1, 1),
            new BlockPos(0, 1, -1)
    };

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) { setToggled(false); return; }
        HitResult hit = mc.getCameraEntity().raycast(reach.getValue().doubleValue(), 0, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) {
            Sydney.CHAT_MANAGER.tagged("Aim at a block to start casting.", getName());
            setToggled(false); return;
        }
        castPos = blockHit.getBlockPos();
        if (mc.world.getBlockState(castPos).isAir()) {
            Sydney.CHAT_MANAGER.tagged("Target block is air. Aim at a solid block.", getName());
            setToggled(false); return;
        }
        if (InventoryUtils.find(Items.LAVA_BUCKET, 0, 8) == -1) {
            Sydney.CHAT_MANAGER.tagged("No lava bucket in hotbar.", getName());
            setToggled(false); return;
        }
        if (InventoryUtils.find(Items.WATER_BUCKET, 0, 8) == -1) {
            Sydney.CHAT_MANAGER.tagged("No water bucket in hotbar.", getName());
            setToggled(false); return;
        }
        phase = Phase.PLACE_LAVA;
        ticks = 0;
        lavaSecondsLeft = lavaFlowSeconds.getValue().intValue();
        layer = 1;
        buildStep = 0;
        updateRotations(castPos);
        if (sneak.getValue()) mc.options.sneakKey.setPressed(true);
        mc.player.setVelocity(0, 0, 0);
        Sydney.CHAT_MANAGER.tagged("Starting layer 1. Lava will flow for " + lavaSecondsLeft + " seconds.", getName());
    }

    @Override
    public void onDisable() {
        phase = Phase.IDLE;
        if (sneak.getValue()) mc.options.sneakKey.setPressed(false);
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null || castPos == null) return;
        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);

        switch (phase) {
            case PLACE_LAVA -> {
                if (useItem(Items.LAVA_BUCKET)) {
                    phase = Phase.WAIT_LAVA;
                    ticks = 0;
                }
            }
            case WAIT_LAVA -> {
                ticks++;
                if (ticks >= 20) {
                    ticks = 0;
                    lavaSecondsLeft--;
                    if (lavaSecondsLeft <= 0) phase = Phase.PICKUP_LAVA;
                }
            }
            case PICKUP_LAVA -> {
                if (useItem(Items.BUCKET)) { phase = Phase.WAIT_WATER_PLACE; ticks = 0; }
            }
            case WAIT_WATER_PLACE -> {
                ticks++;
                if (ticks >= waterDelayTicks.getValue().intValue()) { phase = Phase.PLACE_WATER; ticks = 0; }
            }
            case PLACE_WATER -> {
                if (useItem(Items.WATER_BUCKET)) { phase = Phase.WAIT_WATER_PICKUP; ticks = 0; }
            }
            case WAIT_WATER_PICKUP -> {
                ticks++;
                if (ticks >= pickupDelayTicks.getValue().intValue()) {
                    if (autoPickupWater.getValue()) { phase = Phase.PICKUP_WATER; }
                    else if (increaseY.getValue()) { phase = Phase.BUILD_UP; }
                    else { finishLayer(); }
                    ticks = 0;
                }
            }
            case PICKUP_WATER -> {
                if (useItem(Items.BUCKET)) {
                    if (increaseY.getValue()) { phase = Phase.BUILD_UP; }
                    else { finishLayer(); }
                    ticks = 0;
                }
            }
            case BUILD_UP -> {
                if (buildStep >= PLUS_SHAPE.length) {
                    buildStep = 0;
                    castPos = castPos.add(0, 1, 0);
                    updateRotations(castPos);
                    mc.player.jump();
                    finishLayer();
                } else {
                    BlockPos target = castPos.add(PLUS_SHAPE[buildStep]);
                    if (placeBlock(target)) buildStep++;
                }
            }
        }
    }

    private boolean useItem(Item item) {
        int slot = InventoryUtils.find(item, 0, 8);
        if (slot == -1) {
            if (item == Items.LAVA_BUCKET) setToggled(false);
            return false;
        }
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
        return true;
    }

    private boolean placeBlock(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return true;
        BlockPos against = null;
        Direction dir = null;
        for (Direction d : Direction.values()) {
            BlockPos adjacent = pos.offset(d);
            if (mc.world.getBlockState(adjacent).isSolid()) {
                against = adjacent; dir = d.getOpposite(); break;
            }
        }
        if (against == null) return false;
        int slot = -1;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) { slot = i; break; }
        }
        if (slot == -1) return true;
        updateRotations(against);
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(against), dir, against, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
        return true;
    }

    private void finishLayer() {
        castPos = castPos.up();
        layer++;
        if (layer > 300) { setToggled(false); return; }
        lavaSecondsLeft = lavaFlowSeconds.getValue().intValue();
        phase = Phase.PLACE_LAVA;
        ticks = 0;
        updateRotations(castPos);
        Sydney.CHAT_MANAGER.tagged("Starting layer " + layer + ". Lava will flow for " + lavaSecondsLeft + " seconds.", getName());
    }

    private void updateRotations(BlockPos target) {
        Vec3d tc = Vec3d.ofCenter(target);
        Vec3d eye = mc.player.getEyePos();
        double dx = tc.x - eye.x, dy = tc.y - eye.y, dz = tc.z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);
        targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(dy, hd));
    }
}
