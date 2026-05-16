package me.aidan.sydney.gui.impl;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.api.Button;
import me.aidan.sydney.gui.api.Frame;
import me.aidan.sydney.settings.impl.BindSetting;
import me.aidan.sydney.utils.input.KeyboardUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindButton extends Button {
    private final BindSetting setting;
    private boolean listening = false;

    public BindButton(BindSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String bind = listening ? "..." : KeyboardUtils.getKeyName(setting.getValue());
        int bindWidth = Sydney.FONT_MANAGER.getWidth(bind) + 8;
        int maxLabelWidth = getWidth() - bindWidth - 12;
        String label = setting.getTag();
        if (Sydney.FONT_MANAGER.getWidth(label) > maxLabelWidth && maxLabelWidth > 10) {
            while (Sydney.FONT_MANAGER.getWidth(label + "...") > maxLabelWidth && label.length() > 1) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        Sydney.FONT_MANAGER.drawTextWithShadow(context, label, getX() + 6, getY() + 2, Color.WHITE);
        Sydney.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + bind, getX() + getWidth() - 8 - Sydney.FONT_MANAGER.getWidth(bind), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                listening = true;
                playClickSound();
            } else {
                setting.setValue(0);
            }

            if(listening) {
                if (button == 1 || button == 2 || button == 3 || button == 4) {
                    setting.setValue(-button - 1);
                    listening = false;
                }
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                setting.setValue(0);
            } else {
                setting.setValue(keyCode);
            }
            listening = false;
        }
    }
}
