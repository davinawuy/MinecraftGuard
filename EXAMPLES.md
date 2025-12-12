# MinecraftGuard - Code Examples

This file contains examples to help you get started with adding features to your Minecraft mod.

## Table of Contents

1. [Adding a Simple Item](#adding-a-simple-item)
2. [Adding a Block](#adding-a-block)
3. [Adding a Custom Recipe](#adding-a-custom-recipe)
4. [Registering Events](#registering-events)
5. [Creating a Config File](#creating-a-config-file)

## Adding a Simple Item

### Step 1: Create the Item Class

Create a new file: `src/main/java/com/davinawuy/minecraftguard/item/ModItems.java`

```java
package com.davinawuy.minecraftguard.item;

import com.davinawuy.minecraftguard.MinecraftGuard;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    // Define your custom item
    public static final Item GUARD_SWORD = registerItem("guard_sword",
            new Item(new FabricItemSettings().maxCount(1)));
    
    public static final Item GUARD_GEM = registerItem("guard_gem",
            new Item(new FabricItemSettings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, 
                new Identifier(MinecraftGuard.MOD_ID, name), item);
    }

    public static void registerModItems() {
        MinecraftGuard.LOGGER.info("Registering Mod Items for " + MinecraftGuard.MOD_ID);
    }
}
```

### Step 2: Register Items in Main Class

Update `MinecraftGuard.java`:

```java
package com.davinawuy.minecraftguard;

import com.davinawuy.minecraftguard.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftGuard implements ModInitializer {
    public static final String MOD_ID = "minecraftguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        LOGGER.info("MinecraftGuard mod has been initialized!");
    }
}
```

### Step 3: Add Item Model and Texture

Create: `src/main/resources/assets/minecraftguard/models/item/guard_sword.json`

```json
{
  "parent": "item/handheld",
  "textures": {
    "layer0": "minecraftguard:item/guard_sword"
  }
}
```

Create: `src/main/resources/assets/minecraftguard/models/item/guard_gem.json`

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "minecraftguard:item/guard_gem"
  }
}
```

### Step 4: Add Language File

Create: `src/main/resources/assets/minecraftguard/lang/en_us.json`

```json
{
  "item.minecraftguard.guard_sword": "Guard Sword",
  "item.minecraftguard.guard_gem": "Guard Gem"
}
```

### Step 5: Add to Creative Tab

To make your item appear in the creative inventory:

```java
package com.davinawuy.minecraftguard.item;

import com.davinawuy.minecraftguard.MinecraftGuard;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item GUARD_SWORD = registerItem("guard_sword",
            new Item(new FabricItemSettings().maxCount(1)));
    
    public static final Item GUARD_GEM = registerItem("guard_gem",
            new Item(new FabricItemSettings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, 
                new Identifier(MinecraftGuard.MOD_ID, name), item);
    }

    public static void registerModItems() {
        MinecraftGuard.LOGGER.info("Registering Mod Items for " + MinecraftGuard.MOD_ID);
        
        // Add items to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(GUARD_SWORD);
        });
        
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(GUARD_GEM);
        });
    }
}
```

## Adding a Block

### Step 1: Create Block Class

Create: `src/main/java/com/davinawuy/minecraftguard/block/ModBlocks.java`

```java
package com.davinawuy.minecraftguard.block;

import com.davinawuy.minecraftguard.MinecraftGuard;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block GUARD_BLOCK = registerBlock("guard_block",
            new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, 
                new Identifier(MinecraftGuard.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, new Identifier(MinecraftGuard.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    public static void registerModBlocks() {
        MinecraftGuard.LOGGER.info("Registering Mod Blocks for " + MinecraftGuard.MOD_ID);
    }
}
```

### Step 2: Add Block Models

Create: `src/main/resources/assets/minecraftguard/blockstates/guard_block.json`

```json
{
  "variants": {
    "": {
      "model": "minecraftguard:block/guard_block"
    }
  }
}
```

Create: `src/main/resources/assets/minecraftguard/models/block/guard_block.json`

```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "minecraftguard:block/guard_block"
  }
}
```

Create: `src/main/resources/assets/minecraftguard/models/item/guard_block.json`

```json
{
  "parent": "minecraftguard:block/guard_block"
}
```

## Adding a Custom Recipe

Create: `src/main/resources/data/minecraftguard/recipes/guard_sword.json`

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    " G ",
    " G ",
    " S "
  ],
  "key": {
    "G": {
      "item": "minecraftguard:guard_gem"
    },
    "S": {
      "item": "minecraft:stick"
    }
  },
  "result": {
    "item": "minecraftguard:guard_sword",
    "count": 1
  }
}
```

## Registering Events

### Example: Player Attack Event

```java
package com.davinawuy.minecraftguard.event;

import com.davinawuy.minecraftguard.MinecraftGuard;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class ModEvents {
    public static void registerEvents() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient()) {
                player.sendMessage(Text.literal("You attacked an entity!"), false);
                MinecraftGuard.LOGGER.info("Player {} attacked entity {}", 
                        player.getName().getString(), entity.getType().getName().getString());
            }
            return ActionResult.PASS;
        });
    }
}
```

Register in main class:

```java
@Override
public void onInitialize() {
    ModItems.registerModItems();
    ModBlocks.registerModBlocks();
    ModEvents.registerEvents();
    LOGGER.info("MinecraftGuard mod has been initialized!");
}
```

## Creating a Config File

### Using a Simple JSON Config

Create: `src/main/java/com/davinawuy/minecraftguard/config/ModConfig.java`

```java
package com.davinawuy.minecraftguard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    public boolean enableGuardSword = true;
    public int guardSwordDamage = 10;
    public String welcomeMessage = "Welcome to MinecraftGuard!";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/minecraftguard.json");

    public static ModConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

Load config in main class:

```java
public class MinecraftGuard implements ModInitializer {
    public static final String MOD_ID = "minecraftguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        ModItems.registerModItems();
        LOGGER.info("MinecraftGuard mod has been initialized!");
    }
}
```

## Testing Your Changes

After making any changes:

1. Build the mod:
   ```bash
   ./gradlew build
   ```

2. Run the development client:
   ```bash
   ./gradlew runClient
   ```

3. Test your new items/blocks in-game

4. Check logs for any errors:
   - Located in `run/logs/latest.log`

## Common Mistakes to Avoid

1. **Forgetting to register items/blocks** - Always call your registration methods
2. **Incorrect resource paths** - Make sure paths match exactly (case-sensitive)
3. **Missing translations** - Add entries to `lang/en_us.json`
4. **Not creating models** - Every item needs a model JSON file
5. **Forge code in Fabric mod** - Use Fabric API, not Forge methods

## Additional Resources

- [Fabric Wiki](https://fabricmc.net/wiki/) - Official documentation
- [Fabric API Javadoc](https://maven.fabricmc.net/docs/fabric-api-0.92.2+1.20.1/)
- [Minecraft Wiki](https://minecraft.fandom.com/wiki/Minecraft_Wiki) - Game information

## Next Steps

1. Try adding your own custom item
2. Experiment with different block properties
3. Create custom crafting recipes
4. Add custom sounds and textures
5. Explore the Fabric API for more advanced features
