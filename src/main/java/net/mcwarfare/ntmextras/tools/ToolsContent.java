// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools;

import com.hbm.items.CreativeContents;
import com.hbm.items.CreativeContents.Tab;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ToolsContent {
    public static final Reg.Handle<ItemRTTYDesignator> RTTY_DESIGNATOR =
            NtmExtras.REG.item(
                    "rtty_designator",
                    ItemRTTYDesignator::new,
                    () -> new Item.Properties().stacksTo(1));
    public static final Reg.Handle<Item> AMMO_ROCKET_MICRO =
            NtmExtras.REG.item("ammo_standard_rocket_micro", Item::new, Item.Properties::new);
    public static final Reg.Handle<ItemGunBaseNT> GUN_FLIEGERFAUST =
            NtmExtras.REG.item(
                    "gun_fliegerfaust", XFactoryFliegerfaust::create, Item.Properties::new);

    private ToolsContent() {}

    static {
        XFactoryFliegerfaust.init();
        CreativeContents.insertAfter(
                Tab.CONSUMABLES,
                Library.id("rtty_pager"),
                () -> new ItemStack(RTTY_DESIGNATOR.get()));
        CreativeContents.insertAfter(
                Tab.WEAPONS,
                Library.id("gun_quadro"),
                () -> new ItemStack(GUN_FLIEGERFAUST.get()));
        CreativeContents.insertBefore(
                Tab.WEAPONS,
                Library.id("ammo_standard_rocket_he"),
                () -> new ItemStack(AMMO_ROCKET_MICRO.get()));
    }
}
