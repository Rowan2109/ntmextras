// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.structures;

import com.hbm.blocks.ModBlocks;
import com.hbm.registration.NeoForgeRegistrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/** Turns a logic block's stored direction with the structure, so BOMB_TRAP finds the same door in every rotation. */
public final class LogicDirectionProcessor implements StructureProcessor {
    public static final LogicDirectionProcessor INSTANCE = new LogicDirectionProcessor();
    public static final MapCodec<LogicDirectionProcessor> MAP_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final RegistryHandle<MapCodec<LogicDirectionProcessor>> TYPE =
            NeoForgeRegistrar.registerStructureProcessorType("ntmextras:logic_direction", MAP_CODEC);

    private LogicDirectionProcessor() {}

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            BlockPos templateRelativePos,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings) {
        CompoundTag nbt = processedBlockInfo.nbt();
        if (nbt == null) return processedBlockInfo;
        if (!processedBlockInfo.state().is(ModBlocks.LOGIC_BLOCK.get())
                && !processedBlockInfo.state().is(ModBlocks.LOGIC_BLOCK_INVIS.get())) return processedBlockInfo;
        int stored = nbt.getIntOr("direction", -1);
        if (stored < 0) return processedBlockInfo;

        CompoundTag rotated = nbt.copy();
        rotated.putInt("direction", settings.getRotation().rotate(Direction.from3DDataValue(stored)).get3DDataValue());
        return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), processedBlockInfo.state(), rotated);
    }

    @Override
    public MapCodec<LogicDirectionProcessor> codec() {
        return MAP_CODEC;
    }
}
