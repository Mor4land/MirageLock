package com.miragelock.client.gui;

import com.miragelock.config.ModConfig;
import com.miragelock.config.ProtectedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MirageLockConfigScreen extends Screen {
    private final Screen parent;
    private int activeTab = 0;
    private int scrollOffset = 0;
    private String currentSearch = "";

    private static final int ITEM_ROW_HEIGHT = 24;

    private final List<ProtectedItem> filteredItems = new ArrayList<>();
    private final List<ItemStack> filteredStacks = new ArrayList<>();

    private EditBox searchBox;

    public MirageLockConfigScreen(Screen parent) {
        super(Component.translatable("miragelock.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int midX = this.width / 2;

        // Tab buttons
        Button tabGeneralBtn = Button.builder(Component.translatable("miragelock.tab.general"), btn -> {
            activeTab = 0;
            scrollOffset = 0;
            rebuildWidgets();
        }).bounds(midX - 155, 20, 150, 20).build();
        tabGeneralBtn.active = (activeTab != 0);
        this.addRenderableWidget(tabGeneralBtn);

        int itemCount = ModConfig.getInstance().protectedItems.size();
        Button tabItemsBtn = Button.builder(Component.translatable("miragelock.tab.items", itemCount), btn -> {
            activeTab = 1;
            scrollOffset = 0;
            rebuildWidgets();
        }).bounds(midX + 5, 20, 150, 20).build();
        tabItemsBtn.active = (activeTab != 1);
        this.addRenderableWidget(tabItemsBtn);

        if (activeTab == 0) {
            initGeneralTab(midX);
        } else {
            initItemsTab(midX);
        }

        // Done button
        this.addRenderableWidget(Button.builder(Component.translatable("miragelock.button.done"), btn -> {
            ModConfig.getInstance().flushIfDirty();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(midX + (activeTab == 1 ? 60 : -50), this.height - 25, 100, 20).build());
    }

    private void initGeneralTab(int midX) {
        int y = 46;

        // Row 1: Mod Enabled & Drop Mode
        this.addRenderableWidget(Button.builder(getEnabledText(), btn -> {
            ModConfig.getInstance().enabled = !ModConfig.getInstance().enabled;
            ModConfig.getInstance().save();
            btn.setMessage(getEnabledText());
        }).bounds(midX - 155, y, 150, 20).build());

        this.addRenderableWidget(Button.builder(getModeText(), btn -> {
            ModConfig.getInstance().blockCtrlQOnly = !ModConfig.getInstance().blockCtrlQOnly;
            ModConfig.getInstance().save();
            btn.setMessage(getModeText());
        }).bounds(midX + 5, y, 150, 20).build());

        y += 24;

        // Row 2: Lock Badges & Reset Slots
        this.addRenderableWidget(Button.builder(getIconsText(), btn -> {
            ModConfig.getInstance().showLockIcons = !ModConfig.getInstance().showLockIcons;
            ModConfig.getInstance().save();
            btn.setMessage(getIconsText());
        }).bounds(midX - 155, y, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("miragelock.button.reset_slots"), btn -> {
            ModConfig.getInstance().lockedSlotIndexes.clear();
            ModConfig.getInstance().save();
            this.rebuildWidgets();
        }).bounds(midX + 5, y, 150, 20).build());

        y += 24;

        // Row 3: Sound & Actionbar Alerts
        this.addRenderableWidget(Button.builder(getSoundText(), btn -> {
            ModConfig.getInstance().soundEnabled = !ModConfig.getInstance().soundEnabled;
            ModConfig.getInstance().save();
            btn.setMessage(getSoundText());
        }).bounds(midX - 155, y, 150, 20).build());

        this.addRenderableWidget(Button.builder(getAlertsText(), btn -> {
            ModConfig.getInstance().notificationsEnabled = !ModConfig.getInstance().notificationsEnabled;
            ModConfig.getInstance().save();
            btn.setMessage(getAlertsText());
        }).bounds(midX + 5, y, 150, 20).build());

        y += 24;

        // Row 4: Alt Bypass
        this.addRenderableWidget(Button.builder(getBypassText(), btn -> {
            ModConfig.getInstance().allowBypass = !ModConfig.getInstance().allowBypass;
            ModConfig.getInstance().save();
            btn.setMessage(getBypassText());
        }).bounds(midX - 155, y, 150, 20).build());

        y += 28;

        // Hotbar Slot buttons (H1..H9)
        int slotBtnWidth = 32;
        int slotStartX = midX - ((9 * (slotBtnWidth + 2)) / 2);
        for (int i = 0; i < 9; i++) {
            final int slotId = i;
            boolean isLocked = ModConfig.getInstance().lockedSlotIndexes.contains(slotId);
            Component label = isLocked
                    ? Component.literal("H" + (i + 1) + " [L]").withStyle(ChatFormatting.RED)
                    : Component.literal("H" + (i + 1));

            this.addRenderableWidget(Button.builder(label, btn -> {
                ModConfig.getInstance().toggleSlotLock(slotId);
                this.rebuildWidgets();
            }).bounds(slotStartX + (i * (slotBtnWidth + 2)), y, slotBtnWidth, 20).build());
        }
    }

    private void initItemsTab(int midX) {
        int y = 46;

        this.searchBox = new EditBox(this.font, midX - 155, y, 310, 18, Component.translatable("miragelock.search.hint"));
        this.searchBox.setHint(Component.translatable("miragelock.search.hint").withStyle(ChatFormatting.GRAY));
        this.searchBox.setValue(currentSearch);
        this.searchBox.setResponder(text -> {
            currentSearch = text;
            scrollOffset = 0;
            rebuildWidgets();
        });
        this.addRenderableWidget(this.searchBox);

        y += 22;

        rebuildFilteredItems();

        int listStartY = y;
        int maxRows = getMaxRows(listStartY);
        if (scrollOffset > Math.max(0, filteredItems.size() - maxRows)) {
            scrollOffset = Math.max(0, filteredItems.size() - maxRows);
        }

        // Create buttons for each visible row
        for (int i = 0; i < maxRows && (i + scrollOffset) < filteredItems.size(); i++) {
            final int itemIndex = i + scrollOffset;
            final ProtectedItem item = filteredItems.get(itemIndex);
            int rowY = listStartY + (i * ITEM_ROW_HEIGHT);

            // NBT toggle button
            Component nbtText = item.isMatchNbt()
                    ? Component.translatable("miragelock.nbt.on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("miragelock.nbt.off").withStyle(ChatFormatting.GRAY);

            this.addRenderableWidget(Button.builder(nbtText, btn -> {
                item.setMatchNbt(!item.isMatchNbt());
                ModConfig.getInstance().save();
                rebuildWidgets();
            }).bounds(midX + 55, rowY, 75, 20).build());

            // Delete button
            this.addRenderableWidget(Button.builder(
                    Component.literal("✕").withStyle(ChatFormatting.RED),
                    btn -> {
                        ModConfig.getInstance().protectedItems.remove(item);
                        ModConfig.getInstance().save();
                        rebuildWidgets();
                    }
            ).bounds(midX + 133, rowY, 20, 20).build());
        }

        // Bottom buttons
        int bottomY = this.height - 25;

        Button addHeldBtn = Button.builder(
                Component.translatable("miragelock.button.add_held").withStyle(ChatFormatting.GREEN),
                btn -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        ItemStack mainHand = this.minecraft.player.getMainHandItem();
                        if (!mainHand.isEmpty()) {
                            ModConfig.getInstance().toggleItemProtection(mainHand);
                            rebuildWidgets();
                        }
                    }
                }
        ).bounds(midX - 155, bottomY, 115, 20).build();
        if (this.minecraft == null || this.minecraft.player == null) {
            addHeldBtn.active = false;
        }
        this.addRenderableWidget(addHeldBtn);

        this.addRenderableWidget(Button.builder(
                Component.translatable("miragelock.button.clear_all").withStyle(ChatFormatting.RED),
                btn -> {
                    ModConfig.getInstance().protectedItems.clear();
                    ModConfig.getInstance().save();
                    scrollOffset = 0;
                    rebuildWidgets();
                }
        ).bounds(midX - 35, bottomY, 90, 20).build());
    }

    private void rebuildFilteredItems() {
        filteredItems.clear();
        filteredStacks.clear();
        String query = currentSearch.toLowerCase().trim();

        for (ProtectedItem item : ModConfig.getInstance().protectedItems) {
            ItemStack stack = item.createItemStack();
            String name = item.getDisplayName();
            if (name == null || name.isBlank()) {
                name = !stack.isEmpty() ? stack.getHoverName().getString() : item.getItemId();
            }

            if (query.isEmpty()
                    || name.toLowerCase().contains(query)
                    || item.getItemId().toLowerCase().contains(query)) {
                filteredItems.add(item);
                filteredStacks.add(stack);
            }
        }
    }

    private int getMaxRows(int listStartY) {
        return Math.max(1, (this.height - listStartY - 36) / ITEM_ROW_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == 1) {
            int listStartY = 68;
            int maxRows = getMaxRows(listStartY);
            int maxScroll = Math.max(0, filteredItems.size() - maxRows);

            if (verticalAmount > 0 && scrollOffset > 0) {
                scrollOffset--;
                rebuildWidgets();
                return true;
            } else if (verticalAmount < 0 && scrollOffset < maxScroll) {
                scrollOffset++;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        // Draw screen background dimming
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        int midX = this.width / 2;

        // Title
        guiGraphics.drawCenteredString(this.font, Component.translatable("miragelock.title"), midX, 6, 0xFFFFD700);

        if (activeTab == 0) {
            renderGeneralTab(guiGraphics, midX);
        } else {
            renderItemsTab(guiGraphics, midX, mouseX, mouseY);
        }
    }

    private void renderGeneralTab(GuiGraphics guiGraphics, int midX) {
        int textY = 175;
        guiGraphics.drawString(this.font, Component.translatable("miragelock.hotkeys.title"), midX - 155, textY, 0xFFFFCC00, true);
        guiGraphics.drawString(this.font, Component.translatable("miragelock.hotkeys.item"), midX - 155, textY + 14, 0xFFAAAAAA, true);
        guiGraphics.drawString(this.font, Component.translatable("miragelock.hotkeys.slot"), midX - 155, textY + 26, 0xFFAAAAAA, true);
        guiGraphics.drawString(this.font, Component.translatable("miragelock.hotkeys.bypass"), midX - 155, textY + 38, 0xFFAAAAAA, true);
    }

    private void renderItemsTab(GuiGraphics guiGraphics, int midX, int mouseX, int mouseY) {
        int listStartY = 68;
        int maxRows = getMaxRows(listStartY);
        int rowLeft = midX - 155;

        if (filteredItems.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("miragelock.list.empty"),
                    rowLeft, listStartY + 10, 0xFFAAAAAA, true);
            return;
        }

        // Scroll indicator
        if (filteredItems.size() > maxRows) {
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + maxRows, filteredItems.size())
                    + " / " + filteredItems.size();
            guiGraphics.drawString(this.font, scrollInfo, midX + 90, 48, 0xFFAAAAAA, true);
        }

        // Render each visible row
        for (int i = 0; i < maxRows && (i + scrollOffset) < filteredItems.size(); i++) {
            int itemIndex = i + scrollOffset;
            ProtectedItem item = filteredItems.get(itemIndex);
            ItemStack stack = filteredStacks.get(itemIndex);
            int rowY = listStartY + (i * ITEM_ROW_HEIGHT);

            // Row highlight on hover
            int rowRight = midX + 155;
            if (mouseX >= rowLeft && mouseX <= rowRight && mouseY >= rowY && mouseY < rowY + ITEM_ROW_HEIGHT) {
                guiGraphics.fill(rowLeft, rowY, rowRight, rowY + ITEM_ROW_HEIGHT, 0x20FFFFFF);
            }

            // Item icon
            if (!stack.isEmpty()) {
                guiGraphics.renderFakeItem(stack, rowLeft + 2, rowY + (ITEM_ROW_HEIGHT - 16) / 2);
            }

            // Item display name
            int textX = rowLeft + 22;
            String displayName = item.getDisplayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = !stack.isEmpty() ? stack.getHoverName().getString() : item.getItemId();
            }

            // Fit text within available width before NBT button (midX + 50 - textX)
            int maxTextWidth = (midX + 50) - textX;
            if (this.font.width(displayName) > maxTextWidth) {
                while (displayName.length() > 3 && this.font.width(displayName + "..") > maxTextWidth) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName = displayName + "..";
            }

            // Render display name with 100% alpha (0xFFFFFFFF)
            guiGraphics.drawString(this.font, displayName, textX, rowY + 3, 0xFFFFFFFF, true);

            // Custom name badge
            String customName = item.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                int nameWidth = this.font.width(displayName);
                if (textX + nameWidth + 10 < midX + 55) {
                    guiGraphics.drawString(this.font, "✦", textX + nameWidth + 3, rowY + 3, 0xFF55FFFF, true);
                }
            }

            // Item ID (second line)
            String idStr = item.getItemId();
            if (idStr.startsWith("minecraft:")) {
                idStr = idStr.substring(10);
            }
            if (this.font.width(idStr) > maxTextWidth) {
                while (idStr.length() > 3 && this.font.width(idStr + "..") > maxTextWidth) {
                    idStr = idStr.substring(0, idStr.length() - 1);
                }
                idStr = idStr + "..";
            }
            // Render ID in opaque gray (0xFFAAAAAA)
            guiGraphics.drawString(this.font, idStr, textX, rowY + 14, 0xFFAAAAAA, false);
        }
    }

    private Component getEnabledText() {
        boolean enabled = ModConfig.getInstance().enabled;
        return enabled
                ? Component.translatable("miragelock.status.mod_enabled").withStyle(ChatFormatting.GREEN)
                : Component.translatable("miragelock.status.mod_disabled").withStyle(ChatFormatting.RED);
    }

    private Component getModeText() {
        boolean ctrlQ = ModConfig.getInstance().blockCtrlQOnly;
        return ctrlQ
                ? Component.translatable("miragelock.status.mode_ctrl_q").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("miragelock.status.mode_all_q").withStyle(ChatFormatting.GOLD);
    }

    private Component getIconsText() {
        boolean icons = ModConfig.getInstance().showLockIcons;
        return icons
                ? Component.translatable("miragelock.status.icons_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("miragelock.status.icons_off").withStyle(ChatFormatting.RED);
    }

    private Component getSoundText() {
        boolean sound = ModConfig.getInstance().soundEnabled;
        return sound
                ? Component.translatable("miragelock.status.sound_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("miragelock.status.sound_off").withStyle(ChatFormatting.RED);
    }

    private Component getAlertsText() {
        boolean alerts = ModConfig.getInstance().notificationsEnabled;
        return alerts
                ? Component.translatable("miragelock.status.alerts_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("miragelock.status.alerts_off").withStyle(ChatFormatting.RED);
    }

    private Component getBypassText() {
        boolean bypass = ModConfig.getInstance().allowBypass;
        return bypass
                ? Component.translatable("miragelock.status.bypass_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("miragelock.status.bypass_off").withStyle(ChatFormatting.RED);
    }

    @Override
    public void onClose() {
        ModConfig.getInstance().flushIfDirty();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
