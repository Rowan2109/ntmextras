// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda.client;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mcwarfare.ntmextras.lambda.BlockEntityLaunchpadLambda;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchpadLambda
        implements BlockEntityRenderer<BlockEntityLaunchpadLambda, RenderLaunchpadLambda.State> {

    private static final HFRWavefrontObject MODEL = LambdaModels.LAUNCHPAD;
    private static final int SILO = MODEL.partId("Silo");
    private static final int DOOR_LEFT = MODEL.partId("DoorLeft");
    private static final int DOOR_RIGHT = MODEL.partId("DoorRight");
    private static final int ERECTOR = MODEL.partId("Erector");
    private static final int ROTOR = MODEL.partId("Rotor");
    private static final int PIVOT_UPPER_1 = MODEL.partId("PivotUpper1");
    private static final int CLAMP_UPPER_1 = MODEL.partId("ClampUpper1");
    private static final int PIVOT_LOWER_1 = MODEL.partId("PivotLower1");
    private static final int CLAMP_LOWER_1 = MODEL.partId("ClampLower1");
    private static final int PIVOT_UPPER_2 = MODEL.partId("PivotUpper2");
    private static final int CLAMP_UPPER_2 = MODEL.partId("ClampUpper2");
    private static final int PIVOT_LOWER_2 = MODEL.partId("PivotLower2");
    private static final int CLAMP_LOWER_2 = MODEL.partId("ClampLower2");

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityLaunchpadLambda pad) {
        var pos = pad.getBlockPos();
        return new AABB(
                pos.getX() - 7,
                pos.getY() - 2,
                pos.getZ() - 7,
                pos.getX() + 8,
                pos.getY() + 60,
                pos.getZ() + 8);
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchpadLambda pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.rotation =
                switch (pad.getBlockState().getValue(BlockMultiblockCore.FACING)) {
                    case NORTH -> 90F;
                    case WEST -> 180F;
                    case SOUTH -> 270F;
                    default -> 0F;
                };
        state.doors = pad.getInterpPos(BlockEntityLaunchpadLambda.INDEX_DOORS, partialTicks);
        state.erector =
                pad.getInterpPos(BlockEntityLaunchpadLambda.INDEX_ERECTOR, partialTicks) - 25F;
        state.rotor = pad.getInterpPos(BlockEntityLaunchpadLambda.INDEX_ROTOR, partialTicks);
        state.clamps = pad.getInterpPos(BlockEntityLaunchpadLambda.INDEX_CLAMPS, partialTicks);
        state.pistons = pad.getInterpPos(BlockEntityLaunchpadLambda.INDEX_PISTONS, partialTicks);
        state.renderRocket = pad.erecting || pad.erected;
        state.rocketFixed = pad.erected;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));

        if (state.rocketFixed) {
            pose.pushPose();
            pose.translate(0D, 2D, 0D);
            pose.mulPose(Axis.YN.rotationDegrees(state.rotation));
            collector.submitCustomGeometry(
                    pose,
                    RenderTypes.entityCutoutCull(LambdaModels.ROCKET_TEX),
                    (p, buffer) -> LambdaModels.ROCKET.render(p, buffer, light, -1));
            pose.popPose();
        }

        part(pose, collector, light, SILO);

        // the doors slide into the silo walls, keep them from poking out past the rim
        float doorClip = state.doors - 6F;
        pose.pushPose();
        pose.translate(0D, 0D, state.doors);
        clipped(pose, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, DOOR_LEFT, 0F, 0F, -1F, doorClip);
        pose.popPose();
        pose.pushPose();
        pose.translate(0D, 0D, -state.doors);
        clipped(pose, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, DOOR_RIGHT, 0F, 0F, 1F, doorClip);
        pose.popPose();

        // nothing of the erector may show below the pad surface
        Matrix4f padInverse = new Matrix4f(pose.last().pose()).invert();

        pose.pushPose();
        pose.translate(0D, state.erector, 0D);
        floorClipped(pose, padInverse, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, ERECTOR);

        pose.translate(0D, 13.25D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees(state.rotor));
        pose.translate(0D, -13.25D, 0D);
        floorClipped(pose, padInverse, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, ROTOR);

        clamp(pose, padInverse, collector, light, 3.5D, 19.75D, -state.clamps, state.pistons, PIVOT_UPPER_1, CLAMP_UPPER_1);
        clamp(pose, padInverse, collector, light, 3.5D, 6.75D, state.clamps, state.pistons, PIVOT_LOWER_1, CLAMP_LOWER_1);
        clamp(pose, padInverse, collector, light, -3.5D, 19.75D, state.clamps, -state.pistons, PIVOT_UPPER_2, CLAMP_UPPER_2);
        clamp(pose, padInverse, collector, light, -3.5D, 6.75D, -state.clamps, -state.pistons, PIVOT_LOWER_2, CLAMP_LOWER_2);

        if (state.renderRocket && !state.rocketFixed) {
            pose.translate(0D, 2D, 0D);
            pose.mulPose(Axis.YN.rotationDegrees(state.rotation));
            floorClipped(pose, padInverse, collector, light, LambdaModels.ROCKET, LambdaModels.ROCKET_TEX, 0);
        }
        pose.popPose();
        pose.popPose();
    }

    private static void clamp(
            PoseStack pose,
            Matrix4f padInverse,
            SubmitNodeCollector collector,
            int light,
            double x,
            double y,
            float angle,
            float piston,
            int pivot,
            int clamp) {
        pose.pushPose();
        pose.translate(x, y, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-x, -y, 0D);
        floorClipped(pose, padInverse, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, pivot);
        pose.translate(piston, 0F, 0F);
        floorClipped(pose, padInverse, collector, light, MODEL, LambdaModels.LAUNCHPAD_TEX, clamp);
        pose.popPose();
    }

    private static void part(PoseStack pose, SubmitNodeCollector collector, int light, int part) {
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(LambdaModels.LAUNCHPAD_TEX),
                (p, buffer) -> MODEL.renderPart(p, buffer, light, -1, part));
    }

    // takes the plane y = 0.25 of the pad into the part's own space
    private static void floorClipped(
            PoseStack pose,
            Matrix4f padInverse,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject model,
            Identifier texture,
            int part) {
        Matrix4f local = new Matrix4f(padInverse).mul(pose.last().pose());
        Vector4f plane = new Vector4f(0F, 1F, 0F, -0.25F).mulTranspose(local);
        clipped(pose, collector, light, model, texture, part, plane.x, plane.y, plane.z, -plane.w);
    }

    private static void clipped(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject model,
            Identifier texture,
            int part,
            float nx,
            float ny,
            float nz,
            float d) {
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(texture),
                (p, buffer) -> model.renderPartClipped(p, buffer, light, -1, part, nx, ny, nz, d));
    }

    public static final class State extends BlockEntityRenderState {
        float rotation;
        float doors;
        float erector;
        float rotor;
        float clamps;
        float pistons;
        boolean renderRocket;
        boolean rocketFixed;
    }
}
