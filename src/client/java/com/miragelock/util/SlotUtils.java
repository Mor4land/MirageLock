package com.miragelock.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class SlotUtils {
    /**
     * Maps a slot to a player inventory slot ID (0..35, armor 36..39, offhand 40).
     * Container slots (chests, furnaces, etc.) return -1 to prevent locking slots in all chests globally.
     */
    public static int getGlobalSlotId(Slot slot) {
        if (slot == null) return -1;
        if (slot.container instanceof Inventory) {
            return slot.getContainerSlot();
        }
        return -1;
    }

    public static boolean isPlayerSlot(Slot slot) {
        return slot != null && slot.container instanceof Inventory;
    }

    public static boolean isHotbarSlot(int containerSlot) {
        return containerSlot >= 0 && containerSlot < 9;
    }
}
