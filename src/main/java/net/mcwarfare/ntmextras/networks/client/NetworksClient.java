// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks.client;

import net.mcwarfare.ntmextras.NtmExtras;
import net.mcwarfare.ntmextras.networks.NetworkBlocks;
import net.mcwarfare.ntmextras.networks.NetworkSeparatorBlock;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = NtmExtras.ID, dist = Dist.CLIENT)
public final class NetworksClient {

    public NetworksClient(IEventBus modBus) {
        modBus.addListener(
                (FMLClientSetupEvent event) ->
                        NetworkSeparatorBlock.OPEN_SCREEN =
                                separator ->
                                        Minecraft.getInstance()
                                                .gui
                                                .setScreen(new ScreenNetworkSeparator(separator)));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) ->
                        event.registerBlockEntityRenderer(
                                NetworkBlocks.ANCHOR_BE.get(), context -> new RenderNetworkAnchor()));
    }
}
