package com.davinawuy.minecraftguard.client;

import com.davinawuy.minecraftguard.ModEntities;
import com.davinawuy.minecraftguard.client.render.GuardEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class MinecraftGuardClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GUARD, GuardEntityRenderer::new);
    }
}
