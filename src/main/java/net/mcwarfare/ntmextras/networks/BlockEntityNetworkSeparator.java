// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.EnergyCaps;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.GatherFace;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityNetworkSeparator extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IControlReceiver,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    public static final long MAX_LIMIT = 10_000_000_000L;

    private static final int UNIT_PRIORITY = 0;
    private static final int UNIT_REDSTONE = 1;
    private static final int UNIT_LIMIT = 2;
    private static final int UNIT_TICK = 3;
    private static final int UNIT_SECOND = 4;

    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone;
    public long configuredLimit = 1_000L;
    public long actualLimit = configuredLimit;
    public long lastTick;
    public long deltaSecond;

    private long power;
    private long deltaTracker;
    private final long[] deltaTick = new long[20];
    private final ReferenceOpenHashSet<IEnergyHandlerMK2> surveyed = new ReferenceOpenHashSet<>();
    private @Nullable BlockLookupCache<IEnergyHandlerMK2> outputReceiver;

    public BlockEntityNetworkSeparator(BlockPos pos, BlockState state) {
        super(NetworkBlocks.SEPARATOR_BE.get(), pos, state, 0);
    }

    @Override
    public void setBlockState(BlockState state) {
        if (getBlockState().getValue(BlockMultiblockCore.FACING)
                != state.getValue(BlockMultiblockCore.FACING)) {
            outputReceiver = null;
        }
        super.setBlockState(state);
    }

    private Direction output() {
        return getBlockState().getValue(BlockMultiblockCore.FACING).getCounterClockWise();
    }

    @Override
    public void tickServer() {
        ServerLevel server = (ServerLevel) level;
        Direction out = output();
        BlockPos end = worldPosition.relative(out);
        BlockPos outside = end.relative(out);

        boolean powered =
                server.hasNeighborSignal(worldPosition)
                        || server.hasNeighborSignal(end)
                        || server.hasNeighborSignal(worldPosition.relative(out.getOpposite()));
        if (powered != redstone) {
            redstone = powered;
            syncUnitsChanged(3, 1L << UNIT_REDSTONE);
        }

        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        actualLimit = configuredLimit;
        if (graph.containsCell(outside.asLong())) {
            NodeNetwork<CableData> net = graph.networkAt(outside.asLong());
            if (net != null) survey(server, graph, net);
        } else if (!redstone && power > 0) {
            if (outputReceiver == null) {
                outputReceiver =
                        new NeoForgeCaps.CapCache<>(
                                EnergyCaps.RECEIVER, server, outside, out.getOpposite());
            }
            IEnergyHandlerMK2 neighbour = outputReceiver.find();
            if (neighbour != null && neighbour != this && neighbour.allowDirectProvision()) {
                PowerNetwork.provideDirect(this, List.of(neighbour));
            }
        }
        actualLimit = Math.max(actualLimit, power);

        long total = deltaTick[0];
        for (int i = 1; i < deltaTick.length; i++) {
            total += deltaTick[i];
            deltaTick[i - 1] = deltaTick[i];
        }
        deltaTick[deltaTick.length - 1] = deltaTracker;
        deltaTracker = 0;
        if (lastTick != deltaTick[deltaTick.length - 1]) {
            lastTick = deltaTick[deltaTick.length - 1];
            syncUnitsChanged(3, 1L << UNIT_TICK);
        }
        if (deltaSecond != total) {
            deltaSecond = total;
            syncUnitsChanged(3, 1L << UNIT_SECOND);
        }

        networkPackNT(15);
    }

    /** Never pulls more into the buffer than the outgoing network can take. */
    @SuppressWarnings("unchecked")
    private void survey(
            ServerLevel server, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        long demand = 0;
        surveyed.clear();
        for (GraphNode<CableData> node : graph.activeFaces(net)) {
            EdgeFaces faces = node.ef;
            if (faces == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                if ((faces.activeMask & 1 << d) == 0) continue;
                GatherFace<IEnergyHandlerMK2, ?, ?> face =
                        (GatherFace<IEnergyHandlerMK2, ?, ?>) faces.attachments[d];
                if (face == null) continue;
                IEnergyHandlerMK2 receiver = face.receiver.find();
                if (receiver == null) {
                    // a foreign energy store, which can't be asked here
                    if (face.provider.find() == null) demand = MAX_LIMIT;
                    continue;
                }
                if (receiver == this || !surveyed.add(receiver)) continue;
                demand +=
                        receiver instanceof BlockEntityNetworkSeparator separator
                                ? separator.actualLimit
                                : Math.min(
                                        receiver.getMaxPower() - receiver.getPower(),
                                        receiver.getReceiverSpeed());
            }
        }
        surveyed.clear();
        if (demand < actualLimit) actualLimit = demand;
    }

    public boolean bufferMismatch() {
        return configuredLimit < getMaxPower();
    }

    @Override
    public void usePower(long amount) {
        setPower(power - amount);
        deltaTracker += amount;
        if (amount != 0) energyChanged();
    }

    @Override
    public long getReceiverSpeed() {
        return redstone || bufferMismatch() ? 0 : getMaxPower();
    }

    @Override
    public long getProviderSpeed() {
        return redstone ? 0 : getMaxPower();
    }

    @Override
    public long getMaxPower() {
        return actualLimit;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    public void setPriority(ConnectionPriority next) {
        if (priority == next) return;
        priority = next;
        syncUnitsChanged(3, 1L << UNIT_PRIORITY);
        markChanged();
    }

    public void setConfiguredLimit(long next) {
        next = Math.clamp(next, 0L, MAX_LIMIT);
        if (configuredLimit == next) return;
        configuredLimit = next;
        syncUnitsChanged(3, 1L << UNIT_LIMIT);
        markChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D)
                <= 128D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("limit"))
            setConfiguredLimit(data.getLongOr("limit", configuredLimit));
        if (data.contains("priority")) {
            setPriority(
                    ConnectionPriority.VALUES[
                            Math.clamp(
                                    data.getByteOr("priority", (byte) priority.ordinal()),
                                    0,
                                    ConnectionPriority.VALUES.length - 1)]);
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "deltatick",
            PREFIX_VALUE + "deltasecond",
            PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
            PREFIX_FUNCTION + "setlimit" + NAME_SEPARATOR + "limit",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick[deltaTick.length - 1];
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaSecond;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            setPriority(ConnectionPriority.VALUES[IRORInteractive.parseInt(params[0], 0, 2) + 1]);
        } else if ((PREFIX_FUNCTION + "setlimit").equals(name) && params.length > 0) {
            setConfiguredLimit(parseLimit(params[0]));
        }
        return null;
    }

    private static long parseLimit(String value) {
        long result;
        try {
            result = Long.parseLong(value);
        } catch (NumberFormatException x) {
            try {
                result = Math.round(Double.parseDouble(value));
            } catch (NumberFormatException y) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }
        if (result < 0 || result > MAX_LIMIT) throw new RORFunctionException(EX_FORMAT);
        return result;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = Math.max(0L, input.getLongOr("power", 0L));
        configuredLimit = Math.clamp(input.getLongOr("configuredLimit", 1_000L), 0L, MAX_LIMIT);
        actualLimit = Math.max(0L, input.getLongOr("actualLimit", configuredLimit));
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(
                                input.getByteOr("p", (byte) priority.ordinal()),
                                0,
                                ConnectionPriority.VALUES.length - 1)];
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("configuredLimit", configuredLimit);
        output.putLong("actualLimit", actualLimit);
        output.putByte("p", (byte) priority.ordinal());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1FL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case UNIT_PRIORITY -> output.writeByte(priority.ordinal());
            case UNIT_REDSTONE -> output.writeBoolean(redstone);
            case UNIT_LIMIT -> output.writeLong(configuredLimit);
            case UNIT_TICK -> output.writeLong(lastTick);
            case UNIT_SECOND -> output.writeLong(deltaSecond);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case UNIT_PRIORITY ->
                    priority =
                            ConnectionPriority.VALUES[
                                    Math.clamp(
                                            input.readByte(),
                                            0,
                                            ConnectionPriority.VALUES.length - 1)];
            case UNIT_REDSTONE -> redstone = input.readBoolean();
            case UNIT_LIMIT -> configuredLimit = Math.clamp(input.readLong(), 0L, MAX_LIMIT);
            case UNIT_TICK -> lastTick = input.readLong();
            case UNIT_SECOND -> deltaSecond = input.readLong();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
