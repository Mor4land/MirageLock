package com.miragelock.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public class ProtectedItem {
    private String itemId;
    private String displayName;
    private String nbtFingerprint;
    private String customName;
    private boolean matchNbt;

    public ProtectedItem() {
    }

    public ProtectedItem(String itemId, String displayName, String nbtFingerprint, String customName, boolean matchNbt) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.nbtFingerprint = nbtFingerprint;
        this.customName = customName;
        this.matchNbt = matchNbt;
    }

    public static String computeComponentFingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            sb.append("custom_data=").append(customData.copyTag()).append(";");
        }

        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            sb.append("custom_name=").append(customName.getString()).append(";");
        }

        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (itemName != null) {
            sb.append("item_name=").append(itemName.getString()).append(";");
        }

        Object enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null) {
            sb.append("enchants=").append(enchants).append(";");
        }

        Object storedEnchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (storedEnchants != null) {
            sb.append("stored_enchants=").append(storedEnchants).append(";");
        }

        Object lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            sb.append("lore=").append(lore).append(";");
        }

        Object trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            sb.append("trim=").append(trim).append(";");
        }

        Object cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            sb.append("cmd=").append(cmd).append(";");
        }

        Object potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            sb.append("potion=").append(potion).append(";");
        }

        return sb.toString();
    }

    public static ProtectedItem fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name = stack.getHoverName().getString();

        Component customNameComponent = stack.get(DataComponents.CUSTOM_NAME);
        String customName = customNameComponent != null ? customNameComponent.getString() : "";

        String fingerprint = computeComponentFingerprint(stack);
        boolean hasUniqueData = !fingerprint.isEmpty();

        return new ProtectedItem(id, name, fingerprint, customName, hasUniqueData);
    }

    /**
     * Checks if a given ItemStack matches this protected item rule.
     * Used during gameplay to decide if an item should be blocked from dropping.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String currentId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!currentId.equals(this.itemId)) {
            return false;
        }

        // If matchNbt is disabled, any item with the same ID matches
        if (!this.matchNbt) {
            return true;
        }

        // Check custom name match
        if (this.customName != null && !this.customName.isEmpty()) {
            Component nameComp = stack.get(DataComponents.CUSTOM_NAME);
            String currentCustomName = nameComp != null ? nameComp.getString() : "";
            if (!this.customName.equals(currentCustomName)) {
                return false;
            }
        }

        // Check component / NBT fingerprint match (strict equality)
        if (this.nbtFingerprint != null && !this.nbtFingerprint.isEmpty()) {
            String currentFingerprint = computeComponentFingerprint(stack);
            if (currentFingerprint.equals(this.nbtFingerprint)) {
                return true;
            }
            // Backward compatibility with legacy saved raw NBT tags
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().toString().equals(this.nbtFingerprint)) {
                return true;
            }
            return false;
        }

        return true;
    }

    /**
     * Checks if another ProtectedItem represents the same config entry.
     * Used for deduplication — prevents adding the same item+NBT combo twice.
     */
    public boolean hasSameIdentity(ProtectedItem other) {
        if (other == null) return false;
        if (!Objects.equals(this.itemId, other.itemId)) return false;

        String thisNbt = this.nbtFingerprint != null ? this.nbtFingerprint : "";
        String otherNbt = other.nbtFingerprint != null ? other.nbtFingerprint : "";

        String thisName = this.customName != null ? this.customName : "";
        String otherName = other.customName != null ? other.customName : "";

        return thisNbt.equals(otherNbt) && thisName.equals(otherName);
    }

    /**
     * Creates an ItemStack for GUI rendering (icon display).
     * Uses O(1) registry lookup instead of linear iteration.
     */
    public ItemStack createItemStack() {
        try {
            Identifier loc = Identifier.tryParse(this.itemId);
            if (loc != null) {
                return BuiltInRegistries.ITEM.getOptional(loc)
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNbtFingerprint() {
        return nbtFingerprint;
    }

    public String getCustomName() {
        return customName;
    }

    public boolean isMatchNbt() {
        return matchNbt;
    }

    public void setMatchNbt(boolean matchNbt) {
        this.matchNbt = matchNbt;
    }
}
