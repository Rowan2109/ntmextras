// SPDX-FileCopyrightText: 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Distant Horizons binds its Chunky listener from the chunk load event. On a new world the spawn chunk loads before
 * Chunky has started, so the bind throws, the chunk's load task dies and the server waits for that chunk forever.
 * Wait with the bind until Chunky is up; the next chunk load tries again.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractChunkyAccessor", remap = false)
public abstract class DhChunkySetupMixin {
    @Inject(method = "tryRunFirstTimeSetup", at = @At("HEAD"), cancellable = true, require = 0)
    private void ntmextras$waitForChunky(CallbackInfo ci) {
        if (!ntmextras$chunkyUp()) {
            ci.cancel();
        }
    }

    private static boolean ntmextras$chunkyUp() {
        try {
            Class.forName("org.popcraft.chunky.ChunkyProvider").getMethod("get").invoke(null);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}