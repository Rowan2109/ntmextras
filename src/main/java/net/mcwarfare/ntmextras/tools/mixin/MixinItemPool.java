// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The item pools are baked into NTM's jar, so the launcher joins the C-130 weapon drop pool here, with weight 20 like
 * in the 1.7.10 mod. The pools are weighted trees of alternatives; the old total of the weapon pool is 530.
 */
@Mixin(ItemPool.class)
public abstract class MixinItemPool {

    private static final int OLD_TOTAL = 530;
    private static final int WEIGHT = 20;

    @Inject(method = "root", at = @At("RETURN"))
    private static void ntmextras$addLauncher(
            String resource, CallbackInfoReturnable<JsonObject> cir) {
        JsonObject weapons = cir.getReturnValue().getAsJsonObject(ItemPools.POOL_WEAPONS);
        if (weapons == null) return;

        JsonObject pool = weapons.getAsJsonArray("pools").get(0).getAsJsonObject();
        JsonArray entries = pool.getAsJsonArray("entries");

        JsonObject value = new JsonObject();
        value.addProperty("type", "minecraft:uniform");
        value.addProperty("min", 0);
        value.addProperty("max", OLD_TOTAL + WEIGHT - 1);
        JsonObject range = new JsonObject();
        range.addProperty("max", WEIGHT - 1);
        JsonObject check = new JsonObject();
        check.addProperty("condition", "minecraft:value_check");
        check.add("range", range);
        check.add("value", value);
        JsonArray conditions = new JsonArray();
        conditions.add(check);

        JsonObject launcher = new JsonObject();
        launcher.addProperty("type", "minecraft:item");
        launcher.addProperty("name", "ntmextras:gun_fliegerfaust");
        launcher.addProperty("weight", WEIGHT);
        launcher.add("conditions", conditions);

        JsonArray children = new JsonArray();
        children.add(launcher);
        for (JsonElement old : entries) children.add(old);

        JsonObject tree = new JsonObject();
        tree.addProperty("type", "minecraft:alternatives");
        tree.add("children", children);

        JsonArray replaced = new JsonArray();
        replaced.add(tree);
        pool.add("entries", replaced);
    }
}
