package dev.systemsreborn;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.InteractionHand;

public final class ModuleController {
    public static boolean aimAssist = false;
    public static boolean triggerBot = false;
    public static boolean autoSprint = false;
    public static boolean noBob = false;
    private static int triggerDelayTicks = 0;

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
        if (!mc.options.keyUse.isDown()) return;
        Player best = null;
        double bestAngle = 12.0;
        for (Player candidate : mc.level.players()) {
            if (candidate == self || !candidate.isAlive() || candidate.isSpectator()) continue;
            double distance = self.distanceTo(candidate);
            if (distance > 5.0) continue;
            var delta = candidate.getEyePosition().subtract(self.getEyePosition());
            double targetYaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
            double targetPitch = -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
            double yawDiff = wrapDegrees(targetYaw - self.getYRot());
            double pitchDiff = targetPitch - self.getXRot();
            double angle = Math.hypot(yawDiff, pitchDiff);
            if (angle < bestAngle) { bestAngle = angle; best = candidate; }
        }
        if (best != null) {
            var delta = best.getEyePosition().subtract(self.getEyePosition());
            float targetYaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float targetPitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
            self.setYRot(self.getYRot() + (float)(wrapDegrees(targetYaw - self.getYRot()) * 0.12));
            self.setXRot(self.getXRot() + (targetPitch - self.getXRot()) * 0.12f);
        }
    }

    private static void runTriggerBot(Minecraft mc) {
        if (triggerDelayTicks > 0 || mc.hitResult == null || !(mc.hitResult instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        if (!(target instanceof Player player) || player == mc.player || !player.isAlive() || player.isSpectator()) return;
        if (mc.player.distanceTo(player) > 4.2 || mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;
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
