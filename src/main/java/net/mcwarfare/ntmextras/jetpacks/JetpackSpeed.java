// SPDX-FileCopyrightText: 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.jetpacks;

/**
 * Top speeds for the jetpacks that push along the look direction. NTM NEXT lets the Vectored Jetpack reach 2 blocks a tick
 * and the Boostpack 5 (enum ArmorSuitEffects.Jetpack, speedCap); MC Warfare caps them near the old Tekkit jetpacks. The
 * vertical thrust (ceiling, thrust) is left as it is. The packs without look thrust have no top speed in NTM and get none
 * here. Changed from upstream 2026-11-09.
 */
public final class JetpackSpeed {
    /** Blocks per tick. */
    public static final double VECTOR = 0.6;
    public static final double BOOST = 0.75;

    private JetpackSpeed() {}

    /** The top speed for an ArmorSuitEffects.Jetpack constant, or 0 to keep NTM's. */
    public static double cap(String kind) {
        return switch (kind) {
            case "VECTOR" -> VECTOR;
            case "BOOST" -> BOOST;
            default -> 0;
        };
    }
}
