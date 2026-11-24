package dev.systemsreborn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public final class ModuleController {
    public static volatile boolean aimAssist = false;
    public static volatile boolean triggerBot = false;
    public static volatile boolean autoSprint = false;
    public static volatile boolean desktopHud = true;

    private static int triggerDelayTicks;

    private ModuleController() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        if (autoSprint && mc.options.keyUp.isDown()) mc.player.setSprinting(true);
        if (aimAssist) runAimAssist(mc);
        if (triggerBot) runTriggerBot(mc);
        if (triggerDelayTicks > 0) triggerDelayTicks--;
    }

    private static void runAimAssist(Minecraft mc) {
        Player self = mc.player;
        if (!mc.options.keyAttack.isDown()) return;

        Player best = null;
        double bestScore = Double.MAX_VALUE;
        final double maxDistance = 4.5;
        final double maxAngle = 10.0;

        for (Player candidate : mc.level.players()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator()) continue;
            if (!candidate.isAttackable() || !self.hasLineOfSight(candidate)) continue;

            double distance = self.distanceTo(candidate);
            if (distance > maxDistance) continue;

            var delta = candidate.getEyePosition().subtract(self.getEyePosition());
            double targetYaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
            double targetPitch = -Math.toDegrees(Math.atan2(
                    delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
            double yawDiff = wrapDegrees(targetYaw - self.getYRot());
            double pitchDiff = targetPitch - self.getXRot();
            double angle = Math.hypot(yawDiff, pitchDiff);
            if (angle > maxAngle) continue;

            double score = angle + distance * 0.015;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) return;
        var delta = best.getEyePosition().subtract(self.getEyePosition());
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(
                delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        self.setYRot(self.getYRot() + (float) (wrapDegrees(targetYaw - self.getYRot()) * 0.10));
        self.setXRot(self.getXRot() + (targetPitch - self.getXRot()) * 0.10f);
    }

    private static void runTriggerBot(Minecraft mc) {
        if (triggerDelayTicks > 0 || mc.hitResult == null
                || !(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        if (!(target instanceof Player player) || player == mc.player
                || !player.isAlive() || player.isSpectator()) return;
        if (!mc.player.hasLineOfSight(player) || mc.player.distanceTo(player) > 4.2) return;
        if (!mc.options.keyAttack.isDown() || mc.gameMode == null) return;
        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;

        mc.gameMode.attack(mc.player, player);
        mc.player.swing(InteractionHand.MAIN_HAND);
        triggerDelayTicks = 3;
    }

    private static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) value -= 360.0;
        if (value < -180.0) value += 360.0;
        return value;
    }
}
