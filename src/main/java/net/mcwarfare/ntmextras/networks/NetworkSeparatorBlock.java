// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Three blocks wide and two high, IN on the local west end and OUT on the local east end of the bottom row. */
public class NetworkSeparatorBlock extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, ILookOverlay {

    public static Consumer<BlockEntityNetworkSeparator> OPEN_SCREEN = separator -> {};

    private static final int[] DIMENSIONS = {1, 0, 0, 0, 1, 1};

    public NetworkSeparatorBlock(Properties props) {
        super(props);
    }

    @Override
    protected MultiblockLayout runtimeLayout(int declaredCaps) {
        MultiblockMaskTable.Builder table = MultiblockMaskTable.builder();
        long[][] offsets = new long[4][];
        for (Direction facing : Direction.Plane.HORIZONTAL) offsets[facing.get2DDataValue()] = new long[5];
        int cell = 0;
        for (int ly = 0; ly <= 1; ly++) {
            for (int lx = -1; lx <= 1; lx++) {
                int mask = MASK_NONE, roles = ROLE_ALL;
                if (ly == 0 && lx == -1) {
                    mask = MASK_WEST;
                    roles = ROLE_POWER_IN;
                } else if (ly == 0 && lx == 1) {
                    mask = MASK_EAST;
                    roles = ROLE_POWER_OUT;
                }
                table.cell(lx, ly, 0, mask, roles);
                table.flag(lx, ly, 0, MultiblockMaskTable.FLAG_PROXY);
                if (lx == 0 && ly == 0) continue;
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    offsets[facing.get2DDataValue()][cell] =
                            BlockPos.asLong(
                                    MultiblockMaskTable.worldX(lx, ly, 0, facing),
                                    ly,
                                    MultiblockMaskTable.worldZ(lx, ly, 0, facing));
                }
                cell++;
            }
        }
        MultiblockMaskTable built = table.build();
        return new MultiblockLayout(
                declaredCaps,
                new MultiblockMaskTable[] {built, built, built, built},
                offsets,
                new long[][] {
                    MultiblockLayout.NO_CELLS,
                    MultiblockLayout.NO_CELLS,
                    MultiblockLayout.NO_CELLS,
                    MultiblockLayout.NO_CELLS
                },
                new MultiblockLayout.Placement(
                        DIMENSIONS,
                        0,
                        0,
                        new int[] {
                            DIMENSIONS[0],
                            DIMENSIONS[1],
                            DIMENSIONS[2],
                            DIMENSIONS[3],
                            DIMENSIONS[4],
                            DIMENSIONS[5],
                            0,
                            0,
                            0
                        },
                        new double[0]),
                MultiblockLayout.CellGeometry.NONE);
    }

    @Override
    public int cellSound() {
        return BlockMultiblockCell.SOUND_METAL;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNetworkSeparator(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(NetworkBlocks.SEPARATOR_BE).powerIn().powerOut();
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level.isClientSide() && level.getBlockEntity(core) instanceof BlockEntityNetworkSeparator separator) {
            OPEN_SCREEN.accept(separator);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityNetworkSeparator separator)) return;
        info.title(getName().getString(), 0xffff00, 0x404000)
                .line(
                        ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + BobMathUtil.getShortNumber(separator.lastTick)
                                + "HE/t"
                                + ChatFormatting.RED
                                + " ->")
                .line(
                        ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + BobMathUtil.getShortNumber(separator.deltaSecond)
                                + "HE/s"
                                + ChatFormatting.RED
                                + " ->")
                .line("Configured: " + BobMathUtil.getShortNumber(separator.configuredLimit) + "HE/t")
                .line("Priority: " + ChatFormatting.YELLOW + separator.priority.name());
        if (separator.redstone) info.line(ChatFormatting.RED + "Disabled by redstone");
    }
}
