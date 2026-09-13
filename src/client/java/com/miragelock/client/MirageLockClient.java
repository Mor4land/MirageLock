package com.miragelock.client;

import com.miragelock.config.ModConfig;
import com.miragelock.mixin.AbstractContainerScreenAccessor;
import com.miragelock.mixin.InventoryAccessor;
import com.miragelock.util.SlotUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class MirageLockClient implements ClientModInitializer {
    public static KeyMapping lockItemKey;
    public static KeyMapping lockSlotKey;

    @Override
    public void onInitializeClient() {
        lockItemKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.miragelock.lock_item",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                KeyMapping.Category.GAMEPLAY
        ));

        lockSlotKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.miragelock.lock_slot",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (lockItemKey.consumeClick()) {
                handleItemLockPress(client);
            }
            while (lockSlotKey.consumeClick()) {
                handleSlotLockPress(client);
            }
            ModConfig.getInstance().checkDebounceFlush();
        });
    }

    public static void showClientMessage(Minecraft client, Component message) {
        if (client.player != null && ModConfig.getInstance().notificationsEnabled) {
            Component fullMsg = Component.translatable("miragelock.message.prefix").withStyle(ChatFormatting.GOLD)
                    .append(message);
            client.player.displayClientMessage(fullMsg, true);
        }
    }

    public static void playFeedbackSound(Minecraft client, boolean success) {
        if (ModConfig.getInstance().soundEnabled) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(
                    success ? SoundEvents.NOTE_BLOCK_BELL : SoundEvents.NOTE_BLOCK_BASS,
                    success ? 1.2F : 0.8F
            ));
        }
    }

    public static void toggleItemProtection(Minecraft client, ItemStack targetStack) {
        if (client.player == null) return;

        if (targetStack == null || targetStack.isEmpty()) {
            showClientMessage(client, Component.translatable("miragelock.message.hold_or_hover").withStyle(ChatFormatting.RED));
            playFeedbackSound(client, false);
            return;
        }

        boolean added = ModConfig.getInstance().toggleItemProtection(targetStack);
        String name = targetStack.getHoverName().getString();
        playFeedbackSound(client, added);

        if (added) {
            showClientMessage(client, Component.translatable("miragelock.message.item_locked", name).withStyle(ChatFormatting.GREEN));
        } else {
            showClientMessage(client, Component.translatable("miragelock.message.item_unlocked", name).withStyle(ChatFormatting.RED));
        }
    }

    public static void toggleSlotLock(Minecraft client, Slot slot) {
        if (client.player == null || slot == null) return;

        int slotIndex = SlotUtils.getGlobalSlotId(slot);
        if (slotIndex < 0) {
            return;
        }

        String label = SlotUtils.isHotbarSlot(slotIndex)
                ? Component.translatable("miragelock.slot.hotbar", slotIndex + 1).getString()
                : Component.translatable("miragelock.slot.inventory", slotIndex + 1).getString();

        boolean locked = ModConfig.getInstance().toggleSlotLock(slotIndex);
        playFeedbackSound(client, locked);

        if (locked) {
            showClientMessage(client, Component.translatable("miragelock.message.slot_locked", label).withStyle(ChatFormatting.GREEN));
        } else {
            showClientMessage(client, Component.translatable("miragelock.message.slot_unlocked", label).withStyle(ChatFormatting.RED));
        }
    }

    private static void handleItemLockPress(Minecraft client) {
        if (client.player == null) return;

        ItemStack targetStack = null;
        if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            if (slot != null && slot.hasItem()) {
                targetStack = slot.getItem();
            }
        }

        if (targetStack == null || targetStack.isEmpty()) {
            targetStack = client.player.getMainHandItem();
        }

        toggleItemProtection(client, targetStack);
    }

    private static void handleSlotLockPress(Minecraft client) {
        if (client.player == null) return;

        if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            if (slot != null) {
                toggleSlotLock(client, slot);
                return;
            }
        }

        int selected = ((InventoryAccessor) client.player.getInventory()).getSelected();
        String label = Component.translatable("miragelock.slot.hotbar", selected + 1).getString();
        boolean locked = ModConfig.getInstance().toggleSlotLock(selected);
        playFeedbackSound(client, locked);

        if (locked) {
            showClientMessage(client, Component.translatable("miragelock.message.slot_locked", label).withStyle(ChatFormatting.GREEN));
        } else {
            showClientMessage(client, Component.translatable("miragelock.message.slot_unlocked", label).withStyle(ChatFormatting.RED));
        }
    }
}
