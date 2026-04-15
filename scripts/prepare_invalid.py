"""
prepare_invalid.py — Copies non-corn plant folders into a single "Invalid" folder.

Usage:
    python scripts/prepare_invalid.py

Reads the PlantVillage dataset at:
    C:\Users\Admin\Downloads\PlantsLeafs\Full\{train,val}\

For each split, it:
  1. Identifies all folders that are NOT corn (maize) classes
  2. Copies their images into a single "Invalid/" subfolder
  3. Reports counts per source folder

This is optional — train_model.py already handles Invalid class collection
internally. Use this script if you want to inspect the Invalid folder manually
or pre-stage the data before training.
"""

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
