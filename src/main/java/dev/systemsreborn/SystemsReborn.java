package dev.systemsreborn;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class SystemsReborn implements ClientModInitializer {
    public static final String MOD_ID = "systemsreborn";
    private static KeyMapping clickGuiKey;
    private static KeyMapping hudToggleKey;\n    private static KeyMapping hudEditorKey;

    @Override
    public void onInitializeClient() {
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.systemsreborn.clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.systemsreborn.hud", GLFW.GLFW_KEY_F8,
                KeyMapping.Category.MISC));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.systemsreborn.hud_editor", GLFW.GLFW_KEY_F7,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DesktopHudOverlay.close());
        DesktopHudOverlay.start();
    }

    private void onClientTick(Minecraft client) {
        while (clickGuiKey.consumeClick()) {
            if (client.screen == null) client.setScreen(new ClickGuiScreen());
            else if (client.screen instanceof ClickGuiScreen) client.setScreen(null);
        }
        while (hudToggleKey.consumeClick()) {
            ModuleController.desktopHud = !ModuleController.desktopHud;
        }
        while (hudEditorKey.consumeClick()) {
            DesktopHudOverlay.openEditor();
        }
        ModuleController.tick(client);
        if (ModuleController.desktopHud) DesktopHudOverlay.update(client);
        else DesktopHudOverlay.hide();
    }
}
