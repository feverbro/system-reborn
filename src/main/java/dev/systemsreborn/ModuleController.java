package dev.systemsreborn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class ModuleController {
    public static volatile boolean aimAssist = false;
    public static volatile boolean aimBot = false;
    public static volatile boolean legitAura = false;
    public static volatile boolean triggerBot = false;
    public static volatile boolean clickAssist = false;
    public static volatile boolean wTap = false;
    public static volatile boolean sTap = false;
    public static volatile boolean autoSprint = false;
    public static volatile boolean desktopHud = true;

    public static volatile boolean hudModuleList = true;
    public static volatile boolean hudKeystrokes = true;
    public static volatile boolean hudCps = true;
    public static volatile boolean hudFps = true;
    public static volatile boolean hudPing = true;
    public static volatile boolean hudCoordinates = true;
    public static volatile boolean hudSpeed = true;
    public static volatile boolean hudTargetInfo = true;
    public static volatile boolean hudArmor = true;
    public static volatile boolean hudEffects = true;
    public static volatile boolean hudEditor = false;

    private static volatile int hudScaleIndex = 1;
    private static volatile int accentIndex = 0;
    private static int triggerDelayTicks;
    private static int clickDelayTicks;
    private static int auraDelayTicks;
    private static int sprintResetTicks;

    private static final float[] HUD_SCALES = {0.80f, 1.0f, 1.20f};
    private static final int[] ACCENTS = {0xFF74B2FF, 0xFF9B7BFF, 0xFF55D6A2, 0xFFFFB45E};

    private ModuleController() {}

    public static void tick(Minecraft mc) {
        if (triggerDelayTicks > 0) triggerDelayTicks--;
        if (clickDelayTicks > 0) clickDelayTicks--;
        if (auraDelayTicks > 0) auraDelayTicks--;
        if (mc.player == null || mc.level == null) return;
        if (sprintResetTicks > 0) {
            mc.player.setSprinting(false);
            sprintResetTicks--;
        }
        if (mc.screen != null) return;

        if (autoSprint && mc.options.keyUp.isDown() && sprintResetTicks == 0) {
            mc.player.setSprinting(true);
        }

        if (aimAssist && mc.options.keyAttack.isDown()) {
            Player target = findTarget(mc, 4.5, 10.0);
            if (target != null) rotateTo(mc, target, 0.10f);
        }

        if (aimBot && mc.options.keyAttack.isDown()) {
            Player target = findTarget(mc, 6.0, 28.0);
            if (target != null) rotateTo(mc, target, 0.32f);
        }

        if (legitAura && auraDelayTicks == 0) {
            Player target = findTarget(mc, 3.2, 32.0);
            if (target != null) {
                double angle = angleTo(mc, target);
                rotateTo(mc, target, 0.20f);
                if (angle <= 5.0 && attack(mc, target)) {
                    auraDelayTicks = 4;
                    afterHit(mc);
                }
            }
        }

        Player crosshairTarget = getCrosshairPlayer(mc);
        if (triggerBot && triggerDelayTicks == 0 && crosshairTarget != null) {
            if (attack(mc, crosshairTarget)) {
                triggerDelayTicks = 3;
                afterHit(mc);
            }
        }

        if (clickAssist && mc.options.keyAttack.isDown()
                && clickDelayTicks == 0 && crosshairTarget != null) {
            if (attack(mc, crosshairTarget)) {
                clickDelayTicks = 2;
                afterHit(mc);
            }
        }
    }

    private static Player findTarget(Minecraft mc, double maxDistance, double maxAngle) {
        Player self = mc.player;
        Player best = null;
        double bestScore = Double.MAX_VALUE;

        for (Player candidate : mc.level.players()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator()) continue;
            if (!candidate.isAttackable() || !self.hasLineOfSight(candidate)) continue;
            double distance = self.distanceTo(candidate);
            if (distance > maxDistance) continue;

            double angle = angleTo(mc, candidate);
            if (angle > maxAngle) continue;
            double score = angle + distance * 0.015;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static double angleTo(Minecraft mc, Player target) {
        Vec3 delta = target.getEyePosition().subtract(mc.player.getEyePosition());
        double targetYaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
        double targetPitch = -Math.toDegrees(Math.atan2(
                delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        double yawDiff = wrapDegrees(targetYaw - mc.player.getYRot());
        double pitchDiff = targetPitch - mc.player.getXRot();
        return Math.hypot(yawDiff, pitchDiff);
    }

    private static void rotateTo(Minecraft mc, Player target, float strength) {
        Vec3 delta = target.getEyePosition().subtract(mc.player.getEyePosition());
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(
                delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        mc.player.setYRot(mc.player.getYRot()
                + (float) (wrapDegrees(targetYaw - mc.player.getYRot()) * strength));
        mc.player.setXRot(mc.player.getXRot()
                + (targetPitch - mc.player.getXRot()) * strength);
    }

    private static Player getCrosshairPlayer(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player player
                && player != mc.player && player.isAlive() && !player.isSpectator()
                && mc.player.hasLineOfSight(player) && mc.player.distanceTo(player) <= 4.2) {
            return player;
        }
        return null;
    }

    private static boolean attack(Minecraft mc, Player target) {
        if (mc.gameMode == null || mc.player.getAttackStrengthScale(0.0f) < 1.0f) return false;
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private static void afterHit(Minecraft mc) {
        if (wTap) sprintResetTicks = Math.max(sprintResetTicks, 2);
        if (sTap) {
            Vec3 velocity = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(velocity.x * 0.65, velocity.y, velocity.z * 0.65);
            mc.player.setSprinting(false);
        }
    }

    public static float hudScale() {
        return HUD_SCALES[hudScaleIndex];
    }

    public static void cycleHudScale() {
        hudScaleIndex = (hudScaleIndex + 1) % HUD_SCALES.length;
    }

    public static int accentColor() {
        return ACCENTS[accentIndex];
    }

    public static void cycleAccentColor() {
        accentIndex = (accentIndex + 1) % ACCENTS.length;
    }

    private static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) value -= 360.0;
        if (value < -180.0) value += 360.0;
        return value;
    }
}
