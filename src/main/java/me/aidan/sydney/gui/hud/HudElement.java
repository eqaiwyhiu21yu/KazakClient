package me.aidan.sydney.gui.hud;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class HudElement {
    private final String key;
    private final String name;
    private int offsetX, offsetY;
    private boolean visible = true;
    private int screenX, screenY, width, height;

    public HudElement(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return mouseX >= screenX && mouseX <= screenX + width && mouseY >= screenY && mouseY <= screenY + height;
    }
}
