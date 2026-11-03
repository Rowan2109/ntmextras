// SPDX-FileCopyrightText: 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras;

import com.hbm.registration.NeoForgeRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NtmExtras.ID)
public final class NtmExtrasNeoForge {
    public NtmExtrasNeoForge(IEventBus modBus) {
        NeoForgeRegistrar.addon(NtmExtras.ID, modBus, NtmExtras::register);
    }
}
