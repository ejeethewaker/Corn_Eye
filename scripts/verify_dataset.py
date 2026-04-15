"""
verify_dataset.py — Counts images per class and flags corrupt files.

Usage:
    python scripts/verify_dataset.py

Scans the PlantVillage dataset at:
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\{train,val}\\

For each split it:
  1. Counts images per folder (class)
  2. Identifies corn vs non-corn (Invalid) folders
  3. Attempts to open every image with PIL to detect corrupt/truncated files
  4. Reports a summary with warnings for classes that are too small
"""

import os
import sys
from collections import defaultdict

try:
    from PIL import Image
    Image.MAX_IMAGE_PIXELS = None  # allow large images
except ImportError:
    print("⚠️  Pillow not installed. Run: pip install Pillow")
    sys.exit(1)

BASE_DIR  = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
SPLITS    = ["train", "val"]
EXTENSIONS = {".jpg", ".jpeg", ".png"}

CORN_FOLDERS = {
    "Corn_(maize)___Common_rust_",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
    "Corn_(maize)___healthy",
    "Corn_(maize)___Northern_Leaf_Blight",
}

MIN_IMAGES_WARNING = 500  # warn if any class has fewer than this


def check_image(path):
    """Try to fully load an image. Returns error string or None."""
    try:
        with Image.open(path) as img:
            img.verify()
        # verify() doesn't fully decode; re-open and load pixels
        with Image.open(path) as img:
            img.load()
        return None
    except Exception as e:
        return str(e)


def main():
    total_corrupt = 0

    for split in SPLITS:
        split_dir = os.path.join(BASE_DIR, split)
        if not os.path.isdir(split_dir):
            print(f"⚠️  Split dir not found: {split_dir}")
            continue

        print(f"\n{'=' * 60}")
        print(f"  {split.upper()} SPLIT — {split_dir}")
        print(f"{'=' * 60}")

        folder_counts = {}
        corrupt_files = []
        corn_total = 0
        invalid_total = 0

        for folder in sorted(os.listdir(split_dir)):
            folder_path = os.path.join(split_dir, folder)
            if not os.path.isdir(folder_path):
                continue

            files = [f for f in os.listdir(folder_path)
                     if os.path.splitext(f)[1].lower() in EXTENSIONS]
            count = len(files)
            ftype = "CORN" if folder in CORN_FOLDERS else "Invalid"
            print(f"  Checking {folder} ({count} images, {ftype})...", flush=True)

            for fname in files:
                fpath = os.path.join(folder_path, fname)
                err = check_image(fpath)
                if err:
                    corrupt_files.append((fpath, err))

            folder_counts[folder] = count
            is_corn = folder in CORN_FOLDERS
            if is_corn:
                corn_total += count
            else:
                invalid_total += count

        # Print table
        print(f"\n{'Folder':55s} {'Count':>8s}  {'Type':>10s}")
        print("-" * 80)
        for folder in sorted(folder_counts):
            count = folder_counts[folder]
            ftype = "CORN" if folder in CORN_FOLDERS else "Invalid"
            warn = " ⚠️ LOW" if count < MIN_IMAGES_WARNING else ""
            print(f"  {folder:55s} {count:8d}  {ftype:>10s}{warn}")

        print(f"\n  Total corn images:    {corn_total}")
        print(f"  Total invalid images: {invalid_total}")
        print(f"  Total folders:        {len(folder_counts)}")

        if corrupt_files:
            print(f"\n  ❌ CORRUPT FILES ({len(corrupt_files)}):")
            for fpath, err in corrupt_files:
                print(f"    {fpath}")
                print(f"      → {err}")
            total_corrupt += len(corrupt_files)
        else:
            print(f"\n  ✅ No corrupt files found")

    if total_corrupt > 0:
        print(f"\n⚠️  Total corrupt files across all splits: {total_corrupt}")
        print("   Delete or replace these before training.")
    else:
        print(f"\n✅ Dataset looks clean — ready for training!")


if __name__ == "__main__":
    main()
