// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.tool.ItemRTTYPager;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ItemRTTYDesignator extends Item implements IItemControlReceiver {

    public static final String KEY_CHANNEL = "chan";
    public static final double RANGE = 300D;

    public static Consumer<Player> OPEN_SCREEN = player -> {};

    public ItemRTTYDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // the channel packet only ever writes to the main hand
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) OPEN_SCREEN.accept(player);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        String channel = ItemRTTYPager.channel(player.getItemInHand(hand));
        if (channel.isEmpty()) return InteractionResult.SUCCESS;

        // a ray that hits nothing sends nothing instead of a position in the sky
        HitResult hit = player.pick(RANGE, 1F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = block.getBlockPos().relative(block.getDirection());
        String coords =
                pos.getX()
                        + IRORInteractive.PARAM_SEPARATOR
                        + pos.getY()
                        + IRORInteractive.PARAM_SEPARATOR
                        + pos.getZ();
        RTTYSystem.broadcast(level, channel, coords);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void receiveControl(ServerPlayer player, ItemStack stack, CompoundTag data) {
        String channel = data.getStringOr(KEY_CHANNEL, "");
        if (channel.length() > ItemRTTYPager.MAX_CHANNEL_LENGTH) {
            channel = channel.substring(0, ItemRTTYPager.MAX_CHANNEL_LENGTH);
        }
        ItemRTTYPager.setChannel(stack, channel);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String channel = ItemRTTYPager.channel(stack);
        if (channel.isEmpty()) {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.noChannelSet")
                            .withStyle(ChatFormatting.RED));
        } else {
            adder.accept(
                    Component.translatable("desc.item.rttyPager.channel", channel)
                            .withStyle(ChatFormatting.YELLOW));
        }
    }
}
