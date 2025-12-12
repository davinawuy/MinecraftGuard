# MinecraftGuard

This is a test of the ability of AI to create Minecraft Mods.

## About

MinecraftGuard is a Minecraft Java mod built using the **Fabric mod loader**. This project serves as a foundation for creating custom Minecraft modifications.

This repository contains a complete base setup for Minecraft mod development, including:
- ✅ Gradle build system with wrapper
- ✅ Fabric mod loader configuration
- ✅ Proper directory structure
- ✅ Main mod class with initialization
- ✅ Mod metadata (fabric.mod.json)
- ✅ Mixin configuration
- ✅ Comprehensive setup documentation

See [SETUP.md](SETUP.md) for detailed setup instructions and [EXAMPLES.md](EXAMPLES.md) for code examples.

## Prerequisites

- Java 17 or higher
- Gradle (included via Gradle Wrapper)

## Prerequisites

- **Java 17 or higher** - [Download JDK](https://adoptium.net/)
- **Network access** to Maven repositories:
  - https://maven.fabricmc.net/ (Fabric Maven Repository)
  - https://repo.maven.apache.org/maven2/ (Maven Central)

## Building the Mod

To build the mod, run:

```bash
./gradlew build
```

**Note**: The first build will download Minecraft, Fabric Loader, Fabric API, and other dependencies. This may take 5-15 minutes depending on your internet connection.

The compiled mod JAR will be located in `build/libs/`.

## Development

This mod uses:
- **Minecraft Version**: 1.20.1
- **Fabric Loader**: 0.15.11
- **Fabric API**: 0.92.2+1.20.1
- **Yarn Mappings**: 1.20.1+build.10

### Running the Development Client

To test the mod in a development environment:

```bash
./gradlew runClient
```

### Running the Development Server

To test the mod on a development server:

```bash
./gradlew runServer
```

## Project Structure

```
MinecraftGuard/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/davinawuy/minecraftguard/
│       │       └── MinecraftGuard.java    # Main mod class
│       └── resources/
│           ├── fabric.mod.json             # Mod metadata
│           └── minecraftguard.mixins.json  # Mixin configuration
├── build.gradle                            # Gradle build script
├── gradle.properties                       # Project properties
└── settings.gradle                         # Gradle settings

```

## Adding Features

To add new features to your mod:

1. Create new Java classes in `src/main/java/com/davinawuy/minecraftguard/`
2. Register items, blocks, or other content in the main mod class
3. Use Fabric API's registration methods
4. Build and test your changes

## License

All Rights Reserved - This mod is provided for testing purposes only.
