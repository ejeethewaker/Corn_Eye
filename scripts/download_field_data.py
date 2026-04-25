"""
CornEye — Real-World Field Photo Downloader
============================================
Downloads two public corn disease datasets with real-world field photos
and merges them into the existing PlantVillage dataset directory:

  Dataset 1: PlantDoc  (GitHub, no auth — 2,569 web-scraped field images)
             https://github.com/pratikkayal/PlantDoc-Dataset

  Dataset 2: Kaggle "corn-or-maize-leaf-disease-dataset" by smaranjitghose
             kaggle datasets download smaranjitghose/corn-or-maize-leaf-disease-dataset

Both datasets have images taken in real-world / field conditions — hands,
soil, multiple leaves, natural lighting — which is exactly what the current
PlantVillage-only model struggles with.

Output:
  New images are copied into SUBFOLDERS under each existing class folder:
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\train\\Corn_(maize)___healthy\\plantdoc\\
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\train\\Corn_(maize)___healthy\\kaggle\\
  This preserves original PlantVillage images and lets you remove the new
  images independently if needed.

Run:
  python scripts/download_field_data.py
"""

import os
import re
import sys
import shutil
import zipfile
import random
import requests

# ── Config ────────────────────────────────────────────────────────────────────
BASE_DIR   = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
TRAIN_DIR  = os.path.join(BASE_DIR, "train")
VAL_DIR    = os.path.join(BASE_DIR, "val")

TEMP_DIR   = r"C:\Users\Admin\Downloads\_corneye_field_tmp"

VAL_SPLIT  = 0.20   # 20% of each new source goes to val/
SEED       = 42

# Destination folder names in the existing dataset
CORN_FOLDERS = {
    "Common Rust"         : "Corn_(maize)___Common_rust_",
    "Gray Leaf Spot"      : "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
    "Healthy"             : "Corn_(maize)___healthy",
    "Northern Leaf Blight": "Corn_(maize)___Northern_Leaf_Blight",
}

# ── PlantDoc class name → our canonical class name ───────────────────────────
PLANTDOC_MAP = {
    "Corn Common rust leaf": "Common Rust",
    "Corn leaf blight"     : "Northern Leaf Blight",
    "Corn Gray leaf spot"  : "Gray Leaf Spot",
    "Corn healthy leaf"    : "Healthy",
}

# ── Kaggle dataset structure: folder name → canonical class name ─────────────
# smaranjitghose/corn-or-maize-leaf-disease-dataset uses these folder names
KAGGLE_MAP = {
    "Corn_(maize)___Common_rust_"                       : "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot": "Gray Leaf Spot",
    "Corn_(maize)___healthy"                            : "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight"               : "Northern Leaf Blight",
    # Some Kaggle uploads use simpler names
    "Common_Rust"    : "Common Rust",
    "Gray_Leaf_Spot" : "Gray Leaf Spot",
    "Healthy"        : "Healthy",
    "Northern_Leaf_Blight": "Northern Leaf Blight",
    "Blight"         : "Northern Leaf Blight",
    "Rust"           : "Common Rust",
    "Gray_leaf_spot" : "Gray Leaf Spot",
}

random.seed(SEED)
os.makedirs(TEMP_DIR, exist_ok=True)


# ── Helpers ───────────────────────────────────────────────────────────────────
def longpath(p: str) -> str:
    """Return path with \\?\ prefix so Windows allows >260-char paths."""
    if os.name == "nt" and not p.startswith("\\\\?\\"):
        return "\\\\?\\" + os.path.abspath(p)
    return p


_WIN_INVALID = re.compile(r'[<>:"/\\|?*\x00-\x1f]')

def sanitize_filename(name: str) -> str:
    """Strip characters that are invalid in Windows filenames."""
    return _WIN_INVALID.sub("_", name)


def extract_zip_longpath(zip_path: str, extract_dir: str):
    """Extract ZIP file using \\?\-prefixed paths to bypass Windows MAX_PATH.
    Also sanitizes filenames that contain characters invalid on Windows (e.g. ?)."""
    abs_dir = os.path.abspath(extract_dir)
    with zipfile.ZipFile(zip_path, "r") as zf:
        members = zf.infolist()
        total = len(members)
        for i, member in enumerate(members, 1):
            # Split into parts and sanitize each filename segment
            parts = member.filename.replace("\\", "/").split("/")
            parts = [sanitize_filename(p) if p else p for p in parts]
            rel = os.path.join(*parts) if parts else ""
            if not rel:
                continue
            target_abs = os.path.join(abs_dir, rel)
            target = longpath(target_abs)
            parent = longpath(os.path.dirname(target_abs))
            if member.filename.endswith("/"):
                os.makedirs(target, exist_ok=True)
            else:
                os.makedirs(parent, exist_ok=True)
                with zf.open(member) as src, open(target, "wb") as dst:
                    shutil.copyfileobj(src, dst)
            if i % 500 == 0 or i == total:
                print(f"  Extracted {i}/{total} files ...", flush=True)


def is_image(name: str) -> bool:
    return name.lower().endswith((".jpg", ".jpeg", ".png", ".bmp", ".webp"))


def copy_images(src_dir: str, class_name: str, tag: str):
    """
    Copy all images from src_dir into train/ and val/ under a tagged subfolder.
    Returns (train_count, val_count).
    """
    dest_folder = CORN_FOLDERS[class_name]
    images = [f for f in os.listdir(src_dir) if is_image(f)]
    if not images:
        return 0, 0

    random.shuffle(images)
    split_idx = max(1, int(len(images) * (1 - VAL_SPLIT)))
    train_imgs = images[:split_idx]
    val_imgs   = images[split_idx:]

    train_dest = os.path.join(TRAIN_DIR, dest_folder, tag)
    val_dest   = os.path.join(VAL_DIR,   dest_folder, tag)
    os.makedirs(train_dest, exist_ok=True)
    os.makedirs(val_dest,   exist_ok=True)

    for fname in train_imgs:
        shutil.copy2(os.path.join(src_dir, fname), os.path.join(train_dest, fname))
    for fname in val_imgs:
        shutil.copy2(os.path.join(src_dir, fname), os.path.join(val_dest, fname))

    return len(train_imgs), len(val_imgs)


# ── Dataset 1: PlantDoc ───────────────────────────────────────────────────────
def download_plantdoc():
    print("\n" + "=" * 60)
    print("DATASET 1 — PlantDoc (GitHub)")
    print("=" * 60)

    zip_url  = "https://github.com/pratikkayal/PlantDoc-Dataset/archive/refs/heads/master.zip"
    zip_path = os.path.join(TEMP_DIR, "plantdoc.zip")
    extract  = os.path.join(TEMP_DIR, "plantdoc")

    if os.path.exists(extract):
        print("  Already extracted, skipping download.")
    else:
        if not os.path.exists(zip_path):
            print(f"  Downloading {zip_url} ...")
            resp = requests.get(zip_url, stream=True, timeout=120)
            resp.raise_for_status()
            total = int(resp.headers.get("content-length", 0))
            downloaded = 0
            last_reported_mb = -1
            with open(zip_path, "wb") as f:
                for chunk in resp.iter_content(chunk_size=65536):
                    f.write(chunk)
                    downloaded += len(chunk)
                    mb = downloaded // (1024 * 1024)
                    if mb != last_reported_mb:
                        last_reported_mb = mb
                        if total:
                            pct = downloaded / total * 100
                            bar_len = 30
                            filled = int(bar_len * downloaded / total)
                            bar = "█" * filled + "░" * (bar_len - filled)
                            total_mb = total // (1024 * 1024)
                            print(f"  [{bar}] {pct:5.1f}%  {mb} / {total_mb} MB", flush=True)
                        else:
                            print(f"  Downloaded {mb} MB ...", flush=True)
        else:
            print("  ZIP already downloaded, extracting...")

        os.makedirs(extract, exist_ok=True)
        print("  Extracting...")
        extract_zip_longpath(zip_path, extract)

    # PlantDoc structure:  PlantDoc-Dataset-master/TRAIN/<class>/  and /TEST/<class>/
    root = os.path.join(extract, "PlantDoc-Dataset-master")
    if not os.path.exists(root):
        # Try alternate name after extraction
        candidates = [d for d in os.listdir(extract) if os.path.isdir(os.path.join(extract, d))]
        if candidates:
            root = os.path.join(extract, candidates[0])
        else:
            print("  ERROR: Could not find PlantDoc root directory.")
            return

    total_train = total_val = 0
    for split_name, dest_split in [("TRAIN", "plantdoc_train"), ("TEST", "plantdoc_test")]:
        split_dir = os.path.join(root, split_name)
        if not os.path.exists(split_dir):
            print(f"  WARNING: {split_dir} not found, skipping.")
            continue
        for class_folder in os.listdir(split_dir):
            class_path = os.path.join(split_dir, class_folder)
            if not os.path.isdir(class_path):
                continue
            canonical = PLANTDOC_MAP.get(class_folder)
            if canonical is None:
                continue  # Non-corn class — skip (already covered by PlantVillage Invalid)
            tr, vl = copy_images(class_path, canonical, "plantdoc")
            print(f"  {class_folder:40s}  → {canonical:25s}  train={tr}  val={vl}")
            total_train += tr
            total_val   += vl

    print(f"\n  PlantDoc total: {total_train} train, {total_val} val images added.")


# ── Dataset 2: Kaggle corn disease ────────────────────────────────────────────
def download_kaggle():
    print("\n" + "=" * 60)
    print("DATASET 2 — Kaggle: smaranjitghose/corn-or-maize-leaf-disease-dataset")
    print("=" * 60)

    dataset_id = "smaranjitghose/corn-or-maize-leaf-disease-dataset"
    zip_path   = os.path.join(TEMP_DIR, "kaggle_corn.zip")
    extract    = os.path.join(TEMP_DIR, "kaggle_corn")

    if os.path.exists(extract):
        print("  Already extracted, skipping download.")
    else:
        try:
            import kaggle
        except ImportError:
            print("  kaggle package not found — skipping. Install with: pip install kaggle")
            return

        if not os.path.exists(zip_path):
            print(f"  Downloading {dataset_id} via Kaggle API ...")
            os.system(f'kaggle datasets download -d "{dataset_id}" -p "{TEMP_DIR}" --quiet')
            # kaggle CLI names the file as the dataset slug
            slug_zip = os.path.join(TEMP_DIR, "corn-or-maize-leaf-disease-dataset.zip")
            if os.path.exists(slug_zip):
                os.rename(slug_zip, zip_path)

        if not os.path.exists(zip_path):
            print("  ERROR: Download failed — check Kaggle credentials in ~/.kaggle/kaggle.json")
            return

        os.makedirs(extract, exist_ok=True)
        print("  Extracting...")
        extract_zip_longpath(zip_path, extract)

    # Walk the extracted directory looking for recognizable class folders
    total_train = total_val = 0
    matched_folders = []
    for dirpath, dirnames, filenames in os.walk(extract):
        folder_name = os.path.basename(dirpath)
        canonical   = KAGGLE_MAP.get(folder_name)
        if canonical is None:
            continue
        images = [f for f in filenames if is_image(f)]
        if not images:
            continue
        matched_folders.append(folder_name)
        tr, vl = copy_images(dirpath, canonical, "kaggle")
        print(f"  {folder_name:50s}  → {canonical:25s}  train={tr}  val={vl}")
        total_train += tr
        total_val   += vl

    if not matched_folders:
        # Dump top-level folders to help diagnose unknown structure
        top = [d for d in os.listdir(extract) if os.path.isdir(os.path.join(extract, d))]
        print(f"  WARNING: No matching class folders found. Top-level: {top}")
        print("  You may need to update KAGGLE_MAP in this script.")
    else:
        print(f"\n  Kaggle total: {total_train} train, {total_val} val images added.")


# ── Summary ───────────────────────────────────────────────────────────────────
def print_summary():
    print("\n" + "=" * 60)
    print("DATASET SUMMARY (all sources combined)")
    print("=" * 60)
    for split_name, split_dir in [("train", TRAIN_DIR), ("val", VAL_DIR)]:
        print(f"\n  {split_name}/")
        for cn, folder in CORN_FOLDERS.items():
            d = os.path.join(split_dir, folder)
            if not os.path.exists(d):
                print(f"    {folder}: MISSING")
                continue
            # Count all images recursively
            total = sum(
                len([f for f in files if is_image(f)])
                for _, _, files in os.walk(d)
            )
            # Count by source tag
            sub_counts = {}
            for sub in os.listdir(d):
                sub_path = os.path.join(d, sub)
                if os.path.isdir(sub_path):
                    n = len([f for f in os.listdir(sub_path) if is_image(f)])
                    if n:
                        sub_counts[sub] = n
                else:
                    sub_counts.setdefault("(root)", 0)
                    if is_image(sub):
                        sub_counts["(root)"] += 1
            tags = "  +  ".join(f"{k}:{v}" for k, v in sub_counts.items())
            print(f"    {folder:55s}  total={total:5d}  [{tags}]")


# ── Main ──────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print("CornEye — Real-World Field Photo Downloader")
    print(f"Temp dir : {TEMP_DIR}")
    print(f"Data dir : {BASE_DIR}")

    download_plantdoc()
    download_kaggle()
    print_summary()

    print("\nDone. Now retrain the model:")
    print("  python scripts/train_model.py")
