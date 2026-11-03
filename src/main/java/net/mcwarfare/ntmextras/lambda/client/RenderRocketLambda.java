// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda.client;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.lib.Library;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcwarfare.ntmextras.lambda.EntityRocketLambda;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RenderRocketLambda extends EntityRenderer<EntityRocketLambda, RenderRocketLambda.State> {

    private static final Identifier FLARE_TEX = Library.id("textures/particle/flare.png");

    public RenderRocketLambda(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityRocketLambda entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityRocketLambda entity, Frustum culler, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(LambdaModels.ROCKET_TEX),
                (p, buffer) -> LambdaModels.ROCKET.render(p, buffer, light, -1));

        if (state.ageInTicks >= 2F) {
            // move the flare one block towards the player so it doesn't clip into the model
            Vec3 towards = camera.pos.subtract(state.x, state.y, state.z).normalize();
            float scale =
                    (float)
                            (Math.min(5D, (state.ageInTicks - 2D) * 0.5D)
                                    * (0.8D + Math.random() * 0.4D)
                                    * 2D);
            pose.pushPose();
            pose.translate(towards.x, towards.y, towards.z);
            pose.mulPose(camera.orientation);
            collector.submitCustomGeometry(
                    pose,
                    WeaponRenderTypes.flash(FLARE_TEX),
                    (p, buffer) -> {
                        flareQuad(p, buffer, scale, 1F, 0.75F, 0.5F, 0.5F);
                        flareQuad(p, buffer, scale * 0.5F, 1F, 1F, 1F, 0.75F);
                    });
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }

    private static void flareQuad(
            PoseStack.Pose p, VertexConsumer buffer, float s, float r, float g, float b, float a) {
        int light = LightCoordsUtil.FULL_BRIGHT;
        int color = ARGB.colorFromFloat(a, r, g, b);
        Vertices.emit(buffer, p, -s, -s, 0, color, 1, 1, light, 0, 0, 1);
        Vertices.emit(buffer, p, -s, s, 0, color, 1, 0, light, 0, 0, 1);
        Vertices.emit(buffer, p, s, s, 0, color, 0, 0, light, 0, 0, 1);
        Vertices.emit(buffer, p, s, -s, 0, color, 0, 1, light, 0, 0, 1);
    }

    public static final class State extends EntityRenderState {}
}
