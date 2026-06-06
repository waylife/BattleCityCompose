# Battle City — Kotlin Multiplatform / Compose Multiplatform

A cross-platform port of the classic FC Battle City, originally written in C
for UEFI Shell (see `UEFI_battlecity/` for the reference). The game logic
runs entirely in `commonMain`; rendering uses Compose Multiplatform (Skia)
and runs on Android, iOS, and Desktop (JVM).

## Credits

Ported from the original C/UEFI implementation:
[https://github.com/MikeWuPing/UEFI_battlecity](https://github.com/MikeWuPing/UEFI_battlecity)

## Quick start

```bash
# 1. Generate placeholder sprite PNGs and level .map files.
python3 scripts/generate_assets.py

# 2a. Desktop (JVM) — fastest feedback loop.
./gradlew :composeApp:desktopRun

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

> Player 2 is inactive by default on the desktop build (single-player mode); the P2 WASD binding only takes effect once a two-player mode is wired in.

## How to play

### Objective
- **Defend the base**: the eagle nest (HAWK) at the bottom-center of the map. If a bullet hits it, the game is over.
- **Clear the level**: destroy all **20** enemy tanks in a stage. Eliminating them all advances you to the next stage.
- **Survive**: each player starts with **3 lives**. Lose one per hit. Game ends when lives run out.

### Stages
- **20 stages** in total, loaded sequentially from `level1.map` to `level20.map`.
- Each stage spawns a mix of three enemy types at random:
  - **Type 0** Basic (speed 0.7, worth 100 pts)
  - **Type 1** Fast (speed 1.2, worth 200 pts)
  - **Type 2** Armoured (speed 0.5, takes 4 hits, worth 300 pts)
- Up to 4 enemies on screen in single-player mode, 6 in two-player mode. New enemies spawn from one of the 4 spawn points at the top of the map, in round-robin, every 3 seconds.

### Map tiles
| Tile | Behaviour |
|------|-----------|
| `BRICK` | Any bullet chips off a 1/4 sub-cell (16-bit mask, 8×8 sub-grid). |
| `CONCRETE` | **Only the player's Type ≥ 3 bullet can break it.** |
| `TREE` | Passable for tanks and bullets; visually hides both sides. |
| `RIVER` | Passable for everyone; the water sprite animates at 0.5s per frame. |
| `STONE` | Indestructible and impassable. |
| `HAWK` (base) | Impassable; a single hit ends the game immediately. |

### Player upgrades
Picking up a **STAR** upgrades your tank, increasing bullet speed and bullet capacity:

| Type | Bullet speed | Bullet slots | Notes |
|------|--------------|--------------|-------|
| 0    | 3.0          | 1            | Initial |
| 1    | 4.0          | 1            | + 1 star |
| 2    | 5.0          | 2            | + 2 stars (dual-fire unlocked) |
| 3    | 5.0          | 2            | + 3 stars (bullets break concrete) |

> Max Type is 3; further STARs do nothing. On respawn the type resets to 0 (you have to re-grab stars).

### Bonus items
When the **4th, 11th, or 18th** enemy of a stage is killed, that enemy drops a random bonus that flickers for 10 seconds before despawning. Player and enemy pickups have different effects:

| Bonus | Player picks up | Enemy picks up |
|-------|-----------------|----------------|
| `LIFE` Extra life | +1 life (no cap) | +5 to the stage's total enemy count |
| `CLOCK` Freeze | Freeze all enemies for 10 s | Lock the player for 5 s (cannot move) |
| `SHOVEL` Wall | Upgrade the base wall to concrete for 20 s | Strip the base wall entirely (naked base) |
| `BOMB` Bomb | Instantly destroy every enemy on screen | Players lose 1 life, shield or not |
| `STAR` Upgrade | Upgrade the tank (Type 0 → 3) | Instantly become armoured (Type 2) |
| `HELMET` Shield | 10-second invulnerability shield | 10-second invulnerability shield |

> Players respawn with a 3-second spawn shield; enemies respawn with a 0.8-second shield (red/white flicker). Both shields block bullets.

### Tips
- **You can't ram bricks**: tanks cannot break brick or concrete by driving into them — use bullets.
- **Aim for the seams**: bullets hit-test on a 4×4 sub-grid, so diagonal or corner shots are often more efficient at clearing walls than dead-on shots.
- **Shields save lives**: TREE bushes hide you from bullets but not from tank collisions. When surrounded, grab a STAR to reach Type 2 for dual-fire, then push to Type 3 to break concrete walls.
- **The base comes first**: the 4th / 11th / 18th enemy always drops a bonus — **always grab the SHOVEL** when it spawns; 20 seconds of concrete wall is the key to surviving the late game.
- **Pause**: press `Esc` to return to the title screen. Press `Esc` again on the title screen to set the `bored` flag (no extra behaviour wired up yet — kept as an extension hook).

### Scoring
- Score per kill = `(enemy Type + 1) × 100`, i.e. 100 / 200 / 300.
- Extra lives, upgrades, and shields **do not grant points** — they only change state.
- Respawning does not deduct points, but you do lose your upgrade level and shield.

### Win / lose
- **GAME OVER**:
  - The base (HAWK) is hit — the base is the top priority.
  - Both players (in two-player mode) run out of lives.
- **Stage clear**: when all 20 enemies of stage N are destroyed, the WIN screen shows for 3 seconds, then the game advances to `level(N+1).map` (wraps back to level 1 after stage 20).
- Press `Enter` on the title screen to start a new game (score, lives, and stage are all reset).

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
