package dev.systemsreborn;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    private static final int WIDTH = 270;
    private static final int ROW = 29;
    private static final String[] MODULES = {
            "Aim Assist", "TriggerBot", "Auto Sprint", "Desktop HUD"
    };

    public ClickGuiScreen() {
        super(Component.literal("SYSTEMS REBORN"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        int height = 82 + MODULES.length * ROW;
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - height) / 2;
        g.fill(left, top, left + WIDTH, top + height, 0xF010131C);
        g.fill(left, top, left + WIDTH, top + 34, 0xFF252B3B);
        g.drawString(this.font, "SYSTEMS REBORN", left + 14, top + 12, 0xFFFFFFFF);
        g.drawString(this.font, "LEGIT CLIENT  /  1.21.11", left + 14, top + 41, 0xFF9AA8C0);
        g.drawString(this.font, "F7  HUD EDITOR", left + 14, top + 56, 0xFF74B2FF);

        for (int i = 0; i < MODULES.length; i++) {
            boolean enabled = enabled(i);
            int y = top + 74 + i * ROW;
            boolean hovered = mouseX >= left + 9 && mouseX <= left + WIDTH - 9
                    && mouseY >= y && mouseY <= y + 24;
            g.fill(left + 9, y, left + WIDTH - 9, y + 24,
                    hovered ? 0xFF343D52 : 0xFF202635);
            g.drawString(this.font, MODULES[i], left + 17, y + 8, 0xFFFFFFFF);
            g.drawString(this.font, enabled ? "ON" : "OFF", left + WIDTH - 43, y + 8,
                    enabled ? 0xFF65E6A0 : 0xFF9AA3B2);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            int height = 82 + MODULES.length * ROW;
            int left = (this.width - WIDTH) / 2;
            int top = (this.height - height) / 2;
            for (int i = 0; i < MODULES.length; i++) {
                int y = top + 74 + i * ROW;
                if (click.x() >= left + 9 && click.x() <= left + WIDTH - 9
                        && click.y() >= y && click.y() <= y + 24) {
                    toggle(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean enabled(int i) {
        return switch (i) {
            case 0 -> ModuleController.aimAssist;
            case 1 -> ModuleController.triggerBot;
            case 2 -> ModuleController.autoSprint;
            default -> ModuleController.desktopHud;
        };
    }

    private void toggle(int i) {
        switch (i) {
            case 0 -> ModuleController.aimAssist = !ModuleController.aimAssist;
            case 1 -> ModuleController.triggerBot = !ModuleController.triggerBot;
            case 2 -> ModuleController.autoSprint = !ModuleController.autoSprint;
            case 3 -> ModuleController.desktopHud = !ModuleController.desktopHud;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
