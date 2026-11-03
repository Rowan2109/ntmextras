// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda;

import com.hbm.inventory.container.BlockEntityMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MenuLaunchpadLambda extends BlockEntityMenu<BlockEntityLaunchpadLambda> {

    public MenuLaunchpadLambda(int containerId, Inventory playerInv, BlockEntityLaunchpadLambda pad) {
        super(LambdaContent.LAUNCHPAD_LAMBDA_MENU.get(), containerId, pad);
        checkContainerSize(pad, BlockEntityLaunchpadLambda.SLOT_COUNT);

        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_ROCKET, 35, 17));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_SATELLITE, 53, 17));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_FUEL_IN, 107, 80));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_FUEL_OUT, 107, 98));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_OXIDIZER_IN, 125, 80));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_OXIDIZER_OUT, 125, 98));
        addSlot(new ValidatedSlot(pad, BlockEntityLaunchpadLambda.SLOT_BATTERY, 89, 80));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 144 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 202));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, BlockEntityLaunchpadLambda.SLOT_COUNT);
    }

    private static final class ValidatedSlot extends Slot {

        private final BlockEntityLaunchpadLambda pad;

        private ValidatedSlot(BlockEntityLaunchpadLambda pad, int index, int x, int y) {
            super(pad, index, x, y);
            this.pad = pad;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return pad.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
