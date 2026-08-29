#!/usr/bin/env python3
"""Draws the three block textures for the time machine.

Sixteen-by-sixteen pixel art is faster to describe in code than to nudge around in an editor,
and this way the palette stays consistent across the three faces. Run it with:

    python3 tools/gen_assets.py

Everything it writes is checked in, so a build never depends on running it.
"""

import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEXTURES = os.path.join(ROOT, "src/main/resources/assets/timemachine/textures/block")

# Gunmetal casing, cyan light. The same cyan the dial and the HUD use.
FRAME = (0x14, 0x18, 0x1C)
CASE_DARK = (0x25, 0x2B, 0x31)
CASE = (0x2E, 0x35, 0x3C)
CASE_LIGHT = (0x3A, 0x43, 0x4B)
RIVET = (0x55, 0x60, 0x69)
GLOW_DIM = (0x1E, 0x6B, 0x82)
GLOW = (0x2F, 0xA8, 0xC8)
GLOW_HOT = (0x8C, 0xE8, 0xFF)
NEEDLE = (0xFF, 0xC2, 0x4A)


def write_png(path, rows):
    """rows is 16 lists of 16 (r, g, b) tuples."""
    raw = b"".join(b"\x00" + bytes(c for px in row for c in (px + (255,))) for row in rows)

    def chunk(tag, body):
        return (struct.pack(">I", len(body)) + tag + body
                + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)


def base():
    """A riveted metal panel: dark frame, mottled face, a rivet in each corner."""
    grid = [[CASE for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                grid[y][x] = FRAME
            elif x in (1, 14) or y in (1, 14):
                grid[y][x] = CASE_DARK
            elif (x * 7 + y * 5) % 11 == 0:
                grid[y][x] = CASE_LIGHT
            elif (x * 3 + y * 11) % 13 == 0:
                grid[y][x] = CASE_DARK
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        grid[y][x] = RIVET
    return grid


def side():
    """A seam of light running round the middle of the casing."""
    grid = base()
    for x in range(2, 14):
        grid[7][x] = GLOW_DIM
        grid[8][x] = GLOW if x % 3 else GLOW_HOT
    return grid


def top():
    """A ring of light around a dark well, so the machine glows from above."""
    grid = base()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d < 2.2:
                grid[y][x] = FRAME
            elif 3.0 <= d < 4.0:
                grid[y][x] = GLOW_HOT if (x + y) % 2 == 0 else GLOW
            elif 4.0 <= d < 4.8:
                grid[y][x] = GLOW_DIM
    return grid


def front():
    """The dial: a lit face, tick marks round the rim, and a needle stuck in the past."""
    grid = base()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d < 5.6:
                grid[y][x] = GLOW_DIM if d > 4.9 else FRAME

    # Twelve ticks around the face, the way a clock has hours.
    for i in range(12):
        angle = i * math.pi / 6
        x = int(round(cx + math.cos(angle) * 4.6))
        y = int(round(cy + math.sin(angle) * 4.6))
        grid[y][x] = GLOW_HOT if i % 3 == 0 else GLOW

    # The needle, pointing back and up — anticlockwise is the direction of travel.
    for step in range(1, 5):
        x = int(round(cx - step * 0.72))
        y = int(round(cy - step * 0.62))
        grid[y][x] = NEEDLE
    grid[7][7] = GLOW_HOT
    grid[8][8] = GLOW_HOT
    return grid


def main():
    write_png(os.path.join(TEXTURES, "time_machine_side.png"), side())
    write_png(os.path.join(TEXTURES, "time_machine_top.png"), top())
    write_png(os.path.join(TEXTURES, "time_machine_front.png"), front())
    print("wrote 3 textures to", TEXTURES)


if __name__ == "__main__":
    main()
