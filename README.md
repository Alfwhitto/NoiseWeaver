# NoiseWeaver

NoiseWeaver is a client-side Fabric mod for Minecraft 26.3 that chooses among weighted blocks from your inventory when you place a block. It uses coordinate-based value noise so nearby placements tend to form patches instead of a fully random scatter.

## Features

- Reads block items carried in your inventory and hotbar.
- Configure each block with an enable toggle and a weight slider from `0.00` to `1.00`.
- Displays each enabled block's normalized share as a percentage.
- Applies the palette to ordinary block placement and Effortless Building's bulk placement path.
- Saves settings to `config/noiseweaver.json`.

## Controls

| Action | Default key | Change it |
| --- | --- | --- |
| Open the NoiseWeaver palette | `H` | Minecraft **Options → Controls → Key Binds** |
| Toggle NoiseWeaver placement | `T` | Minecraft **Options → Controls → Key Binds** |

Only enabled blocks currently carried by the player are eligible. A zero weight excludes a block. Percentages are calculated by dividing each enabled block's weight by the total enabled weight.

## Install

1. Install Fabric Loader and Fabric API for Minecraft 26.3.
2. Put the NoiseWeaver JAR in the instance's `mods` folder.
3. Start the game, carry the blocks you want to use, open the palette with `H`, and enable placement with `T`.

Effortless Building is optional. The compatibility hook targets its Minecraft 26.3 bulk-placement handler when that mod is present. The current implementation is intended for singleplayer/integrated-server play; it does not synchronize the toggle or palette to a remote multiplayer server.

## Build from source

Requirements: JDK 25. The Gradle wrapper downloads and uses Gradle 9.6.0.

```powershell
.\gradlew.bat build
```

The mod JAR is written to `build/libs/noiseweaver-1.0.0.jar`.

## License

NoiseWeaver is licensed under the MIT License. See [LICENSE](LICENSE).

Minecraft is a trademark of Microsoft/Mojang. NoiseWeaver is an independent project and is not affiliated with or endorsed by Mojang or Microsoft.
