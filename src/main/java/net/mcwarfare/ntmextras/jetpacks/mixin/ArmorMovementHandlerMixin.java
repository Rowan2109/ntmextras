// SPDX-FileCopyrightText: 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.jetpacks.mixin;

import com.hbm.client.ArmorMovementHandler;
import com.hbm.items.armor.ArmorSuitEffects;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * NTM adds the look-direction push while the speed is still under speedCap, so one push can overshoot it (the Boostpack's
 * push is 0.25 a tick). Only as much of the push is kept as brings the speed up to speedCap, so the pack tops out at the
 * cap itself; the movement the player already has (the vertical thrust, a fall) is left alone.
 */
@Mixin(value = ArmorMovementHandler.class, remap = false)
public abstract class ArmorMovementHandlerMixin {
    @ModifyArg(method = "jetpack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", ordinal = 1))
    private static Vec3 ntmextras$topOut(Vec3 motion, @Local(argsOnly = true) LocalPlayer player,
            @Local(argsOnly = true) ArmorSuitEffects.Jetpack kind) {
        double cap = kind.speedCap;
        if (kind.lookThrust <= 0 || motion.lengthSqr() <= cap * cap) {
            return motion;
        }
        Vec3 before = player.getDeltaMovement();
        Vec3 push = motion.subtract(before);
        double a = push.lengthSqr();
        double c = before.lengthSqr() - cap * cap;
        if (a < 1.0E-9 || c >= 0) {
            return c >= 0 ? before : motion;
        }
        // |before + s * push| = cap, 0 <= s <= 1
        double b = before.dot(push);
        double s = (-b + Math.sqrt(b * b - a * c)) / a;
        return before.add(push.scale(Math.min(1, Math.max(0, s))));
    }
}
