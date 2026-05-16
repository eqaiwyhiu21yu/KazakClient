package me.aidan.sydney.gui;

import lombok.Getter;
import lombok.Setter;
import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.api.DescriptionFrame;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.modules.impl.core.ClickGuiModule;
import me.aidan.sydney.gui.api.Button;
import me.aidan.sydney.gui.api.Frame;
import me.aidan.sydney.modules.impl.core.ColorModule;
import me.aidan.sydney.utils.color.ColorUtils;
import me.aidan.sydney.utils.graphics.Renderer2D;
import me.aidan.sydney.utils.system.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class ClickGuiScreen extends Screen {
    private final ArrayList<Frame> frames = new ArrayList<>();
    private final ArrayList<Button> buttons = new ArrayList<>();
    private final DescriptionFrame descriptionFrame;

    private final Timer lineTimer = new Timer();
    private boolean showLine = false;
    private Color colorClipboard = null;
    private String searchQuery = "";
    private boolean searching = false;

    public ClickGuiScreen() {
        super(Text.literal(Sydney.MOD_ID + "-click-gui"));

        int x = 6;
        for(Module.Category category : Module.Category.values()) {
            frames.add(new Frame(category, x, 3, 120, 14));
            x += 126;
        }

        this.descriptionFrame = new DescriptionFrame();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (lineTimer.hasTimeElapsed(400L)){
            showLine = !showLine;
            lineTimer.reset();
        }

        descriptionFrame.setDescription("");
        for(Frame frame : frames) frame.render(context, mouseX, mouseY, delta);

        descriptionFrame.render(context, mouseX, mouseY, delta);

        renderSearchBar(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for(Frame frame : frames) frame.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Frame frame : frames) {
            frame.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0) {
            int sbX = width / 2 - 80;
            int sbY = height - 22;
            if (mouseX >= sbX && mouseX <= sbX + 160 && mouseY >= sbY && mouseY <= sbY + 16) {
                searching = true;
                return super.mouseClicked(mouseX, mouseY, button);
            } else {
                searching = false;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Frame frame : frames) {
            frame.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)).isPresent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searching) {
            if (keyCode == 257 || keyCode == 335) {
                searching = false;
                return true;
            }
            if (keyCode == 259 && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            return true;
        }

        for (Frame frame : frames) {
            frame.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searching) {
            if (chr >= 32 && chr <= 126) {
                searchQuery += chr;
            }
            return true;
        }

        for (Frame frame : frames) {
            frame.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if(Sydney.MODULE_MANAGER.getModule(ClickGuiModule.class).blur.getValue()) applyBlur();
        Renderer2D.renderQuad(context.getMatrices(), 0, 0, this.width, this.height, new Color(0, 0, 0, 120));
    }

    @Override
    public void close() {
        super.close();
        Sydney.MODULE_MANAGER.getModule(ClickGuiModule.class).setToggled(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static Color getButtonColor(int index, int alpha) {
        Color color = Sydney.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow() ? ColorUtils.getOffsetRainbow(index*10L) : Sydney.MODULE_MANAGER.getModule(ClickGuiModule.class).color.getColor();
        return ColorUtils.getColor(color, alpha);
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        int sbX = width / 2 - 80;
        int sbY = height - 22;
        int sbW = 160;
        int sbH = 16;

        Color bg = new Color(35, 35, 35, 255);
        Renderer2D.renderQuad(context.getMatrices(), sbX, sbY, sbX + sbW, sbY + sbH, bg);
        Renderer2D.renderOutline(context.getMatrices(), sbX, sbY, sbX + sbW, sbY + sbH, new Color(0, 0, 0, 255));

        boolean hovered = mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= sbY && mouseY <= sbY + sbH;

        String display;
        if (searching) {
            display = searchQuery + (showLine ? "|" : "");
        } else if (!searchQuery.isEmpty()) {
            display = searchQuery;
        } else {
            display = Formatting.DARK_GRAY + "Search modules...";
        }
        Sydney.FONT_MANAGER.drawTextWithShadow(context, display, sbX + 4, sbY + 3, hovered || searching ? Color.WHITE : new Color(180, 180, 180));
    }

    public static String getSearchQuery() {
        ClickGuiScreen gui = Sydney.CLICK_GUI;
        return gui != null ? gui.searchQuery : "";
    }
}
