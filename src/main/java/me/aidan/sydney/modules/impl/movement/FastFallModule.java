package me.aidan.sydney.modules.impl.movement;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.events.SubscribeEvent;
import me.aidan.sydney.events.impl.PlayerUpdateEvent;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import me.aidan.sydney.settings.impl.NumberSetting;

@RegisterModule(name = "FastFall", description = "Pulls you down towards the ground faster.", category = Module.Category.MOVEMENT)
public class FastFallModule extends Module {
    public NumberSetting speed = new NumberSetting("Speed", "The speed at which you will be pulled down.", 3.0, 0.1, 10.0);
    public NumberSetting height = new NumberSetting("Height", "The maximum height at which you will be pulled down.", 3.0, 0.0, 12.0);

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!Sydney.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(300L)) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isRiding() || mc.player.isGliding() || mc.player.isHoldingOntoLadder() || mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.input.playerInput.jump() || mc.player.input.playerInput.sneak()) return;

        if (mc.player.isOnGround() && nearBlock(height.getValue().doubleValue())) {
            mc.player.setVelocity(mc.player.getVelocity().getX(), -speed.getValue().doubleValue(), mc.player.getVelocity().getZ());
        }
    }

    private boolean nearBlock(double height) {
        for (double i = 0; i < height + 0.5; i += 0.01) {
            if (!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0, -i, 0))) return true;
        }
        return false;
    }
}
