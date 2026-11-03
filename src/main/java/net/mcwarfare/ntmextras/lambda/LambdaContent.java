// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.items.CreativeContents;
import com.hbm.items.CreativeContents.Tab;
import com.hbm.lib.Library;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.Set;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class LambdaContent {

    public static final Reg.Handle<LaunchpadLambda> LAUNCHPAD_LAMBDA =
            NtmExtras.REG.blockItem(
                    "launchpad_lambda",
                    LaunchpadLambda::new,
                    () ->
                            BlockBehaviour.Properties.of()
                                    .strength(5.0F, 10.0F)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.METAL)
                                    .noOcclusion());

    public static final Reg.BeHandle<BlockEntityLaunchpadLambda> LAUNCHPAD_LAMBDA_BE =
            NtmExtras.REG
                    .be(
                            "launchpad_lambda",
                            () ->
                                    new BlockEntityType<>(
                                            BlockEntityLaunchpadLambda::new,
                                            Set.of(LAUNCHPAD_LAMBDA.get())))
                    .ticks(BlockEntityMachineBase::tickClient, BlockEntityMachineBase::tickServer);

    public static final RegistryHandle<MenuType<MenuLaunchpadLambda>> LAUNCHPAD_LAMBDA_MENU =
            NeoForgeRegistrar.registerBlockEntityMenu(
                    NtmExtras.ID + ":launchpad_lambda", BlockEntityLaunchpadLambda.class);

    public static final Reg.Handle<Item> MISSILE_LAMBDA =
            NtmExtras.REG.item(
                    "missile_lambda", Item::new, () -> new Item.Properties().stacksTo(1));

    public static final Reg.EntityHandle<EntityRocketLambda> ROCKET_LAMBDA =
            NtmExtras.REG.entity(
                    "entity_rocket_lambda",
                    () ->
                            EntityType.Builder.<EntityRocketLambda>of(
                                            EntityRocketLambda::new, MobCategory.MISC)
                                    .noLootTable()
                                    .fireImmune()
                                    .sized(3.0F, 25.0F)
                                    .clientTrackingRange(63)
                                    .updateInterval(1)
                                    .build(
                                            ResourceKey.create(
                                                    Registries.ENTITY_TYPE,
                                                    Identifier.fromNamespaceAndPath(
                                                            NtmExtras.ID,
                                                            "entity_rocket_lambda"))));

    static {
        CreativeContents.insertAfter(
                Tab.MISSILES,
                Library.id("launchpad_soyuz"),
                () -> new ItemStack(LAUNCHPAD_LAMBDA.get()));
        CreativeContents.insertAfter(
                Tab.MISSILES,
                Library.id("missile_soyuz_lander"),
                () -> new ItemStack(MISSILE_LAMBDA.get()));
    }

    private LambdaContent() {}
}
