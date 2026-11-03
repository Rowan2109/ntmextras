// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.networks;

import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemWrench;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A pipe anchor for a network other than fluids. ItemWrench only knows the fluid anchor, so the block answers the wrench. */
public interface NetworkAnchor {

    VoxelShape[] SHAPES = shapes();

    LevelNodeGraph<?> graph(ServerLevel level);

    void link(ServerLevel level, BlockPos first, BlockPos second);

    private static VoxelShape[] shapes() {
        VoxelShape[] shapes = new VoxelShape[6];
        double min = 4 / 16D, max = 12 / 16D;
        for (Direction facing : Direction.VALUES) {
            Direction dir = facing.getOpposite();
            shapes[facing.ordinal()] =
                    Shapes.box(
                            dir == Direction.WEST ? 0 : min,
                            dir == Direction.DOWN ? 0 : min,
                            dir == Direction.NORTH ? 0 : min,
                            dir == Direction.EAST ? 1 : max,
                            dir == Direction.UP ? 1 : max,
                            dir == Direction.SOUTH ? 1 : max);
        }
        return shapes;
    }

    static @Nullable InteractionResult wrench(
            ItemStack stack, Block self, Level level, BlockPos pos, Player player) {
        if (!(stack.getItem() instanceof ItemWrench)) return null;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Long stored = stack.get(ModDataComponents.WRENCH_TARGET.get());
        if (stored == null) {
            stack.set(ModDataComponents.WRENCH_TARGET.get(), pos.asLong());
            if (!level.isClientSide())
                player.sendSystemMessage(Component.translatable("desc.item.wrench.pipeStart"));
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos first = BlockPos.of(stored);
        stack.remove(ModDataComponents.WRENCH_TARGET.get());
        player.sendSystemMessage(Component.literal(connect((ServerLevel) level, self, first, pos)));
        return InteractionResult.SUCCESS;
    }

    private static String connect(ServerLevel level, Block self, BlockPos first, BlockPos second) {
        if (!level.isLoaded(first)) return "Pipe error";
        Block other = level.getBlockState(first).getBlock();
        if (other != self) {
            return other instanceof NetworkAnchor || other instanceof FluidPipeAnchorBlock
                    ? "Pipe error - Pipes are not the same type"
                    : "Pipe error";
        }
        if (first.equals(second)) return "Pipe error - Cannot connect to the same pipe anchor";
        if (Vec3.atCenterOf(first).distanceTo(Vec3.atCenterOf(second))
                > FluidPipeAnchorBlock.MAX_PIPE_LENGTH) {
            return "Pipe error - Pipe anchor is too far away";
        }
        ((NetworkAnchor) self).link(level, first, second);
        return "Pipe end";
    }
}
