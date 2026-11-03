// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only
// Based on RenderPipeAnchor from NTM NEXT, 2026 movblock <admin@movblock.mov>

package net.mcwarfare.ntmextras.networks.client;

import com.hbm.client.render.RenderPipeAnchor;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.mcwarfare.ntmextras.NtmExtras;
import net.mcwarfare.ntmextras.networks.BlockEntityNetworkAnchor;
import net.mcwarfare.ntmextras.networks.NetworkBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderNetworkAnchor
        implements BlockEntityRenderer<BlockEntityNetworkAnchor, RenderNetworkAnchor.State> {

    private static final int PIPE = ResourceManager.pipe_anchor.partId("Pipe");
    private static final int RING = ResourceManager.pipe_anchor.partId("Ring");
    private static final RenderType EXHAUST = type("pipe_anchor_exhaust");
    private static final RenderType PNEUMATIC = type("pipe_anchor_pneumatic");

    private final HFRWavefrontObject model = ResourceManager.pipe_anchor;

    private static RenderType type(String name) {
        return RenderTypes.entityCutoutCull(
                Identifier.fromNamespaceAndPath(
                        NtmExtras.ID, "textures/block/models/network/" + name + ".png"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityNetworkAnchor be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.runs.clear();
        state.renderType =
                be.getBlockState().is(NetworkBlocks.PIPE_ANCHOR_EXHAUST.get()) ? EXHAUST : PNEUMATIC;

        Vec3 self = Vec3.atCenterOf(be.getBlockPos());
        for (long key : be.connections) {
            BlockPos otherPos = BlockPos.of(key);
            Vec3 target = Vec3.atCenterOf(otherPos);
            if (!RenderPipeAnchor.isDominant(self, target)) continue;

            double dX = target.x - self.x;
            double dY = target.y - self.y;
            double dZ = target.z - self.z;
            double hyp = Mth.length(dX, dZ);
            Run run = new Run();
            run.yaw = (float) Math.toDegrees(Math.atan2(dX, dZ));
            run.pitch = (float) Math.toDegrees(Math.atan2(dY, hyp));
            run.length = (float) Mth.length(dX, dY, dZ);
            state.runs.add(run);
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (s.runs.isEmpty()) return;
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        for (Run run : s.runs) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(run.yaw));
            ps.mulPose(Axis.XP.rotationDegrees(90F - run.pitch));

            ps.pushPose();
            ps.scale(1F, run.length, 1F);
            ps.translate(0.0, -0.5, 0.0);
            col.submitCustomGeometry(
                    ps,
                    s.renderType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, PIPE));
            ps.popPose();

            ps.pushPose();
            ps.translate(0.0, run.length / 2D - 1.5, 0.0);
            col.submitCustomGeometry(
                    ps,
                    s.renderType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, RING));
            ps.popPose();

            ps.popPose();
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityNetworkAnchor be) {
        BlockPos pos = be.getBlockPos();
        AABB box = new AABB(pos);
        Vec3 self = Vec3.atCenterOf(pos);
        for (long key : be.connections) {
            BlockPos peer = BlockPos.of(key);
            if (RenderPipeAnchor.isDominant(self, Vec3.atCenterOf(peer)))
                box = box.minmax(new AABB(peer));
        }
        return box.inflate(1.0D);
    }

    public static final class Run {
        public float yaw;
        public float pitch;
        public float length;
    }

    public static final class State extends BlockEntityRenderState {
        public final List<Run> runs = new ArrayList<>();
        public RenderType renderType = EXHAUST;
    }
}
