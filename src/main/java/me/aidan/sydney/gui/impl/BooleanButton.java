package me.aidan.sydney.gui.impl;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.ClickGuiScreen;
import me.aidan.sydney.gui.api.Button;
import me.aidan.sydney.gui.api.Frame;
import me.aidan.sydney.settings.impl.BooleanSetting;
import me.aidan.sydney.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;

public class BooleanButton extends Button {
    private final BooleanSetting setting;

    public BooleanButton(BooleanSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int bx = getX();
        int by = getY();
        int bw = getWidth() - getPadding() * 2;

        String label = setting.getTag();
        int boxX = bx + bw - 17;
        int boxSize = 9;
        if (boxX - bx - 10 > 0) {
            int labelColor = setting.getValue() ? 0xFFFFFFFF : 0xFF999999;
            Sydney.FONT_MANAGER.drawTextWithShadow(context, label, bx + 4, by + 2, new Color(labelColor, true));
        }

        int boxY = by + 2;
        boolean hovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= boxY && mouseY <= boxY + boxSize;

        if (setting.getValue()) {
            Renderer2D.renderBorderedRect(context.getMatrices(), boxX, boxY, boxX + boxSize, boxY + boxSize, ClickGuiScreen.getButtonColor(getY(), 200), new Color(0, 0, 0, 100));
        } else if (hovered) {
            Renderer2D.renderBorderedRect(context.getMatrices(), boxX, boxY, boxX + boxSize, boxY + boxSize, new Color(60, 60, 60, 100), new Color(80, 80, 80, 150));
        } else {
            Renderer2D.renderBorderedRect(context.getMatrices(), boxX, boxY, boxX + boxSize, boxY + boxSize, new Color(30, 30, 30, 150), new Color(80, 80, 80, 120));
        }

        if (setting.getValue()) {
            Renderer2D.renderLine(context.getMatrices(), boxX + 2, boxY + boxSize / 2, boxX + 4, boxY + boxSize - 2, Color.WHITE);
            Renderer2D.renderLine(context.getMatrices(), boxX + 4, boxY + boxSize - 2, boxX + boxSize - 2, boxY + 2, Color.WHITE);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
            playClickSound();
        }
    }
}
