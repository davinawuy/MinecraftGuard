# Minecraft Mod Development Setup Guide

This guide will help you set up your development environment for the MinecraftGuard mod.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17 or higher**
   - Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
   - Verify installation: `java -version`

2. **Git** (for version control)
   - Download from [git-scm.com](https://git-scm.com/)
   - Verify installation: `git --version`

3. **An IDE** (Integrated Development Environment)
   - **IntelliJ IDEA** (Recommended) - [Download](https://www.jetbrains.com/idea/download/)
   - **Eclipse** - [Download](https://www.eclipse.org/downloads/)
   - **Visual Studio Code** with Java extensions - [Download](https://code.visualstudio.com/)

### Network Requirements

Building this mod requires access to the following Maven repositories:
- **Fabric Maven Repository**: https://maven.fabricmc.net/
- **Maven Central**: https://repo.maven.apache.org/maven2/

If you're behind a corporate firewall or proxy, you may need to configure Gradle to work with your network. See the [Gradle Proxy Configuration](#gradle-proxy-configuration) section below.

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/davinawuy/MinecraftGuard.git
cd MinecraftGuard
```

### 2. Verify Gradle Wrapper

The project includes a Gradle Wrapper, which ensures everyone uses the same Gradle version:

```bash
# On Linux/macOS:
./gradlew --version

# On Windows:
gradlew.bat --version
```

### 3. Build the Mod

To build the mod for the first time:

```bash
# On Linux/macOS:
./gradlew build

# On Windows:
gradlew.bat build
```

**First build will take several minutes** as Gradle downloads:
- Minecraft game files
- Fabric Loader
- Fabric API
- Yarn mappings (deobfuscation mappings)

## Development Workflow

### Running the Development Client

Test your mod in a Minecraft client:

```bash
./gradlew runClient
```

This will:
1. Set up a test environment
2. Launch Minecraft with your mod loaded
3. Any changes you make will require rebuilding and restarting

### Running the Development Server

Test your mod on a Minecraft server:

```bash
./gradlew runServer
```

### Making Changes

1. Edit Java files in `src/main/java/`
2. Add resources to `src/main/resources/`
3. Build the mod: `./gradlew build`
4. Test: `./gradlew runClient`

### Building for Distribution

To create a JAR file you can share:

```bash
./gradlew build
```

The built JAR will be in: `build/libs/minecraftguard-1.0.0.jar`

## IDE Setup

### IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. Click **File** → **Open**
3. Select the `MinecraftGuard` folder
4. Wait for Gradle to sync (shown in bottom-right corner)
5. IntelliJ will automatically detect the Gradle project

#### Generate Run Configurations

```bash
./gradlew genIntellijRuns
```

After running this, IntelliJ will have run configurations for:
- Minecraft Client
- Minecraft Server

### Eclipse

1. Open Eclipse
2. Click **File** → **Import** → **Gradle** → **Existing Gradle Project**
3. Select the `MinecraftGuard` folder
4. Click **Finish**

#### Generate Run Configurations

```bash
./gradlew eclipse
./gradlew genEclipseRuns
```

### Visual Studio Code

1. Install Java Extension Pack from marketplace
2. Open the `MinecraftGuard` folder
3. VS Code will detect the Gradle project
4. Use the built-in terminal to run Gradle commands

## Project Structure

```
MinecraftGuard/
├── src/
│   └── main/
│       ├── java/                          # Java source code
│       │   └── com/davinawuy/minecraftguard/
│       │       └── MinecraftGuard.java    # Main mod class
│       └── resources/
│           ├── fabric.mod.json            # Mod metadata
│           ├── minecraftguard.mixins.json # Mixin configuration
│           └── assets/                    # Textures, models, sounds
│               └── minecraftguard/
├── build/                                 # Build output (ignored by git)
│   └── libs/                             # Compiled mod JARs
├── run/                                  # Development environment (ignored by git)
├── build.gradle                          # Build configuration
├── gradle.properties                     # Project properties
└── settings.gradle                       # Gradle settings
```

## Gradle Proxy Configuration

If you're behind a proxy, create/edit `gradle.properties` in your home directory:

### Linux/macOS
Location: `~/.gradle/gradle.properties`

### Windows
Location: `C:\Users\YourName\.gradle\gradle.properties`

### Proxy Settings

```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.http.proxyUser=username
systemProp.http.proxyPassword=password

systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
systemProp.https.proxyUser=username
systemProp.https.proxyPassword=password
```

## Common Issues

### "Could not resolve dependencies"

**Problem**: Cannot download dependencies from Maven repositories.

**Solutions**:
1. Check your internet connection
2. Verify you can access https://maven.fabricmc.net/
3. Configure proxy settings if behind a firewall
4. Try running with `--refresh-dependencies`: `./gradlew build --refresh-dependencies`

### "Java version mismatch"

**Problem**: Wrong Java version installed.

**Solution**: 
1. Install JDK 17 or higher
2. Set `JAVA_HOME` environment variable to point to JDK 17+
3. Verify: `java -version` should show 17 or higher

### "Minecraft crashes on launch"

**Problem**: Mod has errors or incompatibilities.

**Solutions**:
1. Check the crash log in `run/logs/latest.log`
2. Ensure all dependencies in `build.gradle` are correct
3. Verify `fabric.mod.json` has correct entrypoint

### "Build takes too long"

**Problem**: First build downloads many dependencies.

**Solutions**:
1. First build takes 5-15 minutes - this is normal
2. Subsequent builds will be much faster (cached)
3. Increase Gradle memory: Edit `gradle.properties` and set `org.gradle.jvmargs=-Xmx4G`

## Additional Resources

### Documentation
- [Fabric Wiki](https://fabricmc.net/wiki/) - Official Fabric documentation
- [Fabric API Javadocs](https://maven.fabricmc.net/docs/fabric-api-0.92.2+1.20.1/) - API documentation
- [Minecraft Wiki](https://minecraft.fandom.com/wiki/Minecraft_Wiki) - Game mechanics reference

### Community
- [Fabric Discord](https://discord.gg/v6v4pMv) - Get help from the community
- [Fabric GitHub](https://github.com/FabricMC) - Source code and issue tracking

### Tutorials
- [Fabric Wiki Tutorials](https://fabricmc.net/wiki/tutorial:start) - Step-by-step guides
- [Modded Minecraft Tutorials](https://www.youtube.com/results?search_query=fabric+minecraft+mod+tutorial) - Video tutorials

## Next Steps

1. ✅ You've set up the base project structure
2. 📝 Read the [Fabric Wiki](https://fabricmc.net/wiki/) to learn about mod development
3. 🎨 Add your first item or block
4. 🧪 Test in the development environment
5. 📦 Build and share your mod!

## Getting Help

If you encounter issues:

1. Check the [Common Issues](#common-issues) section above
2. Search existing [GitHub Issues](https://github.com/davinawuy/MinecraftGuard/issues)
3. Ask on the [Fabric Discord](https://discord.gg/v6v4pMv)
4. Create a new [GitHub Issue](https://github.com/davinawuy/MinecraftGuard/issues/new) with details
