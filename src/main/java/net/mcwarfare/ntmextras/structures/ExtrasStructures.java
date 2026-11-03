// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.structures;

import com.hbm.blockentity.BlockEntityLogicBlock;
import com.hbm.blockentity.bomb.BlockEntityCharge;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.world.gen.util.LogicBlockActions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** What the structures need beyond data: the logic action NTM NEXT left out, used by the military base. */
public final class ExtrasStructures {
    private static final int TRAP_FUSE = 2400;

    static {
        LogicBlockActions.ACTIONS.put("BOMB_TRAP", ExtrasStructures::bombTrap);
        LogicDirectionProcessor.INSTANCE.getClass();
    }

    private ExtrasStructures() {}

    private static void bombTrap(BlockEntityLogicBlock tile) {
        if (tile.phase != 1 || !(tile.getLevel() instanceof ServerLevel level)) return;

        Direction facing = tile.direction == null ? Direction.NORTH : tile.direction;
        BlockPos pos = tile.getBlockPos();
        BlockPos chargePos = pos.relative(facing.getOpposite());
        level.setBlockAndUpdate(
                chargePos, ModBlocks.CHARGE_C4.get().defaultBlockState().setValue(BlockChargeBase.FACING, facing));
        if (level.getBlockEntity(chargePos) instanceof BlockEntityCharge charge) {
            charge.timer = TRAP_FUSE;
            charge.arm(level);
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
