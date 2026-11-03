// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks;

import com.hbm.blocks.ModSoundTypes;
import com.hbm.client.model.AnchorModel;
import com.hbm.items.CreativeContents;
import com.hbm.items.CreativeContents.Tab;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import java.util.Set;
import net.mcwarfare.ntmextras.NtmExtras;
import net.mcwarfare.ntmextras.networks.client.NetworkSeparatorModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class NetworkBlocks {

    public static final Reg.Handle<PipeAnchorExhaustBlock> PIPE_ANCHOR_EXHAUST =
            NtmExtras.REG
                    .blockItem(
                            "pipe_anchor_exhaust",
                            PipeAnchorExhaustBlock::new,
                            NetworkBlocks::anchorProps,
                            (block, props) -> new AnchorBlockItem(block, props))
                    .bakedBy(() -> new AnchorModel());

    public static final Reg.Handle<PipeAnchorPneumaticBlock> PIPE_ANCHOR_PNEUMATIC =
            NtmExtras.REG
                    .blockItem(
                            "pipe_anchor_pneumatic",
                            PipeAnchorPneumaticBlock::new,
                            NetworkBlocks::anchorProps,
                            (block, props) -> new AnchorBlockItem(block, props))
                    .bakedBy(() -> new AnchorModel());

    public static final Reg.Handle<NetworkSeparatorBlock> NETWORK_SEPARATOR =
            NtmExtras.REG
                    .blockItem(
                            "network_separator",
                            NetworkSeparatorBlock::new,
                            () ->
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.METAL)
                                            .strength(5.0F, 6.0F)
                                            .requiresCorrectToolForDrops()
                                            .noOcclusion())
                    .bakedBy(() -> new NetworkSeparatorModel());

    public static final Reg.BeHandle<BlockEntityNetworkAnchor> ANCHOR_BE =
            NtmExtras.REG
                    .be(
                            "network_anchor",
                            () ->
                                    new BlockEntityType<>(
                                            BlockEntityNetworkAnchor::new,
                                            Set.of(
                                                    PIPE_ANCHOR_EXHAUST.get(),
                                                    PIPE_ANCHOR_PNEUMATIC.get())))
                    .ticksServer(BlockEntityNetworkAnchor::tickServer);

    public static final Reg.BeHandle<BlockEntityNetworkSeparator> SEPARATOR_BE =
            NtmExtras.REG
                    .be(
                            "network_separator",
                            () ->
                                    new BlockEntityType<>(
                                            BlockEntityNetworkSeparator::new,
                                            Set.of(NETWORK_SEPARATOR.get())))
                    .ticksServer(BlockEntityNetworkSeparator::tickServer);

    private NetworkBlocks() {}

    static {
        CreativeContents.insertAfter(
                Tab.MACHINES,
                Library.id("pipe_anchor"),
                () -> new ItemStack(PIPE_ANCHOR_EXHAUST.get()));
        CreativeContents.insertAfter(
                Tab.MACHINES,
                Library.id("pipe_anchor"),
                () -> new ItemStack(PIPE_ANCHOR_PNEUMATIC.get()));
        CreativeContents.insertAfter(
                Tab.MACHINES,
                Library.id("cable_diode"),
                () -> new ItemStack(NETWORK_SEPARATOR.get()));
    }

    private static BlockBehaviour.Properties anchorProps() {
        return BlockBehaviour.Properties.of()
                .strength(5.0F, 6.0F)
                .sound(ModSoundTypes.PIPE)
                .noOcclusion()
                .forceSolidOn()
                .requiresCorrectToolForDrops();
    }
}
