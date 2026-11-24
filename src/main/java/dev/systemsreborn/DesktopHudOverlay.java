package dev.systemsreborn;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class DesktopHudOverlay {
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final AtomicReference<Snapshot> STATE = new AtomicReference<>(Snapshot.EMPTY);
    private static JFrame frame;
    private static JFrame editor;
    private static HudPanel panel;
    private static volatile boolean showTarget = true, showKeys = true, showModules = true;
    private static volatile boolean showStats = true;
    private static volatile float scale = 1.0f;
    private static volatile int opacity = 218, offsetX = 18, offsetY = 18;
    private static final ArrayDeque<Long> CLICK_TIMES = new ArrayDeque<>();
    private static boolean wasLeftDown;

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
            frame.setSize(720, 440);
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

        long now = System.currentTimeMillis();
        boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (leftDown && !wasLeftDown) CLICK_TIMES.addLast(now);
        wasLeftDown = leftDown;
        while (!CLICK_TIMES.isEmpty() && now - CLICK_TIMES.peekFirst() > 1000L) CLICK_TIMES.removeFirst();

        int[] x = new int[1], y = new int[1], width = new int[1], height = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetFramebufferSize(handle, width, height);

        String coords = mc.player == null ? "XYZ  --" : String.format(Locale.ROOT,
                "XYZ  %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        String fps = "FPS  " + Minecraft.getInstance().getFps();
        String ping = "PING  --";
        if (mc.player != null && mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) ping = "PING  " + info.getLatency() + " ms";
        }
        String targetName = "TARGET  --";
        String targetHealth = "HP  --";
        String targetDistance = "DIST  --";
        String targetArmor = "ARMOR  --";
        float healthPercent = 0.0f;
        if (mc.hitResult instanceof EntityHitResult hit && mc.player != null) {
            Entity entity = hit.getEntity();
            targetName = "TARGET  " + entity.getName().getString();
            targetDistance = String.format(Locale.ROOT, "DIST  %.1f m", mc.player.distanceTo(entity));
            if (entity instanceof Player player) {
                healthPercent = Math.max(0.0f, Math.min(1.0f, player.getHealth() / Math.max(1.0f, player.getMaxHealth())));
                targetHealth = String.format(Locale.ROOT, "HP  %.1f / %.1f", player.getHealth(), player.getMaxHealth());
                targetArmor = "ARMOR  " + player.getArmorValue();
            } else if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                healthPercent = Math.max(0.0f, Math.min(1.0f, living.getHealth() / Math.max(1.0f, living.getMaxHealth())));
                targetHealth = String.format(Locale.ROOT, "HP  %.1f / %.1f", living.getHealth(), living.getMaxHealth());
            }
        }
        String keys = "W " + key(mc.options.keyUp.isDown())
                + "  A " + key(mc.options.keyLeft.isDown())
                + "  S " + key(mc.options.keyDown.isDown())
                + "  D " + key(mc.options.keyRight.isDown())
                + "  SPACE " + key(mc.options.keyJump.isDown())
                + "  SHIFT " + key(mc.options.keyShift.isDown());
        String modules = "AIM " + on(ModuleController.aimAssist)
                + "   TRIGGER " + on(ModuleController.triggerBot)
                + "   SPRINT " + on(ModuleController.autoSprint);
        STATE.set(new Snapshot(x[0], y[0], Math.max(1, width[0]), Math.max(1, height[0]),
                coords, "FPS  " + Minecraft.getInstance().getFps() + "   " + ping,
                targetName, targetHealth, targetDistance, targetArmor, keys, modules,
                CLICK_TIMES.size(), healthPercent));

        SwingUtilities.invokeLater(() -> {
            if (frame == null || panel == null) return;
            Snapshot s = STATE.get();
            frame.setBounds(s.x, s.y, s.width, s.height);
            if (!frame.isVisible()) frame.setVisible(true);
            panel.repaint();
        });
    }

    public static void openEditor() {
        if (!WINDOWS) return;
        SwingUtilities.invokeLater(() -> {
            if (editor != null && editor.isDisplayable()) {
                editor.setVisible(true);
                editor.toFront();
                return;
            }
            editor = new JFrame("SYSTEMS REBORN — HUD EDITOR");
            editor.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            editor.setAlwaysOnTop(true);
            JPanel root = new JPanel();
            root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            JLabel heading = new JLabel("HUD ELEMENTS");
            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
            root.add(heading);
            root.add(Box.createVerticalStrut(8));
            addToggle(root, "Target panel (health / distance / armor)", true, v -> showTarget = v);
            addToggle(root, "Keystrokes", true, v -> showKeys = v);
            addToggle(root, "Module list", true, v -> showModules = v);
            addToggle(root, "FPS / Ping / Coordinates / CPS", true, v -> showStats = v);
            root.add(Box.createVerticalStrut(8));
            root.add(new JLabel("Scale"));
            JSlider scaleSlider = new JSlider(75, 150, 100);
            scaleSlider.setMajorTickSpacing(25);
            scaleSlider.setPaintTicks(true);
            scaleSlider.setPaintLabels(true);
            scaleSlider.addChangeListener(e -> scale = scaleSlider.getValue() / 100.0f);
            root.add(scaleSlider);
            root.add(new JLabel("Background opacity"));
            JSlider alphaSlider = new JSlider(80, 255, opacity);
            alphaSlider.setMajorTickSpacing(35);
            alphaSlider.setPaintTicks(true);
            alphaSlider.addChangeListener(e -> opacity = alphaSlider.getValue());
            root.add(alphaSlider);
            root.add(new JLabel("HUD horizontal position"));
            JSlider xSlider = new JSlider(0, 120, offsetX);
            xSlider.addChangeListener(e -> offsetX = xSlider.getValue());
            root.add(xSlider);
            root.add(new JLabel("HUD vertical position"));
            JSlider ySlider = new JSlider(0, 120, offsetY);
            ySlider.addChangeListener(e -> offsetY = ySlider.getValue());
            root.add(ySlider);
            JButton close = new JButton("Close");
            close.addActionListener(e -> editor.setVisible(false));
            root.add(Box.createVerticalStrut(8));
            root.add(close);
            editor.setContentPane(root);
            editor.pack();
            editor.setMinimumSize(new Dimension(330, 440));
            editor.setLocationRelativeTo(null);
            editor.setVisible(true);
        });
    }

    private static void addToggle(JPanel root, String label, boolean initial,
                                  java.util.function.Consumer<Boolean> setter) {
        JCheckBox box = new JCheckBox(label, initial);
        box.addActionListener(e -> setter.accept(box.isSelected()));
        root.add(box);
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
            if (editor != null) { editor.dispose(); editor = null; }
            if (frame != null) { frame.dispose(); frame = null; panel = null; }
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

    private record Snapshot(int x, int y, int width, int height, String coords, String fps,
                            String target, String targetHealth, String targetDistance, String targetArmor,
                            String keys, String modules, int cps, float healthPercent) {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 720, 440, "XYZ  --", "FPS  --",
                "TARGET  --", "HP  --", "DIST  --", "ARMOR  --", "", "", 0, 0.0f);
    }

    private static final class HudPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Snapshot s = STATE.get();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.scale(scale, scale);
                int ox = offsetX, oy = offsetY;
                if (showStats) {
                    drawPanel(g, ox, oy, 270, 86);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g.setColor(new Color(116, 178, 255));
                    g.drawString("SYSTEMS REBORN", ox + 14, oy + 24);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.setColor(Color.WHITE);
                    g.drawString(s.coords, ox + 14, oy + 46);
                    g.drawString(s.fps + "   CPS  " + s.cps, ox + 14, oy + 66);
                }
                if (showModules) {
                    drawPanel(g, ox, oy + 96, 390, 44);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.setColor(new Color(210, 220, 238));
                    g.drawString(s.modules, ox + 14, oy + 123);
                }
                if (showTarget) {
                    drawPanel(g, ox, oy + 150, 300, 112);
                    g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    g.setColor(Color.WHITE);
                    g.drawString(s.target, ox + 14, oy + 174);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.setColor(new Color(210, 220, 238));
                    g.drawString(s.targetHealth, ox + 14, oy + 195);
                    g.drawString(s.targetDistance + "   " + s.targetArmor, ox + 14, oy + 215);
                    g.setColor(new Color(48, 57, 74));
                    g.fillRoundRect(ox + 14, oy + 228, 270, 8, 8, 8);
                    g.setColor(new Color(94, 220, 142));
                    g.fillRoundRect(ox + 14, oy + 228, Math.round(270 * s.healthPercent), 8, 8, 8);
                }
                if (showKeys) {
                    drawPanel(g, ox, oy + 272, 390, 48);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g.setColor(new Color(210, 220, 238));
                    g.drawString(s.keys, ox + 14, oy + 301);
                }
            } finally {
                g.dispose();
            }
        }

        private void drawPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(13, 17, 27, opacity));
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(new Color(94, 135, 190, 180));
            g.drawRoundRect(x, y, w, h, 12, 12);
        }
    }
}
