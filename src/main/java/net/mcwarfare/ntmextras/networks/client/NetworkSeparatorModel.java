// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks.client;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.BlockModel;
import com.hbm.client.model.CarrierRoot;
import com.hbm.client.model.Meshes;
import com.hbm.client.model.ObjUnbakedGeometry;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import java.util.List;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public record NetworkSeparatorModel() implements BlockModel<HFRWavefrontObject> {

    private static final Identifier OBJ =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "models/network/network_separator.obj");
    private static final String[] BODY = {"Plane"};

    @Override
    public HFRWavefrontObject prepare() {
        return Meshes.load(OBJ);
    }

    @Override
    public BlockStateModel.UnbakedRoot root(
            HFRWavefrontObject prepared, Block block, BlockState state) {
        return new Root(BlockModel.base(block), prepared, state.getValue(BlockMultiblockCore.FACING));
    }

    public record Root(Identifier carrier, HFRWavefrontObject obj, Direction facing)
            implements CarrierRoot {

        @Override
        public QuadCollection quads(
                BlockState blockState,
                ModelBaker baker,
                ResolvedModel carrier,
                TextureSlots slots) {
            Material.Baked material =
                    BlockModel.slot(baker, carrier, slots, ObjUnbakedGeometry.SINGLE_SLOT);
            Matrix4f pose =
                    new Matrix4f()
                            .translate(0.5F, 0F, 0.5F)
                            .rotateY((float) Math.toRadians(Facing.yaw(facing, 90)));
            return ObjUnbakedGeometry.bakeGroups(
                    List.of(ObjUnbakedGeometry.Group.rawSmooth(obj, BODY, material, pose)),
                    0F,
                    ObjUnbakedGeometry.RAW_OFFSET_Y,
                    0F);
        }

        @Override
        public Object visualEqualityGroup(BlockState state) {
            return facing;
        }
    }
}
