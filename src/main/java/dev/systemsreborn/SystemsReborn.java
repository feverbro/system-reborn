package dev.systemsreborn;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class SystemsReborn implements ClientModInitializer {
    public static final String MOD_ID = "systemsreborn";
    private static KeyMapping clickGuiKey;
    private static KeyMapping hudToggleKey;

    @Override
    public void onInitializeClient() {
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.systemsreborn.clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.systemsreborn.hud", GLFW.GLFW_KEY_F8,
                KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
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
        ModuleController.tick(client);
        if (ModuleController.desktopHud) DesktopHudOverlay.update(client);
        else DesktopHudOverlay.hide();
    }
}
