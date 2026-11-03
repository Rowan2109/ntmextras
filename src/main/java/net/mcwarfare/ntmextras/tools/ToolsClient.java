// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools;

import com.hbm.client.ClientRegistry;
import com.hbm.client.render.RenderBulletMK4;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.items.weapon.sedna.factory.LegoClient;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

@Mod(value = NtmExtras.ID, dist = Dist.CLIENT)
public final class ToolsClient {
    private static final Identifier GUN_CLIENT_SETUP_ID =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "tools_gun_client_setup");
    private static final Identifier FLARE_TEX = Library.id("textures/particle/flare.png");

    private static int grenadePart = -1;

    private static final RenderBulletMK4.Tracer RENDER_FLIEGERFAUST =
            (state, pose, collector) -> {
                pose.pushPose();
                pose.scale(0.25F * 4F, 0.125F, 0.125F);
                pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.translate(0F, 0.25F, 0F);
                collector.submitCustomGeometry(
                        pose,
                        RenderTypes.entityCutout(ResourceManager.grenade_tex),
                        (p, buf) ->
                                ResourceManager.projectiles.renderPart(
                                        p, buf, state.light, -1, grenadePart));
                pose.popPose();

                // back out of the bullet's own rotation so the sprite can face the camera
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(-(state.pitch + 180F)));
                pose.mulPose(Axis.YP.rotationDegrees(-(state.yaw - 90F)));
                if (state.cameraOrientation != null) pose.mulPose(state.cameraOrientation);
                collector.submitCustomGeometry(
                        pose,
                        WeaponRenderTypes.flash(FLARE_TEX),
                        (p, buf) -> {
                            flareQuad(p, buf, 0.375F, 1F, 0.875F, 0.25F, 0.125F);
                            flareQuad(p, buf, 0.1875F, 1F, 1F, 1F, 0.25F);
                        });
                pose.popPose();
            };

    public ToolsClient(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> bindClientHooks());
        modBus.addListener(
                (AddClientReloadListenersEvent event) -> {
                    event.addListener(
                            GUN_CLIENT_SETUP_ID,
                            (ResourceManagerReloadListener)
                                    resourceManager -> setupGunClient());
                    event.addDependency(ClientRegistry.GUN_CLIENT_SETUP_ID, GUN_CLIENT_SETUP_ID);
                });
    }

    private static void bindClientHooks() {
        ItemRTTYDesignator.OPEN_SCREEN =
                player ->
                        Minecraft.getInstance()
                                .gui
                                .setScreen(new ScreenRTTYDesignator(player.getMainHandItem()));
    }

    private static void setupGunClient() {
        ItemRenderWeaponBase.register(
                ToolsContent.GUN_FLIEGERFAUST::get, new ItemRenderFliegerfaust());
        ToolsContent.GUN_FLIEGERFAUST
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        grenadePart = ResourceManager.projectiles.partId("Grenade");
        RenderBulletMK4.setRenderer(XFactoryFliegerfaust.rocket_ff, RENDER_FLIEGERFAUST);
    }

    private static void flareQuad(
            PoseStack.Pose p, VertexConsumer buf, float s, float r, float g, float b, float a) {
        int light = LightCoordsUtil.FULL_BRIGHT;
        int color = ARGB.colorFromFloat(a, r, g, b);
        Vertices.emit(buf, p, -s, -s, 0, color, 1, 1, light, 0, 0, 1);
        Vertices.emit(buf, p, -s, s, 0, color, 1, 0, light, 0, 0, 1);
        Vertices.emit(buf, p, s, s, 0, color, 0, 0, light, 0, 0, 1);
        Vertices.emit(buf, p, s, -s, 0, color, 0, 1, light, 0, 0, 1);
    }
}
