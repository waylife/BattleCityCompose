# Battle City — Kotlin Multiplatform / Compose Multiplatform

A cross-platform port of the classic FC Battle City, originally written in C
for UEFI Shell (see `UEFI_battlecity/` for the reference). The game logic
runs entirely in `commonMain`; rendering uses Compose Multiplatform (Skia)
and runs on Android, iOS, and Desktop (JVM).

## Quick start

```bash
# 1. Generate placeholder sprite PNGs and level .map files.
python3 scripts/generate_assets.py

# 2a. Desktop (JVM) — fastest feedback loop.
./gradlew :composeApp:run

# 2b. Android — install onto a connected device or emulator.
./gradlew :composeApp:installDebug

# 2c. iOS — open iosApp/iosApp.xcodeproj in Xcode and Run.
# (After the first Xcode build, gradle :composeApp:linkDebug* tasks
#  build the Compose framework that Xcode embeds.)
```

## Controls

| Platform | Player 1           | Player 2 | System       |
|----------|--------------------|----------|--------------|
| Desktop  | Arrow keys + Space | WASD     | Enter / Esc  |
| Android  | Virtual pad + 🔥   | (n/a)    | Tap to start |
| iOS      | Virtual pad + 🔥   | (n/a)    | Tap to start |

## Project layout

```
BattleCityCompose/
├── composeApp/
│   ├── build.gradle.kts                  # KMP + CMP application module
│   └── src/
│       ├── commonMain/kotlin/com/battlecity/
│       │   ├── App.kt                    # Top-level composable
│       │   ├── clock/Clock.kt            # expect
│       │   ├── engine/                   # 14 files of pure game logic
│       │   │   ├── GameConfig.kt         # All UEFI constants
│       │   │   ├── Direction.kt
│       │   │   ├── Sprite.kt
│       │   │   ├── Tile.kt               # 16-bit mask sub-cells
│       │   │   ├── Plane.kt              # 13×13 map + 4-quadrant collision
│       │   │   ├── Tank.kt               # speed/speedRem, lane-snap
│       │   │   ├── Player.kt
│       │   │   ├── Enemy.kt
│       │   │   ├── Bullet.kt
│       │   │   ├── Bonus.kt
│       │   │   ├── Explode.kt
│       │   │   ├── GameState.kt
│       │   │   ├── GameEngine.kt         # 30 FPS update tick
│       │   │   ├── MapLoader.kt          # Parses .map binary
│       │   │   ├── InputState.kt
│       │   │   └── Rng.kt
│       │   ├── render/                   # Compose Canvas drawing
│       │   │   ├── SpriteAtlas.kt
│       │   │   ├── GameCanvas.kt         # Mirrors UEFI GameRender order
│       │   │   ├── SplashOverlay.kt
│       │   │   └── GameScreen.kt
│       │   └── input/
│       │       ├── KeyboardInput.kt      # Modifier for Compose keys
│       │       └── VirtualPad.kt         # Touch d-pad + fire
│       ├── commonMain/composeResources/
│       │   ├── images/*.png              # 15 sprite sheets
│       │   └── files/maps/level*.map     # 20 levels
│       ├── commonTest/                   # Engine unit tests
│       ├── androidMain/                  # MainActivity, AndroidClock
│       ├── iosMain/                      # MainViewController, IosClock
│       └── desktopMain/                  # main(), DesktopClock
├── iosApp/                               # Xcode project (open this in Xcode)
├── scripts/generate_assets.py            # Placeholder asset generator
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
└── UEFI_battlecity/                      # Original C source (read-only reference)
```

## Tests

```bash
./gradlew :composeApp:jvmTest
```

Engine unit tests cover:
- `MapLoaderTest` — parses 13×13 maps, BRICK/CONCRETE mask bytes
- `TankMoveTest` — verifies `SpeedRem` accumulator behaviour
- `TileHitTestTest` — verifies 16-bit mask destruction in all 4 directions
- `GameStateMachineTest` — `SPLASH → ACTIVE → WIN → next level` transitions
- `KeyBitsTest` — bitmask encoding/decoding

## Re-skin with real art

Drop your PNG files into
`composeApp/src/commonMain/composeResources/images/` (use the same
filenames; the Kotlin code references them by name). To replace
`.map` files, drop new ones into
`composeApp/src/commonMain/composeResources/files/maps/levelN.map`.

The `scripts/generate_assets.py` script currently writes minimal
solid-colour placeholders so the project builds out of the box.
Re-run the script after copying real BMP→PNG conversions from
`UEFI_battlecity/graphics/`.
