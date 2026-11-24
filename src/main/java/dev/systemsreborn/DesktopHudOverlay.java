package dev.systemsreborn;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class DesktopHudOverlay {
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final AtomicReference<Snapshot> STATE = new AtomicReference<>(Snapshot.EMPTY);
    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    private static JFrame frame;
    private static HudPanel panel;
    private static boolean previousLeftDown;
    private static boolean previousEditor;
    private static int offsetX;
    private static int offsetY;
    private static int dragX;
    private static int dragY;

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
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (ModuleController.hudEditor) {
                        dragX = e.getXOnScreen();
                        dragY = e.getYOnScreen();
                    }
                }
            });
            panel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!ModuleController.hudEditor || frame == null) return;
                    int dx = e.getXOnScreen() - dragX;
                    int dy = e.getYOnScreen() - dragY;
                    dragX = e.getXOnScreen();
                    dragY = e.getYOnScreen();
                    offsetX += dx;
                    offsetY += dy;
                    frame.setLocation(frame.getX() + dx, frame.getY() + dy);
                }
            });
            frame.setContentPane(panel);
            frame.setSize(720, 390);
            frame.setLocation(0, 0);
            frame.setVisible(true);
            setClickThrough(frame, true);
        });
    }

    public static void update(Minecraft mc) {
        if (!WINDOWS || mc == null || mc.getWindow() == null) return;
        long handle = mc.getWindow().handle();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) {
            hide();
            return;
        }

        boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (leftDown && !previousLeftDown) CLICKS.addLast(now);
        previousLeftDown = leftDown;
        while (!CLICKS.isEmpty() && now - CLICKS.peekFirst() > 1000L) CLICKS.removeFirst();

        int[] x = new int[1], y = new int[1], width = new int[1], height = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetFramebufferSize(handle, width, height);

        String coords = mc.player == null || !ModuleController.hudCoordinates ? ""
                : String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        String fps = ModuleController.hudFps ? "FPS  " + Minecraft.getInstance().getFps() : "";
        String ping = "";
        String speed = "";
        String effects = "";
        if (mc.player != null) {
            if (ModuleController.hudPing && mc.getConnection() != null
                    && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
                ping = "PING  " + mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency() + " ms";
            }
            if (ModuleController.hudSpeed) {
                double horizontal = Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x
                        + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
                speed = String.format(Locale.ROOT, "SPEED  %.2f m/s", horizontal * 20.0);
            }
            if (ModuleController.hudEffects) {
                effects = "EFFECTS  " + mc.player.getActiveEffects().size();
            }
        }

        String target = "";
        String targetStats = "";
        if (ModuleController.hudTargetInfo && mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && mc.player != null) {
            target = "TARGET  " + living.getName().getString();
            targetStats = String.format(Locale.ROOT, "HP  %.1f / %.1f    DIST  %.1f m",
                    living.getHealth(), living.getMaxHealth(), mc.player.distanceTo(living));
            if (ModuleController.hudArmor && living instanceof Player player) {
                targetStats += "    ARMOR  " + player.getArmorValue();
            }
        }

        String keys = "";
        if (ModuleController.hudKeystrokes && mc.player != null) {
            keys = "W " + key(mc.options.keyUp.isDown())
                    + "   A " + key(mc.options.keyLeft.isDown())
                    + "   S " + key(mc.options.keyDown.isDown())
                    + "   D " + key(mc.options.keyRight.isDown())
                    + "   SPACE " + key(mc.options.keyJump.isDown())
                    + "   SHIFT " + key(mc.options.keyShift.isDown());
        }

        String modules = ModuleController.hudModuleList ? activeModules() : "";
        String cps = ModuleController.hudCps ? "CPS  " + CLICKS.size() : "";
        STATE.set(new Snapshot(x[0], y[0], Math.max(1, width[0]), Math.max(1, height[0]),
                coords, fps, ping, speed, effects, target, targetStats, keys, modules, cps,
                ModuleController.hudEditor));

        boolean editor = ModuleController.hudEditor;
        SwingUtilities.invokeLater(() -> {
            if (frame == null || panel == null) return;
            if (editor != previousEditor) {
                setClickThrough(frame, !editor);
                previousEditor = editor;
            }
            Snapshot s = STATE.get();
            frame.setBounds(s.x + offsetX, s.y + offsetY, 720, 390);
            if (!frame.isVisible()) frame.setVisible(true);
            panel.repaint();
        });
    }

    private static String activeModules() {
        StringBuilder result = new StringBuilder("ACTIVE");
        if (ModuleController.aimAssist) result.append("  AIM ASSIST");
        if (ModuleController.aimBot) result.append("  AIM BOT");
        if (ModuleController.legitAura) result.append("  LEGIT AURA");
        if (ModuleController.triggerBot) result.append("  TRIGGER");
        if (ModuleController.clickAssist) result.append("  CLICK");
        if (ModuleController.wTap) result.append("  W-TAP");
        if (ModuleController.sTap) result.append("  S-TAP");
        if (ModuleController.autoSprint) result.append("  SPRINT");
        return result.toString();
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

    private static String key(boolean value) { return value ? "[X]" : "[ ]"; }

    private static void setClickThrough(JFrame window, boolean enabled) {
        try {
            Pointer hwnd = Native.getComponentPointer(window);
            Function get = NativeLibrary.getInstance("user32").getFunction("GetWindowLongW");
            Function set = NativeLibrary.getInstance("user32").getFunction("SetWindowLongW");
            int style = get.invokeInt(new Object[]{hwnd, -20});
            int flags = 0x00080000 | 0x00000080 | 0x08000000;
            if (enabled) flags |= 0x00000020;
            else style &= ~0x00000020;
            set.invokeInt(new Object[]{hwnd, -20, enabled ? style | flags : style | flags});
        } catch (Throwable ignored) {
            // Keep the overlay visible if native window styling is unavailable.
        }
    }

    private record Snapshot(int x, int y, int width, int height,
                            String coords, String fps, String ping, String speed, String effects,
                            String target, String targetStats, String keys, String modules, String cps,
                            boolean editor) {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 720, 390,
                "", "", "", "", "", "", "", "", "", "", false);
    }

    private static final class HudPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Snapshot s = STATE.get();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.scale(ModuleController.hudScale(), ModuleController.hudScale());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color accent = new Color(ModuleController.accentColor(), true);
                g.setFont(new Font("Segoe UI", Font.BOLD, 14));

                drawPanel(g, 18, 18, 285, 112, accent);
                g.setColor(accent);
                g.drawString("SYSTEMS REBORN", 32, 42);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                int line = 64;
                for (String value : new String[]{s.coords, s.fps, s.ping, s.speed, s.effects}) {
                    if (!value.isEmpty()) {
                        g.drawString(value, 32, line);
                        line += 17;
                    }
                }

                if (!s.modules.isEmpty()) {
                    drawPanel(g, 318, 18, 380, 62, accent);
                    g.setColor(accent);
                    g.drawString("MODULE LIST", 332, 40);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g.drawString(s.modules, 332, 61);
                }

                if (!s.keys.isEmpty()) {
                    drawPanel(g, 18, 142, 350, 54, accent);
                    g.setColor(accent);
                    g.drawString("KEYSTROKES", 32, 164);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.drawString(s.keys, 32, 184);
                }

                if (!s.target.isEmpty()) {
                    drawPanel(g, 18, 208, 350, 70, accent);
                    g.setColor(accent);
                    g.drawString("COMBAT HUD", 32, 231);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g.drawString(s.target, 32, 250);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g.drawString(s.targetStats, 32, 268);
                }

                if (!s.cps.isEmpty()) {
                    drawPanel(g, 382, 142, 145, 54, accent);
                    g.setColor(accent);
                    g.drawString("CLICKS", 396, 164);
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    g.drawString(s.cps, 396, 185);
                }

                if (s.editor) {
                    g.setColor(new Color(255, 255, 255, 220));
                    g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    g.drawString("HUD EDITOR: DRAG THE OVERLAY TO MOVE IT  |  F7 TO EXIT", 18, 315);
                }
            } finally {
                g.dispose();
            }
        }

        private void drawPanel(Graphics2D g, int x, int y, int w, int h, Color accent) {
            g.setColor(new Color(13, 17, 27, 218));
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
            g.drawRoundRect(x, y, w, h, 12, 12);
        }
    }
}
