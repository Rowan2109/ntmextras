// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras;

import com.hbm.items.CreativeContents;
import com.hbm.items.CreativeContents.Tab;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ExtrasItems {
    public static final Reg.Handle<Item> CHUNK_ORE_ILMENITE = plain("chunk_ore_ilmenite");
    public static final Reg.Handle<Item> PLATE_ALCLAD = plain("plate_alclad");

    private ExtrasItems() {}

    static {
        CreativeContents.insertAfter(Tab.PARTS, Library.id("chunk_ore_cryolite"), () -> new ItemStack(CHUNK_ORE_ILMENITE.get()));
        CreativeContents.insertAfter(Tab.PARTS, Library.id("chunk_ore_cryolite"), () -> new ItemStack(PLATE_ALCLAD.get()));
    }

    private static Reg.Handle<Item> plain(String name) {
        return NtmExtras.REG.item(name, Item::new, Item.Properties::new);
    }
}
