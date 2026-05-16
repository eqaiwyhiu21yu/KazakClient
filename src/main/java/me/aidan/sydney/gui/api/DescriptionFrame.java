package me.aidan.sydney.gui.api;

import lombok.Getter;
import lombok.Setter;
import me.aidan.sydney.Sydney;
import me.aidan.sydney.utils.graphics.Renderer2D;
import me.aidan.sydney.utils.text.FormattingUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;

@Getter @Setter
public class DescriptionFrame {
    private String description = "";
    private int textPadding = 4;

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (description.isEmpty()) return;

        List<String> wrapped = FormattingUtils.wrapText(description, 180);
        int lineH = Sydney.FONT_MANAGER.getHeight();
        int boxW = 180;
        int boxH = wrapped.size() * lineH + textPadding * 2;

        int tooltipX = mouseX + 8;
        int tooltipY = mouseY + 4;

        if (tooltipX + boxW > context.getScaledWindowWidth()) tooltipX = mouseX - boxW - 8;
        if (tooltipY + boxH > context.getScaledWindowHeight()) tooltipY = mouseY - boxH - 4;

        int x = Math.max(2, tooltipX);
        int y = Math.max(2, tooltipY);

        Renderer2D.renderQuad(context.getMatrices(), x, y, x + boxW, y + boxH, new Color(0, 0, 0, 200));
        Renderer2D.renderOutline(context.getMatrices(), x, y, x + boxW, y + boxH, new Color(80, 80, 80, 200));

        for (int i = 0; i < wrapped.size(); i++) {
            Sydney.FONT_MANAGER.drawTextWithShadow(context, wrapped.get(i), x + textPadding, y + textPadding + (lineH * i), Color.WHITE);
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return false;
    }
}
