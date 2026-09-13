package com.miragelock.mixin;

import com.miragelock.client.MirageLockClient;
import com.miragelock.config.ModConfig;
import com.miragelock.util.SlotUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int containerId, int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.enabled || player == null) {
            return;
        }

        if (clickType == ClickType.THROW) {
            Minecraft client = Minecraft.getInstance();

            // Bypass check: Alt key held
            if (config.allowBypass) {
                boolean isAltDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                        || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
                if (isAltDown) {
                    return; // Allow the drop to proceed to the server!
                }
            }

            boolean isCtrlQ = (button == 1);
            if (config.blockCtrlQOnly && !isCtrlQ) {
                return;
            }

            if (slotId >= 0 && slotId < player.containerMenu.slots.size()) {
                Slot slot = player.containerMenu.slots.get(slotId);
                ItemStack stack = slot.getItem();

                int globalSlotId = SlotUtils.getGlobalSlotId(slot);
                boolean slotLocked = globalSlotId >= 0 && config.isSlotLocked(globalSlotId);
                boolean itemProtected = !stack.isEmpty() && config.isItemProtected(stack);

                if (slotLocked || itemProtected) {
                    MirageLockClient.playFeedbackSound(client, false);

                    String slotName = SlotUtils.isHotbarSlot(globalSlotId)
                            ? Component.translatable("miragelock.slot.hotbar", globalSlotId + 1).getString()
                            : Component.translatable("miragelock.slot.inventory", globalSlotId + 1).getString();

                    Component reason = slotLocked
                            ? Component.translatable("miragelock.message.blocked_slot", slotName)
                            : Component.translatable("miragelock.message.blocked_item");

                    MirageLockClient.showClientMessage(client, reason.copy().withStyle(ChatFormatting.RED));
                    ci.cancel();
                }
            }
        }
    }
}
