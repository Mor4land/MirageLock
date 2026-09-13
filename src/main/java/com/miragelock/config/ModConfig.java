package com.miragelock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MirageLockConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "miragelock.json");

    private static ModConfig INSTANCE = new ModConfig();

    // Dirty flag for debounced saving
    private transient boolean dirty = false;
    private transient long lastSaveTime = 0;
    private static final long SAVE_COOLDOWN_MS = 500;

    public boolean enabled = true;
    public boolean blockCtrlQOnly = true;
    public boolean showLockIcons = true;
    public boolean soundEnabled = true;
    public boolean notificationsEnabled = true;
    public boolean allowBypass = true;
    public Set<Integer> lockedSlotIndexes = new HashSet<>();
    public List<ProtectedItem> protectedItems = new ArrayList<>();

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            INSTANCE = new ModConfig();
            INSTANCE.saveImmediate();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            INSTANCE = GSON.fromJson(reader, ModConfig.class);
            if (INSTANCE == null) {
                INSTANCE = new ModConfig();
            }
            if (INSTANCE.lockedSlotIndexes == null) {
                INSTANCE.lockedSlotIndexes = new HashSet<>();
            }
            if (INSTANCE.protectedItems == null) {
                INSTANCE.protectedItems = new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load MirageLock configuration", e);
            INSTANCE = new ModConfig();
        }
    }

    /**
     * Marks config as dirty and saves if enough time has passed since the last save.
     * Prevents excessive disk writes when rapidly clicking GUI buttons.
     */
    public void save() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime >= SAVE_COOLDOWN_MS) {
            saveImmediate();
        } else {
            dirty = true;
        }
    }

    /**
     * Forces an immediate save to disk. Use for critical saves (e.g., closing GUI).
     */
    public void saveImmediate() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
            dirty = false;
            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            LOGGER.error("Failed to save MirageLock configuration", e);
        }
    }

    /**
     * Flush pending saves if the config was marked dirty.
     * Should be called periodically (e.g., on client tick or screen close).
     */
    public void flushIfDirty() {
        if (dirty) {
            saveImmediate();
        }
    }

    public void checkDebounceFlush() {
        if (dirty && System.currentTimeMillis() - lastSaveTime >= SAVE_COOLDOWN_MS) {
            saveImmediate();
        }
    }

    public boolean isItemProtected(ItemStack stack) {
        if (!enabled || stack == null || stack.isEmpty()) {
            return false;
        }
        for (ProtectedItem item : protectedItems) {
            if (item.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSlotLocked(int slotIndex) {
        return enabled && lockedSlotIndexes.contains(slotIndex);
    }

    public boolean toggleSlotLock(int slotIndex) {
        boolean added;
        if (lockedSlotIndexes.contains(slotIndex)) {
            lockedSlotIndexes.remove(slotIndex);
            added = false;
        } else {
            lockedSlotIndexes.add(slotIndex);
            added = true;
        }
        save();
        return added;
    }

    /**
     * Toggles protection for an item. Uses identity-based matching for deduplication:
     * if an entry with the same itemId + nbtFingerprint + customName already exists,
     * it gets removed. Otherwise a new entry is added.
     */
    public boolean toggleItemProtection(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ProtectedItem candidate = ProtectedItem.fromItemStack(stack);
        if (candidate == null) {
            return false;
        }

        // Search by identity (exact itemId + nbt + customName), not by gameplay matching
        ProtectedItem existingMatch = null;
        for (ProtectedItem item : protectedItems) {
            if (item.hasSameIdentity(candidate)) {
                existingMatch = item;
                break;
            }
        }

        if (existingMatch != null) {
            protectedItems.remove(existingMatch);
            save();
            return false; // Removed
        } else {
            protectedItems.add(candidate);
            save();
            return true; // Added
        }
    }
}
