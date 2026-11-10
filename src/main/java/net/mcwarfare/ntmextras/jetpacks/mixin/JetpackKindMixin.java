// SPDX-FileCopyrightText: 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.jetpacks.mixin;

import com.hbm.items.armor.ArmorSuitEffects;
import net.mcwarfare.ntmextras.jetpacks.JetpackSpeed;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sets the lower top speed (JetpackSpeed) on the jetpack kinds when the enum is built, on client and server alike, so both
 * sides read the same speedCap.
 */
@Mixin(value = ArmorSuitEffects.Jetpack.class, remap = false)
public abstract class JetpackKindMixin {
    @Shadow
    @Final
    @Mutable
    public double speedCap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ntmextras$topSpeed(String name, int ordinal, double ceiling, double thrust, double lookThrust, double speedCap,
            int burnRate, int hoverBurnRate, float pitch, CallbackInfo ci) {
        double cap = JetpackSpeed.cap(name);
        if (cap > 0) {
            this.speedCap = cap;
        }
    }
}
