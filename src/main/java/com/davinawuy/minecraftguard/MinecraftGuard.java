package com.davinawuy.minecraftguard;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftGuard implements ModInitializer {
    public static final String MOD_ID = "minecraftguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("MinecraftGuard mod has been initialized!");
    }
}
