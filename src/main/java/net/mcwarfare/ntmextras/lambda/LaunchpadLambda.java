// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LaunchpadLambda extends BlockMultiblockCore implements ITickingBlock, ICapabilityBlock {

    private static final int OFFSET = 7;
    private static final int RADIUS = 7;
    private static final int[] DIMENSIONS = {1, 0, 7, 7, 7, 7};
    private static final int[] PREVIEW = {
        1, 0, 7, 7, 7, 7, 0, 0, 0,
        2, -2, 7, -6, 7, 7, 0, 0, 0,
        2, -2, -6, 7, 7, 7, 0, 0, 0,
        2, -2, 7, 7, 7, -6, 0, 0, 0,
        2, -2, 7, 7, -6, 7, 0, 0, 0
    };

    public LaunchpadLambda(Properties properties) {
        super(properties);
    }

    private static boolean isPort(int lx, int ly, int lz) {
        return ly == 1 && lz == RADIUS && lx <= -2 && lx >= -6;
    }

    @Override
    protected MultiblockLayout runtimeLayout(int declaredCaps) {
        MultiblockMaskTable.Builder table = MultiblockMaskTable.builder();
        LongArrayList offsets = new LongArrayList();
        table.cell(0, 0, 0, MASK_NONE, ROLE_ALL);
        table.passive(0, 0, 0, passiveCoreMask(), passiveCoreDomains(declaredCaps));

        for (int ly = 0; ly <= 2; ly++) {
            for (int lz = -RADIUS; lz <= RADIUS; lz++) {
                for (int lx = -RADIUS; lx <= RADIUS; lx++) {
                    if (lx == 0 && ly == 0 && lz == 0) continue;
                    // the top layer is only the two block rim around the silo doors
                    if (ly == 2 && Math.abs(lx) < RADIUS - 1 && Math.abs(lz) < RADIUS - 1) continue;
                    offsets.add(BlockPos.asLong(lx, ly, lz));
                    if (isPort(lx, ly, lz)) {
                        table.cell(lx, ly, lz, MASK_SOUTH, ROLE_POWER_IN | ROLE_FLUID_IN);
                        table.passive(lx, ly, lz, MASK_SOUTH, PASSIVE_ITEMS);
                        table.flag(lx, ly, lz, MultiblockMaskTable.FLAG_PROXY);
                    } else {
                        table.cell(lx, ly, lz, MASK_NONE, ROLE_ALL);
                    }
                }
            }
        }

        return MultiblockLayout.uniform(
                declaredCaps,
                table.build(),
                offsets.toLongArray(),
                new MultiblockLayout.Placement(DIMENSIONS, OFFSET, 0, PREVIEW, new double[0]));
    }

    @Override
    public int cellSound() {
        return BlockMultiblockCell.SOUND_METAL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchpadLambda(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(LambdaContent.LAUNCHPAD_LAMBDA_BE)
                .powerIn()
                .fluidIn()
                .items()
                .itemsAtCells()
                .fe();
    }
}
