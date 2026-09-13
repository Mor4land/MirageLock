package com.miragelock.mixin;

import com.miragelock.client.MirageLockClient;
import com.miragelock.config.ModConfig;
import com.miragelock.util.SlotUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected Slot hoveredSlot;

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void onRenderSlot(GuiGraphics guiGraphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.enabled || !config.showLockIcons || slot == null) {
            return;
        }

        int slotId = SlotUtils.getGlobalSlotId(slot);
        boolean slotLocked = slotId >= 0 && config.isSlotLocked(slotId);
        boolean itemProtected = slot.hasItem() && config.isItemProtected(slot.getItem());

        if (slotLocked || itemProtected) {
            int x = slot.x;
            int y = slot.y;

            if (slotLocked) {
                guiGraphics.fill(x, y, x + 16, y + 16, 0x44FF3333);
                renderMiniPadlock(guiGraphics, x + 10, y + 1, 0xFFFF3333);
            } else {
                renderMiniPadlock(guiGraphics, x + 10, y + 1, 0xFFFFCC00);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.getInstance();

        if (!config.enabled || client.player == null) {
            return;
        }

        // Hotkey: Lock hovered item
        if (MirageLockClient.lockItemKey.matches(event)) {
            Slot slot = this.hoveredSlot;
            if (slot != null && slot.hasItem()) {
                MirageLockClient.toggleItemProtection(client, slot.getItem());
                cir.setReturnValue(true);
                return;
            }
        }

        // Hotkey: Lock hovered slot
        if (MirageLockClient.lockSlotKey.matches(event)) {
            Slot slot = this.hoveredSlot;
            if (slot != null) {
                MirageLockClient.toggleSlotLock(client, slot);
                cir.setReturnValue(true);
                return;
            }
        }

        // Intercept Drop Key (Q / Ctrl+Q)
        if (client.options.keyDrop.matches(event)) {
            // Bypass check: Alt key
            boolean isAltDown = event.hasAltDown()
                    || (event.modifiers() & GLFW.GLFW_MOD_ALT) != 0
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
            if (config.allowBypass && isAltDown) {
                MirageLockClient.showClientMessage(client,
                        Component.translatable("miragelock.message.bypass").withStyle(ChatFormatting.YELLOW));
                return;
            }

            boolean isCtrlPressed = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
            if (config.blockCtrlQOnly && !isCtrlPressed) {
                return;
            }

            Slot slot = this.hoveredSlot;
            if (slot != null) {
                int slotId = SlotUtils.getGlobalSlotId(slot);
                boolean slotLocked = slotId >= 0 && config.isSlotLocked(slotId);
                boolean itemProtected = slot.hasItem() && config.isItemProtected(slot.getItem());

                if (slotLocked || itemProtected) {
                    MirageLockClient.playFeedbackSound(client, false);

                    String slotName = SlotUtils.isHotbarSlot(slotId)
                            ? Component.translatable("miragelock.slot.hotbar", slotId + 1).getString()
                            : Component.translatable("miragelock.slot.inventory", slotId + 1).getString();

                    Component reason = slotLocked
                            ? Component.translatable("miragelock.message.blocked_slot", slotName)
                            : Component.translatable("miragelock.message.blocked_item");

                    MirageLockClient.showClientMessage(client, reason.copy().withStyle(ChatFormatting.RED));
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    private static void renderMiniPadlock(GuiGraphics guiGraphics, int x, int y, int color) {
        // Padlock shackle (top hoop)
        guiGraphics.fill(x + 1, y, x + 4, y + 1, color);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + 3, color);
        guiGraphics.fill(x + 3, y + 1, x + 4, y + 3, color);

        // Padlock body
        guiGraphics.fill(x, y + 2, x + 5, y + 6, color);

        // Keyhole
        guiGraphics.fill(x + 2, y + 3, x + 3, y + 5, 0xFF000000);
    }
}
