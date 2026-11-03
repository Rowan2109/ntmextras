// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools.mixin;

import com.hbm.blocks.generic.BlockLootCrate;
import java.util.List;
import net.mcwarfare.ntmextras.tools.ToolsContent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The weapon crate's list is private and cached; the launcher goes in with weight 2 like in the 1.7.10 mod. */
@Mixin(BlockLootCrate.class)
public abstract class MixinBlockLootCrate {

    @Inject(method = "weaponList", at = @At("RETURN"))
    private static void ntmextras$addLauncher(CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> list = cir.getReturnValue();
        for (ItemStack stack : list) {
            if (stack.is(ToolsContent.GUN_FLIEGERFAUST.get())) return;
        }
        for (int i = 0; i < 2; i++) list.add(new ItemStack(ToolsContent.GUN_FLIEGERFAUST.get()));
    }
}
