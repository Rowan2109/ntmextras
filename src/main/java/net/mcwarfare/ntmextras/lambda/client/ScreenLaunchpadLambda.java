// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda.client;

import com.hbm.client.gui.ScreenInfoContainer;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import net.mcwarfare.ntmextras.NtmExtras;
import net.mcwarfare.ntmextras.lambda.BlockEntityLaunchpadLambda;
import net.mcwarfare.ntmextras.lambda.MenuLaunchpadLambda;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenLaunchpadLambda extends ScreenInfoContainer<MenuLaunchpadLambda> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "textures/gui/machine/gui_launchpad_lambda.png");

    public ScreenLaunchpadLambda(MenuLaunchpadLambda menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 226);
        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private BlockEntityLaunchpadLambda pad() {
        return menu.blockEntity();
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityLaunchpadLambda pad = pad();
        blit(graphics, 0, 0, 0, 0, imageWidth, imageHeight);

        int power = (int) (pad.power * 52 / BlockEntityLaunchpadLambda.MAX_POWER);
        blit(graphics, 89, 78 - power, 194, 52 - power, 16, power);

        blit(graphics, pad.autolaunch ? 52 : 34, 43, pad.autolaunch ? 228 : 210, 26, 18, 18);

        pip(graphics, 112, 13, pad.hasJetFuel());
        pip(graphics, 130, 13, pad.hasOxidizer());
        pip(graphics, 94, 13, pad.power >= BlockEntityLaunchpadLambda.CONSUMPTION);

        if (pad.countdown > 0) blit(graphics, 43, 70, 210, 44, 18, 18);

        blit(graphics, 34, 16, pad.hasRocketLoaded() ? 228 : 210, 8, 18, 18);
        ItemStack satellite = pad.getItem(BlockEntityLaunchpadLambda.SLOT_SATELLITE);
        boolean satReady = !satellite.isEmpty() && !BlockEntityLaunchpadLambda.needsOrbiter(satellite);
        blit(graphics, 52, 16, satReady ? 228 : 210, 8, 18, 18);

        drawFluidBar(graphics, 107, 26, 16, 52, pad.fuelTank);
        drawFluidBar(graphics, 125, 26, 16, 52, pad.oxidizerTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 107, 26, 16, 52, pad.fuelTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 125, 26, 16, 52, pad.oxidizerTank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                89,
                26,
                16,
                52,
                pad.power,
                BlockEntityLaunchpadLambda.MAX_POWER);

        drawCustomInfoStat(
                graphics, mouseX, mouseY, 34, 43, 18, 18, lineArray("desc.gui.lambda.manual"));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 52, 43, 18, 18, lineArray("desc.gui.lambda.auto"));

        super.extractLabels(graphics, mouseX, mouseY);
        status(graphics, pad);
    }

    private static void blit(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, 256, 256);
    }

    private static void pip(GuiGraphicsExtractor graphics, int x, int y, boolean ready) {
        blit(graphics, x, y, ready ? 210 : 216, 0, 6, 8);
    }

    private void status(GuiGraphicsExtractor graphics, BlockEntityLaunchpadLambda pad) {
        if (pad.erected && pad.countdown > 0) {
            String seconds = String.valueOf(pad.countdown / SharedConstants.TICKS_PER_SECOND);
            String cents = String.valueOf(pad.countdown % SharedConstants.TICKS_PER_SECOND * 5);
            if (seconds.length() == 1) seconds = "0" + seconds;
            if (cents.length() == 1) cents += "0";
            graphics.text(font, Component.literal(seconds + ":" + cents), 40, 103, 0xFFFF0000, false);
            return;
        }
        String key;
        int color;
        if (!pad.hasRocketLoaded()) {
            key = "desc.gui.soyuz.idle";
            color = 0xFF0000;
        } else if (!pad.erected) {
            key = "desc.gui.soyuz.loading";
            color = 0xFF8000;
        } else {
            key = "desc.gui.soyuz.ready";
            color = 0x00FF00;
        }
        Component label = Component.translatable(key);
        float scale = Math.min(1F, 22F / font.width(label));
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(
                font,
                label,
                (int) (53 / scale - font.width(label) / 2F),
                (int) (107 / scale - font.lineHeight / 2F),
                0xFF000000 | color,
                false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (click(event, 34, 43, "auto", false)) return true;
        if (click(event, 52, 43, "auto", true)) return true;
        if (pad().erected && click(event, 43, 70, "launch", true)) return true;
        return handled;
    }

    private boolean click(MouseButtonEvent event, int x, int y, String key, boolean value) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, value);
        PacketWire.sendToServer(new NbtControlPayload(pad().getBlockPos(), data));
        return true;
    }
}
