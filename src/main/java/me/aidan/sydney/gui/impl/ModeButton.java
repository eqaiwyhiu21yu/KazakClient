package me.aidan.sydney.gui.impl;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.ClickGuiScreen;
import me.aidan.sydney.gui.api.Button;
import me.aidan.sydney.gui.api.Frame;
import me.aidan.sydney.settings.impl.ModeSetting;
import me.aidan.sydney.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ModeButton extends Button {
    private final ModeSetting setting;
    private boolean open = false;

    public ModeButton(ModeSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String value = setting.getValue();
        int valueWidth = Sydney.FONT_MANAGER.getWidth(value) + 8;
        int maxLabelWidth = getWidth() - valueWidth - 12;
        String label = setting.getTag();
        if (Sydney.FONT_MANAGER.getWidth(label) > maxLabelWidth && maxLabelWidth > 10) {
            while (Sydney.FONT_MANAGER.getWidth(label + "...") > maxLabelWidth && label.length() > 1) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        Sydney.FONT_MANAGER.drawTextWithShadow(context, label, getX() + 6, getY() + 2, Color.WHITE);
        Sydney.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + value, getX() + getWidth() - 8 - Sydney.FONT_MANAGER.getWidth(value), getY() + 2, Color.WHITE);

        if(open) {
            int i = 0;
            for(String s : setting.getModes()) {
                int itemY = getY() + getParent().getHeight() + i;
                boolean hovered = mouseX >= getX() + 4 && mouseX <= getX() + getWidth() - 4 && mouseY >= itemY && mouseY <= itemY + getParent().getHeight();
                boolean selected = setting.getValue().equals(s);

                if(hovered || selected) {
                    Renderer2D.renderQuad(context.getMatrices(), getX() + 4, itemY, getX() + getWidth() - 4, itemY + getParent().getHeight(),
                            selected ? ClickGuiScreen.getButtonColor(getY(), 60) : new Color(255, 255, 255, 10));
                }

                Sydney.FONT_MANAGER.drawTextWithShadow(context, (selected ? "" : Formatting.GRAY) + s, getX() + 8, itemY + 2,
                        selected ? Color.WHITE : (hovered ? Color.WHITE : new Color(140, 140, 140)));
                i += getParent().getHeight();
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                int choice = setting.getModes().indexOf(setting.getValue());
                choice ++;
                if(choice > setting.getModes().size() - 1) choice = 0;
                setting.setValue(setting.getModes().get(choice));
                playClickSound();
            } else if(button == 1) {
                open = !open;
            }
        }

        if(open && isHoveringModes(mouseX, mouseY)) {
            int choice = MathHelper.clamp((int)(mouseY - getY() - getParent().getHeight())/getParent().getHeight(), 0, setting.getModes().size() - 1);
            setting.setValue(setting.getModes().get(choice));
        }
    }

    @Override
    public int getHeight() {
        return getParent().getHeight() + (open ? getParent().getHeight() * setting.getModes().size() : 0);
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() + getPadding() <= mouseX && getY() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() > mouseY;
    }

    public boolean isHoveringModes(double mouseX, double mouseY) {
        int modesHeight = getParent().getHeight() * setting.getModes().size();
        return getX() + getPadding() <= mouseX && getY() + getParent().getHeight() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() + modesHeight > mouseY;
    }
}
