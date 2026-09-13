package com.miragelock.mixin;

import com.miragelock.client.MirageLockClient;
import com.miragelock.config.ModConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts item drops performed outside of any inventory screen (e.g. while walking around in the world).
 * Triggered by keybind Q / Ctrl+Q in open gameplay.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.getInstance();

        if (!config.enabled || client.player == null) {
            return;
        }

        // Bypass check: Alt key
        if (config.allowBypass) {
            boolean isAltDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
            if (isAltDown) {
                MirageLockClient.showClientMessage(client,
                        Component.translatable("miragelock.message.bypass").withStyle(ChatFormatting.YELLOW));
                return;
            }
        }

        // If blockCtrlQOnly is enabled, single Q drops (entireStack == false) are allowed
        if (config.blockCtrlQOnly && !entireStack) {
            return;
        }

        int selectedSlot = ((InventoryAccessor) client.player.getInventory()).getSelected();
        ItemStack stack = client.player.getMainHandItem();

        boolean slotLocked = config.isSlotLocked(selectedSlot);
        boolean itemProtected = !stack.isEmpty() && config.isItemProtected(stack);

        if (slotLocked || itemProtected) {
            MirageLockClient.playFeedbackSound(client, false);

            String slotName = Component.translatable("miragelock.slot.hotbar", selectedSlot + 1).getString();
            Component reason = slotLocked
                    ? Component.translatable("miragelock.message.blocked_slot", slotName)
                    : Component.translatable("miragelock.message.blocked_item");

            MirageLockClient.showClientMessage(client, reason.copy().withStyle(ChatFormatting.RED));

            cir.setReturnValue(false);
        }
    }
}
