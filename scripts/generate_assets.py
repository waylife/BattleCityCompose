#!/usr/bin/env python3
"""
Generate placeholder Battle City assets (sprite PNGs + level .map files).

Why this exists:
- The original UEFI project uses 4/8 bpp indexed BMPs that need conversion to
  PNG for Compose Resources.
- The real BMPs are not committed in this repo (they're upstream artifacts),
  so we generate minimal stand-ins. Once you drop real BMPs from
  `UEFI_battlecity/graphics/` and `map/` into the appropriate folders, you
  can replace these placeholders (or re-run this with a real converter).

Output layout:
- composeApp/src/commonMain/composeResources/images/*.png
- composeApp/src/commonMain/composeResources/files/maps/levelN.map

Usage:
    python3 scripts/generate_assets.py
"""
import os
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES_DIR = ROOT / "composeApp" / "src" / "commonMain" / "composeResources"
IMG_DIR = RES_DIR / "images"
MAP_DIR = RES_DIR / "files" / "maps"


def write_png(path: Path, w: int, h: int, pixels: bytes) -> None:
    """Write a minimal RGBA PNG. `pixels` is row-major, 4 bytes per pixel."""
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    # Each row prefixed with filter byte 0.
    raw = b"".join(b"\x00" + pixels[y * w * 4:(y + 1) * w * 4] for y in range(h))
    idat = chunk(b"IDAT", zlib.compress(raw, 9))
    iend = chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(sig + ihdr + idat + iend)


def fill_solid(w: int, h: int, rgba: tuple) -> bytes:
    return bytes(rgba) * (w * h)


def fill_pattern(w: int, h: int, fg: tuple, bg: tuple, stripe: int = 4) -> bytes:
    out = bytearray()
    for y in range(h):
        for x in range(w):
            out.extend(fg if ((x // stripe + y // stripe) & 1) == 0 else bg)
    return bytes(out)


def main():
    IMG_DIR.mkdir(parents=True, exist_ok=True)
    MAP_DIR.mkdir(parents=True, exist_ok=True)

    # Sprite sheets with the dimensions described in the UEFI source.
    sheets = {
        "tile.png":     (32 * 6, 32, fill_solid(32 * 6, 32, (200, 200, 200, 255))),
        "player1.png":  (28 * 8, 28 * 4, fill_pattern(28 * 8, 28 * 4, (255, 200, 0, 255), (0, 0, 0, 255))),
        "player2.png":  (28 * 8, 28 * 4, fill_pattern(28 * 8, 28 * 4, (0, 200, 255, 255), (0, 0, 0, 255))),
        "bullet.png":   (8 * 4, 8, fill_solid(8 * 4, 8, (255, 255, 255, 255))),
        "enemy.png":    (28 * 12, 28 * 8, fill_pattern(28 * 12, 28 * 8, (180, 180, 180, 255), (0, 0, 0, 255))),
        "explode1.png": (28, 28, fill_solid(28, 28, (255, 200, 0, 255))),
        "explode2.png": (64, 64, fill_solid(64, 64, (255, 100, 0, 255))),
        "bonus.png":    (30 * 6, 28, fill_pattern(30 * 6, 28, (255, 255, 0, 255), (0, 0, 200, 255))),
        "bore.png":     (28, 28, fill_solid(28, 28, (100, 100, 100, 255))),
        "shield.png":   (32, 64, fill_solid(32, 64, (0, 200, 255, 200))),
        "misc.png":     (84, 14, fill_solid(84, 14, (180, 180, 180, 255))),
        "num.png":      (14 * 10, 14, fill_solid(14 * 10, 14, (255, 255, 255, 255))),
        "splash.png":   (376, 222, fill_solid(376, 222, (32, 32, 32, 255))),
        "gameover.png": (248, 160, fill_solid(248, 160, (200, 30, 30, 255))),
        "flag.png":     (32, 32, fill_solid(32, 32, (255, 0, 0, 255))),
    }
    for name, (w, h, px) in sheets.items():
        write_png(IMG_DIR / name, w, h, px)
        print(f"  wrote {name} ({w}x{h})")

    # 20 levels: minimal valid map data — full concrete border with some brick
    # scatter and a single HAWK base in the bottom-middle.
    # Each cell is 1 byte (kind: 0=NULL 1=BRICK 2=CONCRETE 3=TREE 4=RIVER 5=STONE 6=HAWK).
    # BRICK / CONCRETE take a follow-up mask byte (use 1 = bottom half for variety).
    PLANE_W = 13
    PLANE_H = 13
    for lvl in range(1, 21):
        buf = bytearray()
        for r in range(PLANE_H):
            for c in range(PLANE_W):
                # Concrete border.
                if r == 0 or c == 0 or r == PLANE_H - 1 or c == PLANE_W - 1:
                    buf.append(2)  # CONCRETE
                    buf.append(0xFF)  # full mask
                # Hawk base at the bottom middle.
                elif r == 12 and c in (5, 6, 7):
                    if c == 6:
                        buf.append(6)  # HAWK
                    else:
                        buf.append(2)  # CONCRETE wall
                        buf.append(0xFF)
                # Scattered bricks in the middle rows.
                elif lvl % 2 == 0 and r in (4, 8) and c in (3, 5, 7, 9):
                    buf.append(1)  # BRICK
                    buf.append(0xCC)  # diagonal mask
                # Trees at the corners.
                elif (r, c) in ((2, 2), (2, 10), (10, 2), (10, 10)):
                    buf.append(3)  # TREE
                else:
                    buf.append(0)  # NULL
        out = MAP_DIR / f"level{lvl}.map"
        out.write_bytes(bytes(buf))
        print(f"  wrote level{lvl}.map ({len(buf)} bytes)")


if __name__ == "__main__":
    main()
