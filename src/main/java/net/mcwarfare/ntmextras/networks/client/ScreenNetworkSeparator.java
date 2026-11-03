// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only
// Based on ScreenCableDiode from NTM NEXT, 2026 movblock <admin@movblock.mov>

package net.mcwarfare.ntmextras.networks.client;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import net.mcwarfare.ntmextras.networks.BlockEntityNetworkSeparator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class ScreenNetworkSeparator extends Screen {

    private final BlockEntityNetworkSeparator separator;
    private EditBox throughput;
    private int priority;

    public ScreenNetworkSeparator(BlockEntityNetworkSeparator separator) {
        super(Component.empty());
        this.separator = separator;
        this.priority = separator.priority.ordinal();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        throughput =
                new EditBox(
                        font,
                        cx - 150,
                        100,
                        90,
                        20,
                        Component.translatable("gui.diode.throughput"));
        throughput.setMaxLength(11);
        throughput.setValue(Long.toString(separator.configuredLimit));
        addRenderableWidget(throughput);
        addRenderableWidget(
                Button.builder(
                                Component.literal(ConnectionPriority.VALUES[priority].name()),
                                button -> {
                                    priority = (priority + 1) % ConnectionPriority.VALUES.length;
                                    button.setMessage(
                                            Component.literal(
                                                    ConnectionPriority.VALUES[priority].name()));
                                })
                        .bounds(cx + 20, 100, 90, 20)
                        .build());
        setInitialFocus(throughput);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        graphics.text(
                font,
                Component.translatable("gui.diode.throughput"),
                cx - 150,
                80,
                0xFFE0E0E0,
                false);
        graphics.text(
                font,
                Component.translatable("gui.diode.throughputMax"),
                cx - 150,
                90,
                0xFFE0E0E0,
                false);
        graphics.text(
                font, Component.translatable("gui.diode.priority"), cx + 20, 80, 0xFFE0E0E0, false);
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try {
            data.putLong("limit", Long.parseLong(throughput.getValue()));
        } catch (NumberFormatException ignored) {
        }
        PacketWire.sendToServer(new NbtControlPayload(separator.getBlockPos(), data));
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
