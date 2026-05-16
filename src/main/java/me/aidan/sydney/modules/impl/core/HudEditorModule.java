package me.aidan.sydney.modules.impl.core;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.RegisterModule;
import org.lwjgl.glfw.GLFW;

@RegisterModule(name = "HudEditor", description = "Allows you to visually customize the position of every HUD element directly on screen.", category = Module.Category.CORE, drawn = false, bind = GLFW.GLFW_KEY_F4)
public class HudEditorModule extends Module {
    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }
        HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
        hud.initializeHudElements();
        hud.editMode = true;
    }

    @Override
    public void onDisable() {
        HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
        hud.editMode = false;
        hud.draggedElement = null;
    }
}
