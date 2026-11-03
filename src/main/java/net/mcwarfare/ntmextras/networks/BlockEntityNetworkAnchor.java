// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks;

import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.uninos.graph.GraphNode;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Only tells clients which anchors this one is linked to, the graph itself holds the links. */
public class BlockEntityNetworkAnchor extends BlockEntityMachineBase implements SyncUnitSchema {

    private static final int UNIT_LINKS = 0;

    public long[] connections = new long[0];

    public BlockEntityNetworkAnchor(BlockPos pos, BlockState state) {
        super(NetworkBlocks.ANCHOR_BE.get(), pos, state, 0);
    }

    @Override
    public void tickServer() {
        if (!(getBlockState().getBlock() instanceof NetworkAnchor anchor)) return;
        GraphNode<?> node = anchor.graph((ServerLevel) level).getNode(worldPosition.asLong());
        long[] links = new long[0];
        if (node != null && node.hasRemoteLinks()) {
            links = node.remoteLinks.toLongArray();
            Arrays.sort(links);
        }
        if (!Arrays.equals(links, connections)) {
            connections = links;
            syncUnitsChanged(3, 1L << UNIT_LINKS);
        }
        networkPackNTTracking();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << UNIT_LINKS;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != UNIT_LINKS) {
            super.writeSyncUnit(unit, output);
            return;
        }
        output.writeInt(connections.length);
        for (long connection : connections) output.writeLong(connection);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != UNIT_LINKS) {
            super.readSyncUnit(unit, input);
            return;
        }
        long[] values = new long[input.readInt()];
        for (int i = 0; i < values.length; i++) values[i] = input.readLong();
        connections = values;
    }
}
