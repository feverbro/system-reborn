package dev.systemsreborn;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    private static final int WIDTH = 350;
    private static final int ROW = 25;
    private static final String[] TABS = {"COMBAT", "HUD", "INTERFACE"};
    private static final String[][] MODULES = {
            {"Aim Assist", "Aim Bot", "Legit Aura", "TriggerBot",
                    "Click Assist", "W-Tap", "S-Tap", "Auto Sprint"},
            {"External HUD", "Module List", "Keystrokes", "CPS", "FPS", "Ping",
                    "Coordinates", "Speed", "Target Info", "Armor", "Effects", "HUD Editor"},
            {"HUD Scale", "Accent Color"}
    };
    private static final String[] ACCENT_NAMES = {"Blue", "Purple", "Mint", "Amber"};
    private int activeTab;

    public ClickGuiScreen() {
        super(Component.literal("SYSTEMS REBORN"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        int rows = MODULES[activeTab].length;
        int height = 103 + rows * ROW;
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - height) / 2;

        g.fill(left, top, left + WIDTH, top + height, 0xF010131C);
        g.fill(left, top, left + WIDTH, top + 35, 0xFF252B3B);
        g.drawString(this.font, "SYSTEMS REBORN", left + 14, top + 12, 0xFFFFFFFF);
        g.drawString(this.font, "FABRIC 1.21.11  /  " + TABS[activeTab],
                left + 14, top + 43, 0xFF9AA8C0);

        int tabWidth = (WIDTH - 24) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int x = left + 8 + i * tabWidth;
            int y = top + 57;
            boolean selected = i == activeTab;
            boolean hovered = mouseX >= x && mouseX <= x + tabWidth - 4
                    && mouseY >= y && mouseY <= y + 21;
            g.fill(x, y, x + tabWidth - 4, y + 21,
                    selected ? 0xFF405778 : hovered ? 0xFF343D52 : 0xFF202635);
            g.drawCenteredString(this.font, TABS[i], x + (tabWidth - 4) / 2,
                    y + 7, 0xFFFFFFFF);
        }

        for (int i = 0; i < rows; i++) {
            int y = top + 84 + i * ROW;
            boolean hovered = mouseX >= left + 9 && mouseX <= left + WIDTH - 9
                    && mouseY >= y && mouseY <= y + 22;
            g.fill(left + 9, y, left + WIDTH - 9, y + 22,
                    hovered ? 0xFF343D52 : 0xFF202635);
            g.drawString(this.font, MODULES[activeTab][i], left + 17, y + 7, 0xFFFFFFFF);
            String state = state(i);
            int color = state.equals("ON") ? 0xFF65E6A0 : 0xFF9AA3B2;
            if (activeTab == 2) color = 0xFF74B2FF;
            g.drawString(this.font, state, left + WIDTH - 17 - this.font.width(state),
                    y + 7, color);
        }
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
            int rows = MODULES[activeTab].length;
            int height = 103 + rows * ROW;
            int left = (this.width - WIDTH) / 2;
            int top = (this.height - height) / 2;

            int tabWidth = (WIDTH - 24) / TABS.length;
            if (click.y() >= top + 57 && click.y() <= top + 78) {
                for (int i = 0; i < TABS.length; i++) {
                    int x = left + 8 + i * tabWidth;
                    if (click.x() >= x && click.x() <= x + tabWidth - 4) {
                        activeTab = i;
                        return true;
                    }
                }
            }

            for (int i = 0; i < rows; i++) {
                int y = top + 84 + i * ROW;
                if (click.x() >= left + 9 && click.x() <= left + WIDTH - 9
                        && click.y() >= y && click.y() <= y + 22) {
                    toggle(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private String state(int i) {
        if (activeTab == 2) {
            return i == 0 ? Math.round(ModuleController.hudScale() * 100) + "%"
                    : ACCENT_NAMES[accentIndex()];
        }
        boolean enabled = activeTab == 0 ? combatEnabled(i) : hudEnabled(i);
        return enabled ? "ON" : "OFF";
    }

    private int accentIndex() {
        int color = ModuleController.accentColor();
        if (color == 0xFF9B7BFF) return 1;
        if (color == 0xFF55D6A2) return 2;
        if (color == 0xFFFFB45E) return 3;
        return 0;
    }

    private boolean combatEnabled(int i) {
        return switch (i) {
            case 0 -> ModuleController.aimAssist;
            case 1 -> ModuleController.aimBot;
            case 2 -> ModuleController.legitAura;
            case 3 -> ModuleController.triggerBot;
            case 4 -> ModuleController.clickAssist;
            case 5 -> ModuleController.wTap;
            case 6 -> ModuleController.sTap;
            default -> ModuleController.autoSprint;
        };
    }

    private boolean hudEnabled(int i) {
        return switch (i) {
            case 0 -> ModuleController.desktopHud;
            case 1 -> ModuleController.hudModuleList;
            case 2 -> ModuleController.hudKeystrokes;
            case 3 -> ModuleController.hudCps;
            case 4 -> ModuleController.hudFps;
            case 5 -> ModuleController.hudPing;
            case 6 -> ModuleController.hudCoordinates;
            case 7 -> ModuleController.hudSpeed;
            case 8 -> ModuleController.hudTargetInfo;
            case 9 -> ModuleController.hudArmor;
            case 10 -> ModuleController.hudEffects;
            default -> ModuleController.hudEditor;
        };
    }

    private void toggle(int i) {
        if (activeTab == 2) {
            if (i == 0) ModuleController.cycleHudScale();
            else ModuleController.cycleAccentColor();
            return;
        }
        if (activeTab == 0) {
            switch (i) {
                case 0 -> ModuleController.aimAssist = !ModuleController.aimAssist;
                case 1 -> ModuleController.aimBot = !ModuleController.aimBot;
                case 2 -> ModuleController.legitAura = !ModuleController.legitAura;
                case 3 -> ModuleController.triggerBot = !ModuleController.triggerBot;
                case 4 -> ModuleController.clickAssist = !ModuleController.clickAssist;
                case 5 -> ModuleController.wTap = !ModuleController.wTap;
                case 6 -> ModuleController.sTap = !ModuleController.sTap;
                case 7 -> ModuleController.autoSprint = !ModuleController.autoSprint;
            }
            return;
        }
        switch (i) {
            case 0 -> ModuleController.desktopHud = !ModuleController.desktopHud;
            case 1 -> ModuleController.hudModuleList = !ModuleController.hudModuleList;
            case 2 -> ModuleController.hudKeystrokes = !ModuleController.hudKeystrokes;
            case 3 -> ModuleController.hudCps = !ModuleController.hudCps;
            case 4 -> ModuleController.hudFps = !ModuleController.hudFps;
            case 5 -> ModuleController.hudPing = !ModuleController.hudPing;
            case 6 -> ModuleController.hudCoordinates = !ModuleController.hudCoordinates;
            case 7 -> ModuleController.hudSpeed = !ModuleController.hudSpeed;
            case 8 -> ModuleController.hudTargetInfo = !ModuleController.hudTargetInfo;
            case 9 -> ModuleController.hudArmor = !ModuleController.hudArmor;
            case 10 -> ModuleController.hudEffects = !ModuleController.hudEffects;
            case 11 -> ModuleController.hudEditor = !ModuleController.hudEditor;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
