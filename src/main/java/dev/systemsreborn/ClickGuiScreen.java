package dev.systemsreborn;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    private static final int WIDTH = 230;
    private static final int ROW = 25;
    private static final String[] MODULES = {"Aim Assist", "TriggerBot", "Auto Sprint"};

    public ClickGuiScreen() {
        super(Component.literal("SYSTEMS REBORN"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - 125) / 2;
        g.fill(left, top, left + WIDTH, top + 125, 0xEE10131C);
        g.fill(left, top, left + WIDTH, top + 28, 0xFF252B3B);
        g.drawString(this.font, "SYSTEMS REBORN", left + 12, top + 10, 0xFFFFFFFF);
        for (int i = 0; i < MODULES.length; i++) {
            boolean enabled = enabled(i);
            int y = top + 36 + i * ROW;
            boolean hovered = mouseX >= left + 8 && mouseX <= left + WIDTH - 8
                    && mouseY >= y && mouseY <= y + 21;
            g.fill(left + 8, y, left + WIDTH - 8, y + 21,
                    hovered ? 0xFF343D52 : 0xFF202635);
            g.drawString(this.font, MODULES[i], left + 16, y + 7, 0xFFFFFFFF);
            g.drawString(this.font, enabled ? "ON" : "OFF", left + WIDTH - 42, y + 7,
                    enabled ? 0xFF65E6A0 : 0xFF9AA3B2);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();
            int left = (this.width - WIDTH) / 2;
            int top = (this.height - 125) / 2;
            for (int i = 0; i < MODULES.length; i++) {
                int y = top + 36 + i * ROW;
                if (mouseX >= left + 8 && mouseX <= left + WIDTH - 8
                        && mouseY >= y && mouseY <= y + 21) {
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
            default -> ModuleController.autoSprint;
        };
    }

    private void toggle(int i) {
        switch (i) {
            case 0 -> ModuleController.aimAssist = !ModuleController.aimAssist;
            case 1 -> ModuleController.triggerBot = !ModuleController.triggerBot;
            case 2 -> ModuleController.autoSprint = !ModuleController.autoSprint;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
