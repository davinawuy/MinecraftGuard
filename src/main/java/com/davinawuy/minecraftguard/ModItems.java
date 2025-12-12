package com.davinawuy.minecraftguard;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item GUARD_SPAWN_EGG = new SpawnEggItem(
            ModEntities.GUARD,
            0x74655b,
            0x2b2b2b,
            new FabricItemSettings()
    );

    public static void init() {
        Registry.register(Registries.ITEM, new Identifier(MinecraftGuard.MOD_ID, "guard_spawn_egg"), GUARD_SPAWN_EGG);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(GUARD_SPAWN_EGG));
    }
}
