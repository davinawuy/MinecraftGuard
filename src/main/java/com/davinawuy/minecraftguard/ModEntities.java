package com.davinawuy.minecraftguard;

import com.davinawuy.minecraftguard.entity.GuardEntity;
import com.davinawuy.minecraftguard.entity.GuardRole;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<GuardEntity> GUARD = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MinecraftGuard.MOD_ID, "guard"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GuardEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(32)
                    .build()
    );

    public static void init() {
        FabricDefaultAttributeRegistry.register(GUARD, GuardEntity.createGuardAttributes());
        MinecraftGuard.LOGGER.info("Registered guard entity with default role {}", GuardRole.SWORD);
    }
}
