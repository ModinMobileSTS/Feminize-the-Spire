"""Recompress packaged PNG resources to shrink the mod jar.

Rules:
- Multi-card atlas pages (feminizethespire/card_overrides/cards/) hold dozens of card
  artworks sharing one image; a single 256-color palette would band across them, so they
  are only re-encoded losslessly.
- Every other packaged PNG is a single-character illustration and is quantized to a
  256-color palette with Floyd-Steinberg dithering.

Idempotent: already-palette images are skipped, and any result larger than 90% of the
original is discarded. Rollback: `git checkout -- src/main/resources`.
"""
import io
import os
import sys

from PIL import Image, ImageChops, ImageStat

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources"))

# Not packaged (build.gradle excludes them); recompressing would be wasted work.
EXCLUDED_PREFIXES = (
    "images/npcs/",
    "images/monsters/",
)
# Multi-card atlas pages: lossless only.
LOSSLESS_PREFIXES = (
    "feminizethespire/card_overrides/cards/",
)

QUANT_COLORS = 256


def iter_pngs():
    for base, _dirs, files in os.walk(ROOT):
        for name in sorted(files):
            if not name.lower().endswith(".png"):
                continue
            full = os.path.join(base, name)
            rel = os.path.relpath(full, ROOT).replace(os.sep, "/")
            if rel.startswith(EXCLUDED_PREFIXES):
                continue
            yield full, rel


def channel_error(original, replacement):
    """Return (mean_abs_diff, max_diff) over RGBA channels."""
    diff = ImageStat.Stat(ImageChops.difference(original.convert("RGBA"), replacement.convert("RGBA")))
    means = diff.mean
    extremes = diff.extrema
    mean = sum(means) / len(means)
    peak = max(hi for _lo, hi in extremes)
    return mean, peak


def main():
    counts = {"lossless": 0, "quant": 0, "skipped": 0}
    sizes = {"lossless": [0, 0], "quant": [0, 0]}
    worst = ("", 0.0)
    error_sum = 0.0
    error_n = 0
    saved = 0

    for full, rel in iter_pngs():
        with open(full, "rb") as fh:
            data = fh.read()
        try:
            im = Image.open(io.BytesIO(data))
            im.load()
        except Exception as exc:
            print(f"SKIP unreadable {rel}: {exc}", file=sys.stderr)
            counts["skipped"] += 1
            continue

        lossless = rel.startswith(LOSSLESS_PREFIXES)
        if not lossless and im.mode == "P":
            counts["skipped"] += 1
            continue

        out = io.BytesIO()
        if lossless:
            im.save(out, "PNG", optimize=True, compress_level=9)
            new_bytes = out.getvalue()
            mean_err = peak_err = 0.0
        else:
            quantized = im.quantize(colors=QUANT_COLORS, method=Image.FASTOCTREE, dither=Image.FLOYDSTEINBERG)
            out = io.BytesIO()
            quantized.save(out, "PNG", optimize=True, compress_level=9)
            new_bytes = out.getvalue()
            mean_err, peak_err = channel_error(im, quantized)
            error_sum += mean_err * im.width * im.height
            error_n += im.width * im.height
            if peak_err > worst[1]:
                worst = (rel, float(peak_err))

        if len(new_bytes) < len(data) * 0.9:
            with open(full, "wb") as fh:
                fh.write(new_bytes)
            bucket = "lossless" if lossless else "quant"
            counts[bucket] += 1
            sizes[bucket][0] += len(data)
            sizes[bucket][1] += len(new_bytes)
            saved += len(data) - len(new_bytes)
        else:
            counts["skipped"] += 1

    print(f"lossless rewritten : {counts['lossless']:4d} files, {sizes['lossless'][0]/1e6:8.2f}MB -> {sizes['lossless'][1]/1e6:8.2f}MB")
    print(f"quantized          : {counts['quant']:4d} files, {sizes['quant'][0]/1e6:8.2f}MB -> {sizes['quant'][1]/1e6:8.2f}MB")
    print(f"skipped (tiny/P/ng): {counts['skipped']:4d}")
    print(f"total saved        : {saved/1e6:.2f}MB")
    if error_n:
        print(f"quantization error : weighted-mean {error_sum/error_n:.3f}/255, worst single-file max {worst[1]:.0f}/255 ({worst[0]})")


if __name__ == "__main__":
    main()
