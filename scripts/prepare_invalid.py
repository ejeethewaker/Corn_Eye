"""
prepare_invalid.py — Copies non-corn plant folders into a single "Invalid" folder.

Usage:
    python scripts/prepare_invalid.py

Reads the PlantVillage dataset at:
    C:\Users\Admin\Downloads\PlantsLeafs\Full\{train,val}\

For each split, it:
  1. Identifies all folders that are NOT corn (maize) classes
  2. Copies their images into a single "Invalid/" subfolder
  3. Also copies any images from EXTRA_INVALID_DIR (if it exists) — use this
     to add banana leaves, tropical/wide-leaf plants, and other non-corn green
     plants that the PlantVillage set does NOT cover.
  4. Reports counts per source folder

HOW TO ADD BANANA / TROPICAL LEAF IMAGES FOR BETTER OOD DETECTION:
  1. Download images of banana leaves, palm leaves, grass, and other non-corn
     green plants (e.g. from Kaggle "Banana Leaf Disease" dataset or any
     collection of tropical plant photos).
  2. Place them in:
         C:\Users\Admin\Downloads\PlantsLeafs\ExtraInvalid\
     (no sub-folders needed — all images go directly in that directory)
  3. Run this script; images are automatically copied into both train/ and val/
     Invalid folders (80/20 split).
  4. Re-run train_model.py to retrain with the expanded Invalid class.

  Having diverse "healthy-looking but non-corn" leaves (banana, large tropical
  leaves) in the Invalid class is the root fix for the banana-leaf
  misclassification bug.  The in-app hue/texture heuristics are a stopgap.

This is optional — train_model.py already handles Invalid class collection
internally. Use this script if you want to inspect the Invalid folder manually
or pre-stage the data before training.
"""

import os
import shutil
from collections import defaultdict

BASE_DIR  = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
SPLITS    = ["train", "val"]

# Supplementary folder: images here are split 80/20 into train/val Invalid.
# Add banana leaves, tropical plants, etc. here before retraining.
EXTRA_INVALID_DIR = r"C:\Users\Admin\Downloads\PlantsLeafs\ExtraInvalid"

CORN_FOLDERS = {
    "Corn_(maize)___Common_rust_",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
    "Corn_(maize)___healthy",
    "Corn_(maize)___Northern_Leaf_Blight",
}

EXTENSIONS = {".jpg", ".jpeg", ".png"}


def main():
    for split in SPLITS:
        split_dir = os.path.join(BASE_DIR, split)
        if not os.path.isdir(split_dir):
            print(f"⚠️  Split dir not found: {split_dir}")
            continue

        invalid_dir = os.path.join(split_dir, "Invalid")
        os.makedirs(invalid_dir, exist_ok=True)

        counts = defaultdict(int)
        total = 0

        for folder in sorted(os.listdir(split_dir)):
            folder_path = os.path.join(split_dir, folder)
            if not os.path.isdir(folder_path):
                continue
            if folder in CORN_FOLDERS or folder == "Invalid":
                continue

            for fname in os.listdir(folder_path):
                ext = os.path.splitext(fname)[1].lower()
                if ext not in EXTENSIONS:
                    continue

                src = os.path.join(folder_path, fname)
                # Prefix with source folder to avoid name collisions
                dst_name = f"{folder}__{fname}"
                dst = os.path.join(invalid_dir, dst_name)

                if not os.path.exists(dst):
                    shutil.copy2(src, dst)

                counts[folder] += 1
                total += 1

        print(f"\n{split.upper()} — copied {total} images to {invalid_dir}/")
        for folder, count in sorted(counts.items()):
            print(f"  {folder:55s}: {count}")

    # ── Supplementary non-corn images (banana leaves, tropical plants, etc.) ──
    if not os.path.isdir(EXTRA_INVALID_DIR):
        print(f"\nℹ️  No ExtraInvalid folder found at: {EXTRA_INVALID_DIR}")
        print("   Create it and add banana/tropical leaf images to improve OOD detection.")
        return

    extra_files = [
        f for f in os.listdir(EXTRA_INVALID_DIR)
        if os.path.splitext(f)[1].lower() in EXTENSIONS
    ]
    if not extra_files:
        print(f"\nℹ️  ExtraInvalid folder is empty: {EXTRA_INVALID_DIR}")
        return

    import random
    random.shuffle(extra_files)
    split_idx = int(len(extra_files) * 0.80)
    split_map = {"train": extra_files[:split_idx], "val": extra_files[split_idx:]}

    for split, files in split_map.items():
        split_dir = os.path.join(BASE_DIR, split)
        invalid_dir = os.path.join(split_dir, "Invalid")
        os.makedirs(invalid_dir, exist_ok=True)
        copied = 0
        for fname in files:
            src = os.path.join(EXTRA_INVALID_DIR, fname)
            dst = os.path.join(invalid_dir, f"extra_invalid__{fname}")
            if not os.path.exists(dst):
                shutil.copy2(src, dst)
                copied += 1
        print(f"\n{split.upper()} — copied {copied} extra-invalid images from ExtraInvalid/")


if __name__ == "__main__":
    main()


import os
import shutil
from collections import defaultdict

BASE_DIR  = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
SPLITS    = ["train", "val"]

CORN_FOLDERS = {
    "Corn_(maize)___Common_rust_",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
    "Corn_(maize)___healthy",
    "Corn_(maize)___Northern_Leaf_Blight",
}

EXTENSIONS = {".jpg", ".jpeg", ".png"}


def main():
    for split in SPLITS:
        split_dir = os.path.join(BASE_DIR, split)
        if not os.path.isdir(split_dir):
            print(f"⚠️  Split dir not found: {split_dir}")
            continue

        invalid_dir = os.path.join(split_dir, "Invalid")
        os.makedirs(invalid_dir, exist_ok=True)

        counts = defaultdict(int)
        total = 0

        for folder in sorted(os.listdir(split_dir)):
            folder_path = os.path.join(split_dir, folder)
            if not os.path.isdir(folder_path):
                continue
            if folder in CORN_FOLDERS or folder == "Invalid":
                continue

            for fname in os.listdir(folder_path):
                ext = os.path.splitext(fname)[1].lower()
                if ext not in EXTENSIONS:
                    continue

                src = os.path.join(folder_path, fname)
                # Prefix with source folder to avoid name collisions
                dst_name = f"{folder}__{fname}"
                dst = os.path.join(invalid_dir, dst_name)

                if not os.path.exists(dst):
                    shutil.copy2(src, dst)

                counts[folder] += 1
                total += 1

        print(f"\n{split.upper()} — copied {total} images to {invalid_dir}/")
        for folder, count in sorted(counts.items()):
            print(f"  {folder:55s}: {count}")


if __name__ == "__main__":
    main()
