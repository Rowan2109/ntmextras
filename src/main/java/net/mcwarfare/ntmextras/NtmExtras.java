// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras;

import com.hbm.registration.Reg;

/** Holds the content that NTM NEXT 1.0.3 is missing compared to the 1.7.10 mod. */
public final class NtmExtras {
    public static final String ID = "ntmextras";
    public static final Reg.Namespace REG = Reg.in(ID);

    private NtmExtras() {}

    public static void register() {
        load(ExtrasFluids.class, ExtrasItems.class, net.mcwarfare.ntmextras.structures.ExtrasStructures.class, net.mcwarfare.ntmextras.lambda.LambdaContent.class, net.mcwarfare.ntmextras.networks.NetworkBlocks.class, net.mcwarfare.ntmextras.tools.ToolsContent.class);
    }

    private static void load(Class<?>... holders) {
        for (Class<?> holder : holders) {
            try {
                Class.forName(holder.getName(), true, holder.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("content holder is missing: " + holder.getName(), e);
            }
        }
    }
}
