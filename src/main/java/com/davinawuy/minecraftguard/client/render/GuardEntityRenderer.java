package com.davinawuy.minecraftguard.client.render;

import com.davinawuy.minecraftguard.MinecraftGuard;
import com.davinawuy.minecraftguard.entity.GuardEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.PlayerSkinTexture;
import java.io.File;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class GuardEntityRenderer extends MobEntityRenderer<GuardEntity, PlayerEntityModel<GuardEntity>> {
    private static final Identifier DEFAULT_TEXTURE = DefaultSkinHelper.getTexture();
    private final Map<String, Identifier> cachedTextures = new HashMap<>();

    public GuardEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(GuardEntity entity) {
        String url = entity.getSkinUrl();
        if (url != null && !url.isBlank()) {
            return cachedTextures.computeIfAbsent(url, this::registerRemoteTexture);
        }
        return DEFAULT_TEXTURE;
    }

    private Identifier registerRemoteTexture(String url) {
        Identifier id = new Identifier(MinecraftGuard.MOD_ID, "guard/" + Integer.toHexString(url.hashCode()));
        PlayerSkinTexture texture = new PlayerSkinTexture((File) null, url, DefaultSkinHelper.getTexture(), true, null);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
        return id;
    }
}
