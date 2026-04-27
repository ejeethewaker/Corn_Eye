"""
download_invalid_dataset.py
============================
Downloads targeted "Invalid" class images from Open Images v7 via the
fiftyone library and organises them into the training pipeline's Invalid
directory structure.

What it downloads (max 300 images each, train + val):
  • Mobile phone
  • Human hand
  • Paper
  • Computer monitor / Laptop
  • Wall  (plain backgrounds)
  • Human face  (skin tones that can confuse the color filter)

Output layout (drops into your existing dataset root):
    <DATASET_ROOT>/
        train/
            _invalid_realworld/   ← all downloaded train images land here
                *.jpg
        val/
            _invalid_realworld/   ← all downloaded val images land here
                *.jpg

The folder name starts with "_" so it does NOT match any CORN_FOLDER_MAP
key and will therefore be picked up as "Invalid" by collect_paths() in
train_model.py — no changes to train_model.py needed.

Usage:
    pip install fiftyone Pillow
    python scripts/download_invalid_dataset.py

Optional flags:
    --root   Path to dataset root   (default: C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full)
    --max    Max images per class   (default: 300)
    --seed   Random seed            (default: 42)
"""

import argparse
import os
import random
import shutil
import sys

# ── Config ────────────────────────────────────────────────────────────────────
DEFAULT_ROOT    = r"C:\Users\camsy\Downloads\PlantsLeafs\New Plant Diseases Dataset(Augmented)\New Plant Diseases Dataset(Augmented)"
DEFAULT_MAX     = 300   # images per category per split
DEFAULT_SEED    = 42

# Open Images v7 label names to download for the "Invalid" class.
# These were chosen because they represent what users accidentally scan instead
# of corn leaves: phones, hands, paper, screens, walls, faces.
CATEGORIES = [
    "Mobile phone",
    "Human hand",
    "Paper",
    "Computer monitor",
    "Laptop",
    "Wall",
    "Human face",
]

# ── Argument parsing ──────────────────────────────────────────────────────────
parser = argparse.ArgumentParser(description="Download Invalid-class images from Open Images v7")
parser.add_argument("--root", default=DEFAULT_ROOT, help="Dataset root directory")
parser.add_argument("--max",  type=int, default=DEFAULT_MAX, help="Max images per category per split")
parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help="Random seed")
args = parser.parse_args()

DATASET_ROOT = args.root
MAX_PER_CAT  = args.max
SEED         = args.seed

random.seed(SEED)

# ── Validate dataset root ─────────────────────────────────────────────────────
if not os.path.isdir(DATASET_ROOT):
    print(f"ERROR: Dataset root not found: {DATASET_ROOT}")
    print("       Set --root to your PlantsLeafs/Full directory.")
    sys.exit(1)

TRAIN_INVALID_DIR = os.path.join(DATASET_ROOT, "train", "_invalid_realworld")
VAL_INVALID_DIR   = os.path.join(DATASET_ROOT, "valid", "_invalid_realworld")
os.makedirs(TRAIN_INVALID_DIR, exist_ok=True)
os.makedirs(VAL_INVALID_DIR,   exist_ok=True)

print("=" * 60)
print("CornEye — Invalid Class Dataset Downloader")
print("=" * 60)
print(f"Dataset root : {DATASET_ROOT}")
print(f"Max per cat  : {MAX_PER_CAT}")
print(f"Categories   : {CATEGORIES}")
print(f"Train output : {TRAIN_INVALID_DIR}")
print(f"Val output   : {VAL_INVALID_DIR}")
print()

# ── Check fiftyone is installed ───────────────────────────────────────────────
try:
    import fiftyone as fo
    import fiftyone.zoo as foz
    from PIL import Image
except ImportError:
    print("ERROR: Required packages not installed.")
    print("Run:  pip install fiftyone Pillow")
    sys.exit(1)

# ── Download helper ───────────────────────────────────────────────────────────
def download_split(split: str, output_dir: str):
    """Download MAX_PER_CAT images for each category from the given split."""
    print(f"\n{'─'*40}")
    print(f"Downloading split: {split}")
    print(f"{'─'*40}")

    total_saved = 0

    for category in CATEGORIES:
        print(f"\n  [{category}] — downloading up to {MAX_PER_CAT} images from '{split}' split...")

        try:
            dataset = foz.load_zoo_dataset(
                "open-images-v7",
                split=split,
                label_types=["detections"],   # bounding boxes available for most classes
                classes=[category],
                max_samples=MAX_PER_CAT,
                seed=SEED,
                shuffle=True,
            )
        except Exception as e:
            print(f"  WARNING: Could not download '{category}' ({split}): {e}")
            continue

        saved = 0
        for sample in dataset:
            src = sample.filepath
            if not os.path.isfile(src):
                continue

            # Validate image can be opened (skip corrupt files)
            try:
                with Image.open(src) as img:
                    img.verify()
            except Exception:
                continue

            # Build a unique destination filename:
            #   <category_slug>_<split>_<hash>.jpg
            cat_slug = category.lower().replace(" ", "_")
            name     = f"{cat_slug}_{split}_{os.path.basename(src)}"
            dst      = os.path.join(output_dir, name)

            # Convert to JPEG (normalises PNG / WebP / etc.)
            try:
                with Image.open(src) as img:
                    rgb = img.convert("RGB")
                    # Resize to 512×512 max — keeps file sizes manageable while
                    # preserving enough detail for the 224×224 model input
                    rgb.thumbnail((512, 512), Image.LANCZOS)
                    dst_jpg = os.path.splitext(dst)[0] + ".jpg"
                    rgb.save(dst_jpg, "JPEG", quality=90)
                saved += 1
            except Exception as conv_err:
                print(f"    WARN: Could not convert {src}: {conv_err}")
                continue

        print(f"    Saved {saved} images → {output_dir}")
        total_saved += saved

        # Delete the fiftyone dataset entry to free memory/disk cache
        dataset.delete()

    return total_saved


# ── Run download for train and validation splits ──────────────────────────────
train_total = download_split("train",      TRAIN_INVALID_DIR)
val_total   = download_split("validation", VAL_INVALID_DIR)  # Open Images uses 'validation', our folder is 'valid'

print("\n" + "=" * 60)
print("Download complete.")
print(f"  Train invalid images : {train_total}  →  {TRAIN_INVALID_DIR}")
print(f"  Val   invalid images : {val_total}    →  {VAL_INVALID_DIR}")
print()
print("Next steps:")
print("  1. Verify some images look correct:")
print(f"     explorer {TRAIN_INVALID_DIR}")
print()
print("  2. Re-run the model training:")
print("     python scripts/train_model.py")
print()
print("  3. The new _invalid_realworld/ folders are automatically picked up")
print("     as 'Invalid' class samples by collect_paths() in train_model.py")
print("     — no changes to train_model.py required.")
print("=" * 60)
