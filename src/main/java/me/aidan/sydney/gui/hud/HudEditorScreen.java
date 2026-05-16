package me.aidan.sydney.gui.hud;

import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.ClickGuiScreen;
import me.aidan.sydney.modules.impl.core.HUDModule;
import me.aidan.sydney.modules.impl.core.HudEditorModule;
import me.aidan.sydney.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class HudEditorScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int ITEM_HEIGHT = 12;

    private HudElement draggedElement;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartOffsetX, dragStartOffsetY;
    private boolean dragging;

    private int gridSize = 10;
    private boolean showGrid = true;
    private HudElement selectedElement;
    private double sidebarScroll;
    private boolean scrollBarHovered;

    public HudEditorScreen() {
        super(Text.literal(Sydney.MOD_ID + "-hud-editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int editWidth = width - SIDEBAR_WIDTH;

        if (showGrid && gridSize > 1) {
            Color gridColor = new Color(255, 255, 255, 15);
            for (int x = 0; x <= editWidth; x += gridSize) {
                Renderer2D.renderQuad(context.getMatrices(), x, 0, x + 0.5f, height, gridColor);
            }
            for (int y = 0; y <= height; y += gridSize) {
                Renderer2D.renderQuad(context.getMatrices(), 0, y, editWidth, y + 0.5f, gridColor);
            }
        }

        HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
        if (hud == null || hud.getHudElements() == null) return;

        for (HudElement element : hud.getHudElements().values()) {
            if (element.getWidth() <= 0 || element.getHeight() <= 0) continue;
            if (element.getScreenX() > editWidth) {
                element.setScreenX(editWidth - 4);
                renderInlineOverflow(context, mouseX, element, Color.ORANGE);
                continue;
            }

            renderElementBox(context, mouseX, mouseY, element);
        }

        renderSidebar(context, mouseX, mouseY, hud);

        String hint = "\u00a77Drag to reposition | Right-click: toggle | G: grid | +/-: size | ESC: save";
        Sydney.FONT_MANAGER.drawTextWithShadow(context, hint, width / 2 - Sydney.FONT_MANAGER.getWidth(hint) / 2, height - 14, Color.GRAY);
    }

    private void renderElementBox(DrawContext context, int mouseX, int mouseY, HudElement element) {
        boolean hovered = element.isHovering(mouseX, mouseY);
        boolean isDragged = draggedElement == element;
        boolean isSelected = selectedElement == element;

        Color bodyColor = new Color(0, 0, 0, isSelected ? 90 : isDragged ? 80 : 60);

        Color borderColor;
        if (isDragged || isSelected) borderColor = Color.WHITE;
        else if (hovered) borderColor = new Color(200, 200, 200, 200);
        else borderColor = new Color(150, 150, 150, 120);

        int sx = element.getScreenX();
        int sy = element.getScreenY();
        int w = element.getWidth();
        int h = element.getHeight();

        Renderer2D.renderQuad(context.getMatrices(), sx - 2, sy - 2, sx + w + 2, sy + h + 2, bodyColor);

        if (!element.isVisible()) {
            Renderer2D.renderOutline(context.getMatrices(), sx - 2, sy - 2, sx + w + 2, sy + h + 2, new Color(255, 50, 50, 120));
            int cx = sx + w / 2 - 3;
            int cy = sy + h / 2 - 3;
            Renderer2D.renderQuad(context.getMatrices(), cx, cy, cx + 6, cy + 1, new Color(255, 50, 50, 150));
        } else {
            Renderer2D.renderOutline(context.getMatrices(), sx - 2, sy - 2, sx + w + 2, sy + h + 2, borderColor);
        }

        String label = element.getName() + " [" + element.getOffsetX() + ", " + element.getOffsetY() + "]";
        Color labelColor = hovered || isDragged || isSelected ? Color.WHITE : new Color(180, 180, 180);
        int labelY = sy - Sydney.FONT_MANAGER.getHeight() - 2;
        if (labelY < 2) labelY = sy + h + 2;
        Sydney.FONT_MANAGER.drawTextWithShadow(context, label, sx, labelY, labelColor);
    }

    private void renderInlineOverflow(DrawContext context, int mouseX, HudElement element, Color accent) {
        int sx = element.getScreenX();
        int sy = element.getScreenY();
        Renderer2D.renderQuad(context.getMatrices(), sx, sy, sx + 4, sy + element.getHeight(), accent);
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, HUDModule hud) {
        int sx = width - SIDEBAR_WIDTH;
        int headerHeight = 14;
        int bottomHeight = 30;

        Renderer2D.renderQuad(context.getMatrices(), sx, 0, width, height, new Color(0, 0, 0, 160));
        Renderer2D.renderOutline(context.getMatrices(), sx, 0, width, height, new Color(0, 0, 0, 255));

        Renderer2D.renderQuad(context.getMatrices(), sx, 0, width, headerHeight, ClickGuiScreen.getButtonColor(0, 150));
        Renderer2D.renderQuad(context.getMatrices(), sx, 0, width, 1, ClickGuiScreen.getButtonColor(0, 255));
        Sydney.FONT_MANAGER.drawTextWithShadow(context, "HUD Editor", sx + 3, 2, Color.WHITE);

        int listY = headerHeight + 2;
        int listHeight = height - headerHeight - bottomHeight - 4;
        int totalContent = hud.getHudElements().size() * ITEM_HEIGHT;
        boolean needsScroll = totalContent > listHeight;

        context.enableScissor(sx, listY, SIDEBAR_WIDTH, listHeight);
        try {
            int yOff = listY - (int) sidebarScroll;
            for (HudElement element : hud.getHudElements().values()) {
                if (yOff + ITEM_HEIGHT > listY && yOff < listY + listHeight) {
                    boolean hovered = mouseX >= sx && mouseX <= width && mouseY >= yOff && mouseY <= yOff + ITEM_HEIGHT;
                    boolean isSelected = selectedElement == element;
                    boolean isDragged = draggedElement == element;

                    if (isDragged) {
                        Renderer2D.renderQuad(context.getMatrices(), sx, yOff, width, yOff + ITEM_HEIGHT, new Color(255, 255, 255, 30));
                    } else if (isSelected) {
                        Renderer2D.renderQuad(context.getMatrices(), sx, yOff, width, yOff + ITEM_HEIGHT, ClickGuiScreen.getButtonColor(0, 100));
                    } else if (hovered) {
                        Renderer2D.renderQuad(context.getMatrices(), sx, yOff, width, yOff + ITEM_HEIGHT, new Color(255, 255, 255, 15));
                    }

                    Color dotColor = element.isVisible() ? new Color(100, 255, 100, 200) : new Color(255, 100, 100, 200);
                    Renderer2D.renderQuad(context.getMatrices(), sx + 4, yOff + 3, sx + 8, yOff + 9, dotColor);

                    String display = element.getName();
                    if (display.length() > 16) display = display.substring(0, 14) + "..";
                    Sydney.FONT_MANAGER.drawTextWithShadow(context, display, sx + 12, yOff + 2, new Color(220, 220, 220));
                }
                yOff += ITEM_HEIGHT;
            }
        } finally {
            context.disableScissor();
        }

        if (needsScroll) {
            int scrollTrackX = sx + SIDEBAR_WIDTH - 4;
            int scrollTrackH = listHeight;
            float visibleRatio = (float) listHeight / totalContent;
            float scrollProgress = (float) sidebarScroll / (totalContent - listHeight);
            int scrollBarH = Math.max(12, (int) (scrollTrackH * visibleRatio));
            int scrollBarY = listY + (int) (scrollProgress * (scrollTrackH - scrollBarH));

            scrollBarHovered = mouseX >= scrollTrackX && mouseX <= sx + SIDEBAR_WIDTH && mouseY >= scrollBarY && mouseY <= scrollBarY + scrollBarH;

            Color scrollColor = scrollBarHovered ? new Color(180, 180, 180, 100) : new Color(120, 120, 120, 80);
            Renderer2D.renderQuad(context.getMatrices(), scrollTrackX, scrollBarY, sx + SIDEBAR_WIDTH - 1, scrollBarY + scrollBarH, scrollColor);
        }

        int gridY = height - bottomHeight;
        Renderer2D.renderQuad(context.getMatrices(), sx, gridY, width, height, new Color(20, 20, 20, 200));
        Renderer2D.renderQuad(context.getMatrices(), sx, gridY, width, gridY + 1, new Color(0, 0, 0, 255));

        String gridLabel = "\u00a77Grid: \u00a7f" + gridSize + "px " + (showGrid ? "\u00a7a[ON]" : "\u00a78[OFF]");
        Sydney.FONT_MANAGER.drawTextWithShadow(context, gridLabel, sx + 4, gridY + 3, Color.WHITE);

        String hintText = "\u00a77< > adjust  G toggle";
        Sydney.FONT_MANAGER.drawTextWithShadow(context, hintText, sx + 4, gridY + 14, Color.GRAY);

        if (selectedElement != null) {
            String pos = "\u00a77Pos: \u00a7f" + selectedElement.getOffsetX() + ", " + selectedElement.getOffsetY();
            Sydney.FONT_MANAGER.drawTextWithShadow(context, pos, sx + 4, gridY - 10 - Sydney.FONT_MANAGER.getHeight(), Color.WHITE);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int editWidth = width - SIDEBAR_WIDTH;
        boolean inSidebar = mouseX >= editWidth;

        if (inSidebar) {
            selectedElement = null;
            int sx = width - SIDEBAR_WIDTH;
            int headerHeight = 14;
            int bottomHeight = 30;
            int listStartY = headerHeight + 2;

            HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
            if (hud != null && hud.getHudElements() != null) {
                double yOff = listStartY - sidebarScroll;
                for (HudElement element : hud.getHudElements().values()) {
                    if (mouseY >= yOff && mouseY <= yOff + ITEM_HEIGHT) {
                        if (button == 0) {
                            selectedElement = element;
                        } else if (button == 1) {
                            element.setVisible(!element.isVisible());
                        }
                        return super.mouseClicked(mouseX, mouseY, button);
                    }
                    yOff += ITEM_HEIGHT;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
        if (hud == null || hud.getHudElements() == null) return super.mouseClicked(mouseX, mouseY, button);

        selectedElement = null;

        for (HudElement element : hud.getHudElements().values()) {
            if (element.getWidth() <= 0 || element.getHeight() <= 0) continue;
            if (element.getScreenX() > editWidth) continue;

            if (element.isHovering(mouseX, mouseY)) {
                selectedElement = element;
                if (button == 1) {
                    element.setVisible(!element.isVisible());
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                if (button == 0) {
                    draggedElement = element;
                    dragStartMouseX = (int) mouseX;
                    dragStartMouseY = (int) mouseY;
                    dragStartOffsetX = element.getOffsetX();
                    dragStartOffsetY = element.getOffsetY();
                    dragging = true;
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && draggedElement != null && button == 0) {
            int dx = (int) mouseX - dragStartMouseX;
            int dy = (int) mouseY - dragStartMouseY;
            int rawX = dragStartOffsetX + dx;
            int rawY = dragStartOffsetY + dy;
            if (gridSize > 1) {
                rawX = Math.round((float) rawX / gridSize) * gridSize;
                rawY = Math.round((float) rawY / gridSize) * gridSize;
            }
            draggedElement.setOffsetX(rawX);
            draggedElement.setOffsetY(rawY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= width - SIDEBAR_WIDTH) {
            HUDModule hud = Sydney.MODULE_MANAGER.getModule(HUDModule.class);
            if (hud == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            int totalContent = hud.getHudElements().size() * ITEM_HEIGHT;
            int listHeight = height - 44 - 4;
            int maxScroll = Math.max(0, totalContent - listHeight);
            sidebarScroll = Math.max(0, Math.min(maxScroll, sidebarScroll - verticalAmount * 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggedElement = null;
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 71) {
            showGrid = !showGrid;
            return true;
        }
        if (keyCode == 343 || keyCode == 69) {
            gridSize = Math.min(50, gridSize + 5);
            return true;
        }
        if (keyCode == 45 || keyCode == 333) {
            gridSize = Math.max(2, gridSize - 5);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        super.close();
        Sydney.MODULE_MANAGER.getModule(HudEditorModule.class).setToggled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
