package me.aidan.sydney.gui.impl;

import lombok.Getter;
import lombok.Setter;
import me.aidan.sydney.Sydney;
import me.aidan.sydney.gui.ClickGuiScreen;
import me.aidan.sydney.modules.Module;
import me.aidan.sydney.gui.api.Button;
import me.aidan.sydney.gui.api.Frame;
import me.aidan.sydney.settings.Setting;
import me.aidan.sydney.settings.impl.*;
import me.aidan.sydney.utils.graphics.Renderer2D;
import me.aidan.sydney.utils.input.KeyboardUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class ModuleButton extends Button {
    private final Module module;
    private boolean open = false;
    private final ArrayList<Button> buttons = new ArrayList<>();

    public ModuleButton(Module module, Frame parent, int height) {
        super(parent, height, module.getDescription());
        this.module = module;

        for(Setting setting : module.getSettings()) {
            if(setting instanceof BooleanSetting s) {
                buttons.add(new BooleanButton(s, parent, height));
            } else if(setting instanceof NumberSetting s) {
                buttons.add(new NumberButton(s, parent, height));
            } else if(setting instanceof CategorySetting s) {
                buttons.add(new CategoryButton(s, parent, height));
            } else if(setting instanceof BindSetting s) {
                buttons.add(new BindButton(s, parent, height));
            } else if(setting instanceof ModeSetting s) {
                buttons.add(new ModeButton(s, parent, height));
            } else if(setting instanceof WhitelistSetting s) {
                buttons.add(new WhitelistButton(s, parent, height));
            } else if(setting instanceof StringSetting s) {
                buttons.add(new StringButton(s, parent, height));
            } else if(setting instanceof ColorSetting s) {
                buttons.add(new ColorButton(s, parent, height));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isFiltered()) return;
        if(this.isHovering(mouseX, mouseY) && Sydney.CLICK_GUI.getDescriptionFrame().getDescription().isEmpty()) Sydney.CLICK_GUI.getDescriptionFrame().setDescription(this.getDescription());

        int bx = getX();
        int by = getY();
        int bw = getWidth() - getPadding() * 2;
        int bh = getHeight() - 1;

        boolean hovered = isHovering(mouseX, mouseY);

        if(module.isToggled()) {
            Renderer2D.renderQuad(context.getMatrices(), bx, by, bx + bw, by + bh, ClickGuiScreen.getButtonColor(getY(), 180));
        } else if(hovered) {
            Renderer2D.renderQuad(context.getMatrices(), bx, by, bx + bw, by + bh, new Color(255, 255, 255, 15));
        }

        Renderer2D.renderOutline(context.getMatrices(), bx, by, bx + bw, by + bh, new Color(ClickGuiScreen.getButtonColor(getY(), 50).getRGB(), true));

        int textColor = module.isToggled() ? 0xFFFFFFFF : 0xFFAAAAAA;
        Sydney.FONT_MANAGER.drawTextWithShadow(context, module.getName(), bx + 4, by + 2, new Color(textColor, true));

        String bindText = module.getBind() == 0 ? "" : KeyboardUtils.getKeyName(module.getBind());
        if(!bindText.isEmpty()) {
            Sydney.FONT_MANAGER.drawTextWithShadow(context, Formatting.DARK_GRAY + bindText, bx + bw - Sydney.FONT_MANAGER.getWidth(bindText) - 6, by + 2, Color.WHITE);
        }

        if(!buttons.isEmpty()) {
            String arrow = open ? "-" : "+";
            int arrowX = bx + bw - 7;
            if(!bindText.isEmpty()) arrowX = bx + bw - Sydney.FONT_MANAGER.getWidth(arrow) - 3;
            Sydney.FONT_MANAGER.drawTextWithShadow(context, arrow, arrowX, by + 2, open ? new Color(200, 200, 200) : new Color(100, 100, 100));
        }

        if(open) {
            for(Button button : buttons) {
                if(!button.isVisible()) continue;
                button.render(context, mouseX, mouseY, delta);
                if(button.isHovering(mouseX, mouseY) && Sydney.CLICK_GUI.getDescriptionFrame().getDescription().isEmpty()) Sydney.CLICK_GUI.getDescriptionFrame().setDescription(button.getDescription());
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                module.setToggled(!module.isToggled());
                playClickSound();
            } else if(button == 1) {
                open = !open;
                playClickSound();
            }
        }

        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for(Button b : buttons) {
            b.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Button b : buttons) {
            b.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if(open) {
            for(Button b : buttons) {
                if(!b.isVisible()) continue;
                b.charTyped(chr, modifiers);
            }
        }
    }

    private boolean isFiltered() {
        String query = ClickGuiScreen.getSearchQuery();
        return !query.isEmpty() && !module.getName().toLowerCase().contains(query.toLowerCase());
    }

    @Override
    public int getHeight() {
        return isFiltered() ? 0 : super.getHeight();
    }

    public boolean isOpen() {
        return !isFiltered() && open;
    }
}
