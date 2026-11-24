package dev.systemsreborn;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class DesktopHudOverlay {
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final AtomicReference<Snapshot> STATE = new AtomicReference<>(Snapshot.EMPTY);
    private static JFrame frame;
    private static HudPanel panel;

    private DesktopHudOverlay() {}

    public static void start() {
        if (!WINDOWS) return;
        SwingUtilities.invokeLater(() -> {
            if (frame != null) return;
            frame = new JFrame("SYSTEMS REBORN HUD");
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            frame.setAlwaysOnTop(true);
            frame.setFocusableWindowState(false);
            frame.setType(Window.Type.UTILITY);
            panel = new HudPanel();
            panel.setOpaque(false);
            frame.setContentPane(panel);
            frame.setSize(640, 360);
            frame.setLocation(0, 0);
            frame.setVisible(true);
            enableClickThrough(frame);
        });
    }

    public static void update(Minecraft mc) {
        if (!WINDOWS || mc == null || mc.getWindow() == null) return;
        long handle = mc.getWindow().handle();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) {
            hide();
            return;
        }

        int[] x = new int[1], y = new int[1], width = new int[1], height = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetFramebufferSize(handle, width, height);

        String coords = mc.player == null ? "XYZ  --" : String.format(Locale.ROOT,
                "XYZ  %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        String fps = "FPS  " + Minecraft.getInstance().getFps();
        String target = mc.hitResult instanceof EntityHitResult hit
                ? "TARGET  " + hit.getEntity().getName().getString() : "TARGET  --";
        String keys = "W " + key(mc.options.keyUp.isDown())
                + "  A " + key(mc.options.keyLeft.isDown())
                + "  S " + key(mc.options.keyDown.isDown())
                + "  D " + key(mc.options.keyRight.isDown())
                + "  SPACE " + key(mc.options.keyJump.isDown())
                + "  SHIFT " + key(mc.options.keyShift.isDown());
        String modules = "AIM " + on(ModuleController.aimAssist)
                + "   TRIGGER " + on(ModuleController.triggerBot)
                + "   SPRINT " + on(ModuleController.autoSprint)
                + "   TOOL " + on(ModuleController.autoTool);
        STATE.set(new Snapshot(x[0], y[0], Math.max(1, width[0]), Math.max(1, height[0]),
                coords, fps, target, keys, modules));

        SwingUtilities.invokeLater(() -> {
            if (frame == null || panel == null) return;
            Snapshot s = STATE.get();
            frame.setBounds(s.x, s.y, s.width, s.height);
            if (!frame.isVisible()) frame.setVisible(true);
            panel.repaint();
        });
    }

    public static void hide() {
        if (!WINDOWS) return;
        SwingUtilities.invokeLater(() -> {
            if (frame != null && frame.isVisible()) frame.setVisible(false);
        });
    }

    public static void close() {
        if (!WINDOWS) return;
        SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                frame.dispose();
                frame = null;
                panel = null;
            }
        });
    }

    private static String on(boolean value) { return value ? "ON" : "OFF"; }
    private static String key(boolean value) { return value ? "[X]" : "[ ]"; }

    private static void enableClickThrough(JFrame window) {
        try {
            Pointer hwnd = Native.getComponentPointer(window);
            Function get = NativeLibrary.getInstance("user32").getFunction("GetWindowLongW");
            Function set = NativeLibrary.getInstance("user32").getFunction("SetWindowLongW");
            int style = get.invokeInt(new Object[]{hwnd, -20});
            int flags = 0x00080000 | 0x00000020 | 0x00000080 | 0x08000000;
            set.invokeInt(new Object[]{hwnd, -20, style | flags});
        } catch (Throwable ignored) {
            // Keep the HUD visible if native click-through styling is unavailable.
        }
    }

    private record Snapshot(int x, int y, int width, int height,
                            String coords, String fps, String target, String keys, String modules) {
        private static final Snapshot EMPTY =
                new Snapshot(0, 0, 640, 360, "XYZ  --", "FPS  --", "TARGET  --", "", "");
    }

    private static final class HudPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Snapshot s = STATE.get();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font("Segoe UI", Font.BOLD, 14));
                drawPanel(g, 18, 18, 250, 84);
                g.setColor(new Color(116, 178, 255));
                g.drawString("SYSTEMS REBORN", 32, 42);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.drawString(s.coords, 32, 64);
                g.drawString(s.fps, 32, 84);

                drawPanel(g, 18, 110, 390, 48);
                g.setColor(new Color(210, 220, 238));
                g.drawString(s.modules, 32, 139);

                drawPanel(g, 18, 168, 390, 58);
                g.setColor(Color.WHITE);
                g.drawString(s.target, 32, 190);
                g.setColor(new Color(210, 220, 238));
                g.drawString(s.keys, 32, 211);
            } finally {
                g.dispose();
            }
        }

        private void drawPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(13, 17, 27, 218));
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(new Color(94, 135, 190, 180));
            g.drawRoundRect(x, y, w, h, 12, 12);
        }
    }
}
