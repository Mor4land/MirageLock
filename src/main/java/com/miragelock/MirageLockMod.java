package com.miragelock;

import com.miragelock.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirageLockMod implements ModInitializer {
    public static final String MOD_ID = "miragelock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing MirageLock...");
        ModConfig.load();
    }
}
