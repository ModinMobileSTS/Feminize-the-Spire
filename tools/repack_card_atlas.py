"""Repack cards.atlas losslessly for RAM Saver. Requires Pillow.

Usage: python tools/repack_card_atlas.py SOURCE_CARDS_DIR NEW_EMPTY_DIR
The output (including cards.atlas) replaces the packaged cards/ directory.
"""
import argparse
from collections import defaultdict
from pathlib import Path

from PIL import Image


STARTERS = {
    "red": ("attack/strike", "skill/defend", "attack/bash"),
    "green": ("attack/strike", "skill/defend", "attack/neutralize", "skill/survivor"),
    "blue": ("attack/strike", "skill/defend", "skill/zap", "skill/dualcast"),
    "purple": ("attack/strike", "skill/defend", "attack/eruption", "skill/vigilance"),
}


def pair(text):
    return tuple(int(n) for n in text.split(","))


def read_regions(directory):
    regions = {}
    for block in (directory / "cards.atlas").read_text().strip().split("\n\n"):
        page, dimensions, fmt, filtering, repeat, *lines = block.splitlines()
        if (fmt, filtering, repeat) != ("format: RGBA8888", "filter: Linear,Linear", "repeat: none"):
            raise ValueError(f"Unsupported page options in {page}")
        with Image.open(directory / page) as img:
            if img.size != pair(dimensions.removeprefix("size: ")) or img.mode != "RGBA":
                raise ValueError(f"Unexpected page image: {page}")
            fields = {}
            for line in lines + [None]:
                if line is not None and line.startswith("  "):
                    key, value = line.strip().split(": ", 1)
                    fields[key] = value
                    continue
                if fields:
                    if name in regions or fields["rotate"] != "false":
                        raise ValueError(f"Duplicate or rotated entry: {name}")
                    x, y = pair(fields["xy"])
                    w, h = pair(fields["size"])
                    if not (0 < x <= img.width - w - 1 and 0 < y <= img.height - h - 1):
                        raise ValueError(f"Missing filter border: {name}")
                    # Preserve the exact one-texel border sampled by bilinear filtering.
                    tile = img.crop((x - 1, y - 1, x + w + 1, y + h + 1))
                    regions[name] = (fields, tile)
                name, fields = line, {}
    return regions


def pages(names):
    remaining = set(names)
    for color, paths in STARTERS.items():
        group = [f"{color}/{path}" for path in paths]
        for name in group:
            remaining.remove(name)
        yield f"{color}_starter", group, 2
    # A10+ adds this card to every starter deck. Avoid pulling in a page of other curses.
    bane = "curse/ascenders_bane"
    remaining.remove(bane)
    yield "ascenders_bane", [bane], 1
    groups = defaultdict(list)
    for name in sorted(remaining):
        groups[name.split("/", 1)[0]].append(name)
    for color, group in groups.items():
        for i in range(0, len(group), 8):
            yield f"{color}_{i // 8 + 1:02}", group[i:i + 8], 4


def repack(source, output):
    regions = read_regions(source)
    output.mkdir(exist_ok=True)
    if any(output.iterdir()):
        raise ValueError("Use an empty output directory")
    blocks = []
    for page, names, columns in pages(regions):
        rows = [names[i:i + columns] for i in range(0, len(names), columns)]
        width = max(sum(regions[n][1].width for n in row) for row in rows)
        height = sum(max(regions[n][1].height for n in row) for row in rows)
        canvas = Image.new("RGBA", (width, height))
        filename = f"{page}.png"
        block = [filename, f"size: {width},{height}", "format: RGBA8888",
                 "filter: Linear,Linear", "repeat: none"]
        y = 0
        for row in rows:
            x = 0
            for name in row:
                fields, tile = regions[name]
                canvas.paste(tile, (x, y))
                block.append(name)
                for k, v in dict(fields, xy=f"{x + 1}, {y + 1}").items():
                    block.append(f"  {k}: {v}")
                x += tile.width
            y += max(regions[n][1].height for n in row)
        canvas.save(output / filename)
        blocks.append("\n".join(block))
    (output / "cards.atlas").write_text("\n" + "\n\n".join(blocks) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    repack(args.source, args.output)
