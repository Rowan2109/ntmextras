// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda.client;

import net.mcwarfare.ntmextras.NtmExtras;
import net.mcwarfare.ntmextras.lambda.LambdaContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = NtmExtras.ID, dist = Dist.CLIENT)
public final class LambdaClient {

    public LambdaClient(IEventBus modBus) {
        modBus.addListener(
                (RegisterMenuScreensEvent event) ->
                        event.register(
                                LambdaContent.LAUNCHPAD_LAMBDA_MENU.get(),
                                ScreenLaunchpadLambda::new));
        modBus.addListener(
                (EntityRenderersEvent.RegisterRenderers event) -> {
                    event.registerBlockEntityRenderer(
                            LambdaContent.LAUNCHPAD_LAMBDA_BE.get(),
                            context -> new RenderLaunchpadLambda());
                    event.registerEntityRenderer(
                            LambdaContent.ROCKET_LAMBDA.get(), RenderRocketLambda::new);
                });
    }
}
