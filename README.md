# MirageLock

**MirageLock** is a lightweight, client-side Fabric mod for Minecraft that prevents accidental item drops (`Q` / `Ctrl+Q`) with inventory slot locking, modern 1.21+ Data Component matching, Mod Menu configuration, and multilingual support.

## Features

- **Item Drop Protection:** Block drops of specific protected items or slots.
- **Data Component & NBT Precision:** Match exact items by components (enchantments, custom names, lore, trims, custom data) without being invalidated by durability changes.
- **Inventory Slot Locking:** Lock specific player inventory and hotbar slots (`H1`–`H9`).
- **In-Game Shortcuts:**
  - `L` — Toggle drop protection for item in hand or slot under cursor.
  - `K` — Toggle lock for hovered slot or selected hotbar slot.
  - `Alt + Drop` — Emergency drop bypass (instantly drop item when holding `Alt`).
- **In-Game Configuration GUI:** Mod Menu integration with item search, NBT matching toggle, and quick settings.
- **Multilingual Support (I18n):** Native English and Russian translations.
- **Zero-lag Optimization:** Direct O(1) registry lookups and debounced config saving.

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `>=0.19.3`
- Fabric API
- Mod Menu *(Optional, for config screen)*

## Building

```bash
./gradlew build
```

The compiled mod JAR will be located in `build/libs/`.

## License

This project is licensed under [CC0-1.0](LICENSE).
