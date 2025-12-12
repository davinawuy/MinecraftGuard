# Quick Start Guide

Get up and running with MinecraftGuard mod development in 5 minutes!

## Prerequisites Check

Before starting, verify you have:

```bash
# Check Java version (must be 17+)
java -version

# Check Git
git --version
```

If either is missing:
- **Java 17+**: Download from [Adoptium](https://adoptium.net/)
- **Git**: Download from [git-scm.com](https://git-scm.com/)

## Quick Setup

### 1. Clone & Navigate

```bash
git clone https://github.com/davinawuy/MinecraftGuard.git
cd MinecraftGuard
```

### 2. Build the Mod

**Linux/macOS:**
```bash
./gradlew build
```

**Windows:**
```bash
gradlew.bat build
```

⏱️ **First build takes 5-15 minutes** (downloads Minecraft & dependencies)

### 3. Test in Minecraft

**Linux/macOS:**
```bash
./gradlew runClient
```

**Windows:**
```bash
gradlew.bat runClient
```

🎮 This launches Minecraft with your mod loaded!

## Project Files

```
MinecraftGuard/
├── src/main/java/               # Your Java code goes here
│   └── com/davinawuy/minecraftguard/
│       └── MinecraftGuard.java  # Main mod file
├── src/main/resources/          # Resources (JSON, textures, etc.)
│   ├── fabric.mod.json          # Mod information
│   └── assets/minecraftguard/   # Textures, models, sounds
├── build.gradle                 # Build configuration
└── gradle.properties            # Mod version & dependencies
```

## Making Your First Change

### Add a Custom Item

1. **Create item registration class:**

```bash
# Linux/macOS
mkdir -p src/main/java/com/davinawuy/minecraftguard/item
nano src/main/java/com/davinawuy/minecraftguard/item/ModItems.java

# Windows
mkdir src\main\java\com\davinawuy\minecraftguard\item
notepad src\main\java\com\davinawuy\minecraftguard\item\ModItems.java
```

2. **Add this code:**

```java
package com.davinawuy.minecraftguard.item;

import com.davinawuy.minecraftguard.MinecraftGuard;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item CUSTOM_ITEM = registerItem("custom_item",
            new Item(new FabricItemSettings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, 
                new Identifier(MinecraftGuard.MOD_ID, name), item);
    }

    public static void registerModItems() {
        MinecraftGuard.LOGGER.info("Registering Mod Items");
    }
}
```

3. **Update MinecraftGuard.java:**

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
        ModItems.registerModItems();  // Add this line
        LOGGER.info("MinecraftGuard mod has been initialized!");
    }
}
```

4. **Build and test:**

```bash
./gradlew build
./gradlew runClient
```

🎉 Your custom item is now in the game!

## Common Commands

### Build & Run

```bash
# Build mod (creates JAR in build/libs/)
./gradlew build

# Run Minecraft client with your mod
./gradlew runClient

# Run dedicated server with your mod
./gradlew runServer

# Clean build files
./gradlew clean
```

### IDE Setup

```bash
# For IntelliJ IDEA
./gradlew genIntellijRuns

# For Eclipse
./gradlew eclipse
./gradlew genEclipseRuns
```

## What's Next?

✅ You now have a working Minecraft mod!

**Continue Learning:**
- 📖 Read [EXAMPLES.md](EXAMPLES.md) for more code examples
- 📚 Check [SETUP.md](SETUP.md) for detailed documentation
- 🌐 Visit [Fabric Wiki](https://fabricmc.net/wiki/) for tutorials
- 💬 Join [Fabric Discord](https://discord.gg/v6v4pMv) for help

**Try Adding:**
- 🗡️ Custom weapons with special abilities
- 🧱 New blocks with unique properties
- 🎨 Custom textures and models
- ⚙️ Configuration files
- 🎯 Event handlers

## Troubleshooting

### "Could not resolve dependencies"

❌ **Problem:** Can't download Fabric dependencies

✅ **Solution:**
1. Check internet connection
2. Verify access to https://maven.fabricmc.net/
3. If behind proxy, see [SETUP.md#gradle-proxy-configuration](SETUP.md#gradle-proxy-configuration)

### "Java version mismatch"

❌ **Problem:** Wrong Java version

✅ **Solution:**
```bash
# Install Java 17 or higher
# Then set JAVA_HOME environment variable
# Verify:
java -version  # Should show 17 or higher
```

### Build is too slow

❌ **Problem:** Gradle using too little memory

✅ **Solution:**

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G
```

## Getting Help

1. 📖 Check [SETUP.md](SETUP.md) for detailed guides
2. 🔍 Search [existing issues](https://github.com/davinawuy/MinecraftGuard/issues)
3. 💬 Ask on [Fabric Discord](https://discord.gg/v6v4pMv)
4. 🐛 [Create an issue](https://github.com/davinawuy/MinecraftGuard/issues/new)

## File Locations

- **Your mod JAR**: `build/libs/minecraftguard-1.0.0.jar`
- **Game logs**: `run/logs/latest.log`
- **Crash reports**: `run/crash-reports/`
- **Config files**: `config/` (when you add them)

---

**Happy Modding! 🎮✨**

For more examples and tutorials, check out the [EXAMPLES.md](EXAMPLES.md) file!
