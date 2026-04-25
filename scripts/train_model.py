"""
CornEye — Single-Model Disease Classifier (5-Class with Invalid)
=================================================================
Follows the 5-phase pipeline:

  Phase 1 — Data collection & augmentation
    • PlantVillage corn subset (2,000+ images/class)
    • 5th "Invalid" class from all non-corn plant folders (OOD detection)
    • Heavy augmentation: flip, rotate, crop, HSV jitter, Gaussian noise
    • Class balancing via oversampling the minority classes

  Phase 2 — Architecture & training
    • Transfer learning: MobileNetV2 (ImageNet pretrained)
    • Input: 224×224 RGB, normalized [0,1]
    • Two-stage: frozen head (Phase 2a) → fine-tune top layers at LR 1e-4 (Phase 2b)
    • Fresh callbacks per phase to avoid state carryover

  Phase 3 — Evaluation & validation
    • Confusion matrix with per-class precision/recall/F1
    • Target: >95% validation accuracy

  Phase 4 — TFLite conversion & optimization
    • INT8 quantization (representative dataset, 500 images) — keep >94%
    • Validates quantized accuracy on a 500-image sample
    • Outputs corn_disease_model.tflite

  Phase 5 — Mobile UX
    • Handled by the Android app:
      1. Green pixel ratio check (fast color gate)
      2. TFLite inference (this model)
      3. If predicted class == "Invalid" OR confidence <70% OR entropy >0.8 → reject

Dataset expected at:
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\
        train/
            Corn_(maize)___Common_rust_/
            Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot/
            Corn_(maize)___healthy/
            Corn_(maize)___Northern_Leaf_Blight/
            <all other plant folders → "Invalid" class>
        val/     (same structure)

Classes (5):
  0 - Common Rust
  1 - Gray Leaf Spot
  2 - Healthy
  3 - Northern Leaf Blight
  4 - Invalid

Output:
  corn_disease_model.tflite   →  auto-copied to mobile/app/src/main/assets/
  labels.txt                  →  auto-copied to mobile/app/src/main/assets/
  training_phase2a_log.csv   →  saved in models/ for diagnosis
  training_phase2b_log.csv   →  saved in models/ for diagnosis
"""

import os
import sys
import random
import shutil
import numpy as np
import tensorflow as tf
from collections import Counter

# Force UTF-8 output on Windows (fixes UnicodeEncodeError for emoji characters)
if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")

# ══════════════════════════════════════════════════════════════════════════════
# Config
# ══════════════════════════════════════════════════════════════════════════════
BASE_DIR       = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
TRAIN_DIR      = os.path.join(BASE_DIR, "train")
VAL_DIR        = os.path.join(BASE_DIR, "val")

IMG_SIZE       = 224
BATCH_SIZE     = 32
EPOCHS_HEAD    = 40          # Phase 2a: frozen backbone, train head only
EPOCHS_FINE    = 35          # Phase 2b: fine-tune top layers
SEED           = 42
MODELS_DIR     = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
ASSETS_DIR     = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                              "mobile", "app", "src", "main", "assets")
OUTPUT_TFLITE  = os.path.join(MODELS_DIR, "corn_disease_model.tflite")
OUTPUT_LABELS  = os.path.join(MODELS_DIR, "labels.txt")
OUTPUT_KERAS   = os.path.join(MODELS_DIR, "corn_disease_keras_model.keras")
BEST_CKPT      = os.path.join(MODELS_DIR, "best_disease_model.keras")
TRAINING_LOG_A = os.path.join(MODELS_DIR, "training_phase2a_log.csv")
TRAINING_LOG_B = os.path.join(MODELS_DIR, "training_phase2b_log.csv")

TARGET_VAL_ACC   = 0.95      # Phase 3: target >95% validation accuracy
TARGET_QUANT_ACC = 0.94      # Phase 4: quantized model must keep >94%
QUANT_VAL_SAMPLE = 500       # Number of images for quantized accuracy check
REP_DATASET_SIZE = 500       # Representative images for INT8 calibration
# Keep Invalid class at or below corn-class scale so it cannot dominate target size.
# 1.0 means Invalid is capped to the largest corn class count.
INVALID_CAP_RATIO = 1.0

CORN_FOLDER_MAP = {
    "Corn_(maize)___Common_rust_"                        : "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot" : "Gray Leaf Spot",
    "Corn_(maize)___healthy"                             : "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight"                : "Northern Leaf Blight",
}

# ── Real-world phone photos (supplement PlantVillage with field images) ────────
REAL_WORLD_DIR = r"C:\Users\camsy\OneDrive\Desktop\Corn_Eye\public"
REAL_WORLD_FOLDER_MAP = {
    os.path.join("Gray Leaf Spot",      "Gray leaf spot") : "Gray Leaf Spot",
    os.path.join("Healthy Leaf",        "healthy leaf")   : "Healthy",
    os.path.join("Northern Leaf Blight","Northern Blight"): "Northern Leaf Blight",
}
REAL_WORLD_VAL_SPLIT = 0.20   # 20% of real-world images reserved for validation

# 5 classes: 4 corn diseases + Invalid (all non-corn plant folders)
CLASSES     = sorted(CORN_FOLDER_MAP.values()) + ["Invalid"]
NUM_CLASSES = len(CLASSES)   # 5
INVALID_IDX = CLASSES.index("Invalid")

os.makedirs(MODELS_DIR, exist_ok=True)

random.seed(SEED)
np.random.seed(SEED)
tf.random.set_seed(SEED)

print(f"Classes ({NUM_CLASSES}): {CLASSES}")
print(f"Invalid class index: {INVALID_IDX}")

# ══════════════════════════════════════════════════════════════════════════════
# Phase 1 — Data Collection & Augmentation
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("PHASE 1 — Data Collection & Augmentation")
print("=" * 60)

def collect_paths(split_dir):
    """Collect corn disease images AND non-corn images as 'Invalid'."""
    corn_paths    = []
    invalid_paths = []
    for folder in os.listdir(split_dir):
        folder_path = os.path.join(split_dir, folder)
        if not os.path.isdir(folder_path):
            continue
        images = [
            os.path.join(folder_path, f)
            for f in os.listdir(folder_path)
            if f.lower().endswith((".jpg", ".jpeg", ".png"))
        ]
        if folder in CORN_FOLDER_MAP:
            label_idx = CLASSES.index(CORN_FOLDER_MAP[folder])
            corn_paths.extend((p, label_idx) for p in images)
        else:
            # All non-corn plant folders → "Invalid"
            invalid_paths.extend((p, INVALID_IDX) for p in images)
    return corn_paths, invalid_paths

def collect_realworld_paths():
    """Collect real-world phone photos and split 80/20 into train/val.

    Folder structure under REAL_WORLD_DIR:
        Gray Leaf Spot/Gray leaf spot/       → Gray Leaf Spot class
        Healthy Leaf/healthy leaf/           → Healthy class
        Northern Leaf Blight/Northern Blight/→ Northern Leaf Blight class
    """
    train_paths, val_paths = [], []
    rng = random.Random(SEED)
    for rel_folder, class_name in REAL_WORLD_FOLDER_MAP.items():
        folder_path = os.path.join(REAL_WORLD_DIR, rel_folder)
        if not os.path.isdir(folder_path):
            print(f"  ⚠️  Real-world folder not found: {folder_path}")
            continue
        label_idx = CLASSES.index(class_name)
        images = sorted([
            os.path.join(folder_path, f)
            for f in os.listdir(folder_path)
            if f.lower().endswith((".jpg", ".jpeg", ".png"))
        ])
        rng.shuffle(images)
        split = int(len(images) * (1 - REAL_WORLD_VAL_SPLIT))
        for p in images[:split]:
            train_paths.append((p, label_idx))
        for p in images[split:]:
            val_paths.append((p, label_idx))
        print(f"  {class_name:25s}: {len(images)} images → {split} train / {len(images)-split} val")
    return train_paths, val_paths

print(f"\nLoading train split: {TRAIN_DIR}")
train_corn, train_invalid = collect_paths(TRAIN_DIR)
print(f"  corn samples:    {len(train_corn)}")
print(f"  invalid samples: {len(train_invalid)}")

if not train_corn:
    raise RuntimeError(
        f"No corn training images found in {TRAIN_DIR}. "
        f"Check CORN_FOLDER_MAP folder names match your dataset."
    )

# Cap Invalid class so it cannot exceed the largest corn class.
corn_by_class = {}
for _, label in train_corn:
    corn_by_class[label] = corn_by_class.get(label, 0) + 1
max_corn_count = max(corn_by_class.values()) if corn_by_class else 2000
MAX_INVALID = int(max_corn_count * INVALID_CAP_RATIO)
if len(train_invalid) > MAX_INVALID:
    train_invalid = random.sample(train_invalid, MAX_INVALID)
    print(f"  Invalid class capped to {MAX_INVALID} samples ({INVALID_CAP_RATIO:.2f}x largest corn class)")

print(f"Loading val split:   {VAL_DIR}")
val_corn, val_invalid = collect_paths(VAL_DIR)
print(f"  corn samples:    {len(val_corn)}")
print(f"  invalid samples: {len(val_invalid)}")

if not val_corn:
    raise RuntimeError(
        f"No corn validation images found in {VAL_DIR}. "
        f"Check folder names match CORN_FOLDER_MAP."
    )
if not val_invalid:
    print("  ⚠️  No invalid val images found — Invalid class recall cannot be measured.")

# ── Merge real-world phone photos ─────────────────────────────────────────────
print(f"\nLoading real-world photos: {REAL_WORLD_DIR}")
rw_train, rw_val = collect_realworld_paths()
print(f"  Real-world total → train: {len(rw_train)}  val: {len(rw_val)}")
train_corn.extend(rw_train)
val_corn.extend(rw_val)

# Cap val Invalid proportionally
val_corn_counts = {}
for _, label in val_corn:
    val_corn_counts[label] = val_corn_counts.get(label, 0) + 1
max_val_corn = max(val_corn_counts.values()) if val_corn_counts else 500
MAX_VAL_INVALID = int(max_val_corn * INVALID_CAP_RATIO)
if len(val_invalid) > MAX_VAL_INVALID:
    val_invalid = random.sample(val_invalid, MAX_VAL_INVALID)
    print(f"  Invalid val class capped to {MAX_VAL_INVALID} samples ({INVALID_CAP_RATIO:.2f}x largest corn val class)")

train_all = train_corn + train_invalid
val_all   = val_corn + val_invalid

# ── Build stratified representative dataset BEFORE shuffling val_all ──────────
# This way class sampling doesn't depend on shuffle ordering.
REP_PER_CLASS = REP_DATASET_SIZE // NUM_CLASSES  # 100 per class
rep_by_class = {i: [] for i in range(NUM_CLASSES)}
for path, label in val_all:
    if len(rep_by_class[label]) < REP_PER_CLASS:
        rep_by_class[label].append(path)
rep_paths = [p for paths in rep_by_class.values() for p in paths]
random.shuffle(rep_paths)

random.shuffle(train_all)
random.shuffle(val_all)

# ── Class balance via oversampling ────────────────────────────────────────────
train_by_class = {}
for path, label in train_all:
    train_by_class.setdefault(label, []).append((path, label))

class_counts = {k: len(v) for k, v in train_by_class.items()}
corn_target_count = max(class_counts.get(i, 0) for i in range(NUM_CLASSES) if i != INVALID_IDX)

# Safety clamp in case input data changed unexpectedly.
if class_counts.get(INVALID_IDX, 0) > corn_target_count:
    train_by_class[INVALID_IDX] = random.sample(train_by_class[INVALID_IDX], corn_target_count)
    class_counts[INVALID_IDX] = corn_target_count

print("\nOriginal class distribution:")
for idx in sorted(class_counts):
    print(f"  {CLASSES[idx]:25s}: {class_counts[idx]}")

# Oversample minority classes to match the largest CORN class only.
train_balanced = []
for label_idx, samples in train_by_class.items():
    if len(samples) < corn_target_count:
        oversampled = samples + random.choices(samples, k=corn_target_count - len(samples))
        train_balanced.extend(oversampled)
    else:
        train_balanced.extend(samples)

random.shuffle(train_balanced)

balanced_counts = Counter(l for _, l in train_balanced)
print("\nBalanced class distribution:")
for idx in sorted(balanced_counts):
    print(f"  {CLASSES[idx]:25s}: {balanced_counts[idx]}")
print(f"  Target per class           : {corn_target_count} (from largest corn class)")
print(f"\nTrain: {len(train_balanced)}  Val: {len(val_all)}")

# ── tf.data pipeline with heavy augmentation ──────────────────────────────────
def decode_image(path):
    """Decode JPEG/PNG, center-square-crop, then resize to IMG_SIZE×IMG_SIZE.

    Center-square crop matches the Android inference pipeline which also
    center-crops to a square before scaling to 224×224. This ensures training
    and inference see the same framing, eliminating aspect-ratio distortion
    from real-world 9:16 phone photos.
    """
    raw = tf.io.read_file(path)
    image = tf.cond(
        tf.io.is_jpeg(raw),
        lambda: tf.image.decode_jpeg(raw, channels=3),
        lambda: tf.image.decode_png(raw, channels=3),
    )
    image.set_shape([None, None, 3])
    # Center-square crop — removes letterbox padding from non-square images
    shape  = tf.shape(image)
    h, w   = shape[0], shape[1]
    sq     = tf.minimum(h, w)
    off_y  = (h - sq) // 2
    off_x  = (w - sq) // 2
    image  = image[off_y:off_y+sq, off_x:off_x+sq, :]
    image  = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    image  = tf.cast(image, tf.float32) / 255.0
    return image

def augment(image):
    """Heavy augmentation to bridge PlantVillage → real-world phone photos.

    Changes vs original:
    - Crop range widened to 40-100% (simulates far-away and close-up shots)
    - Random zoom-out padding: randomly places the leaf on a synthetic
      background (mean color fill) to simulate partial-frame captures
    - Stronger color jitter (more lighting condition variety)
    - Coarser Gaussian noise (real phone cameras are noisier)
    - Random JPEG quality degradation (simulates phone compression)
    """
    # ── Spatial transforms ──
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_flip_up_down(image)
    # Random 90° rotation
    k = tf.random.uniform(shape=[], minval=0, maxval=4, dtype=tf.int32)
    image = tf.image.rot90(image, k)

    # Random crop 40-100% — wider range teaches model to handle
    # both zoomed-in close-ups and distant whole-plant shots
    crop_frac = tf.random.uniform(shape=[], minval=0.40, maxval=1.0)
    crop_size = tf.cast(tf.cast(IMG_SIZE, tf.float32) * crop_frac, tf.int32)
    crop_size = tf.maximum(crop_size, 32)  # safety floor
    image = tf.image.random_crop(image, [crop_size, crop_size, 3])
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])

    # Random zoom-out: pad image with border and resize back (30% chance)
    # Simulates photos where the leaf is small in the frame
    do_pad = tf.random.uniform(shape=[]) < 0.30
    pad_frac = tf.random.uniform(shape=[], minval=0.05, maxval=0.20)
    pad_px   = tf.cast(tf.cast(IMG_SIZE, tf.float32) * pad_frac, tf.int32)
    padded   = tf.pad(image, [[pad_px, pad_px], [pad_px, pad_px], [0, 0]],
                      mode="REFLECT")
    padded   = tf.image.resize(padded, [IMG_SIZE, IMG_SIZE])
    image    = tf.cond(do_pad, lambda: padded, lambda: image)

    # ── Color / HSV transforms (stronger for lighting variation) ──
    image = tf.image.random_brightness(image, max_delta=0.4)
    image = tf.image.random_contrast(image, lower=0.6, upper=1.5)
    image = tf.image.random_saturation(image, lower=0.5, upper=1.8)
    image = tf.image.random_hue(image, max_delta=0.12)

    # ── Gaussian noise (phone camera sensor noise) ──
    noise = tf.random.normal(shape=tf.shape(image), mean=0.0, stddev=0.05)
    image = image + noise

    # ── Random JPEG quality drop (phone compression artifacts) ──
    do_jpeg  = tf.random.uniform(shape=[]) < 0.40
    img_u8   = tf.cast(tf.clip_by_value(image, 0.0, 1.0) * 255, tf.uint8)
    degraded = tf.cast(tf.image.random_jpeg_quality(img_u8, 60, 95), tf.float32) / 255.0
    image    = tf.cond(do_jpeg, lambda: degraded, lambda: image)

    # ── Random Gaussian blur (simulates phone camera focus variation) ────────
    # 35% chance — forces the model to learn disease morphology from slightly
    # blurry images, not just sharp PlantVillage textures.
    do_blur = tf.random.uniform(shape=[]) < 0.35
    kernel  = tf.constant([[1, 2, 1], [2, 4, 2], [1, 2, 1]], dtype=tf.float32) / 16.0
    kernel  = tf.reshape(kernel, [3, 3, 1, 1])
    kernel  = tf.tile(kernel, [1, 1, 3, 1])
    blurred = tf.nn.depthwise_conv2d(
        tf.expand_dims(image, 0), kernel, strides=[1, 1, 1, 1], padding='SAME'
    )[0]
    image   = tf.cond(do_blur, lambda: blurred, lambda: image)

    # ── CutOut: erase a random rectangle (forces holistic feature learning) ──
    # 40% chance — prevents the model from relying on a single discriminative
    # spot. Without cutout, MobileNetV2-style models latch onto PlantVillage
    # background style rather than disease lesion patterns.
    do_cut   = tf.random.uniform(shape=[]) < 0.40
    cut_frac = tf.random.uniform(shape=[], minval=0.10, maxval=0.35)
    cut_size = tf.cast(tf.cast(IMG_SIZE, tf.float32) * cut_frac, tf.int32)
    max_pos  = IMG_SIZE - cut_size
    cut_y    = tf.minimum(
        tf.random.uniform(shape=[], minval=0, maxval=IMG_SIZE, dtype=tf.int32), max_pos
    )
    cut_x    = tf.minimum(
        tf.random.uniform(shape=[], minval=0, maxval=IMG_SIZE, dtype=tf.int32), max_pos
    )
    yy_grid, xx_grid = tf.meshgrid(tf.range(IMG_SIZE), tf.range(IMG_SIZE), indexing='ij')
    in_box  = tf.logical_and(
        tf.logical_and(yy_grid >= cut_y, yy_grid < cut_y + cut_size),
        tf.logical_and(xx_grid >= cut_x, xx_grid < cut_x + cut_size),
    )
    mask    = tf.cast(tf.logical_not(in_box), tf.float32)[:, :, tf.newaxis]
    mean_col = tf.reduce_mean(image, axis=[0, 1], keepdims=True)
    cutout   = image * mask + mean_col * (1.0 - mask)
    image    = tf.cond(do_cut, lambda: cutout, lambda: image)

    image = tf.clip_by_value(image, 0.0, 1.0)
    return image

def make_dataset(pairs, training=False):
    paths  = [p for p, _ in pairs]
    labels = [l for _, l in pairs]
    ds = tf.data.Dataset.from_tensor_slices((paths, labels))
    ds = ds.map(lambda p, l: (decode_image(p), tf.one_hot(l, NUM_CLASSES)),
                num_parallel_calls=tf.data.AUTOTUNE)
    if training:
        ds = ds.map(lambda x, y: (augment(x), y),
                    num_parallel_calls=tf.data.AUTOTUNE)
        ds = ds.shuffle(buffer_size=4096, seed=SEED)
    ds = ds.batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)
    return ds

ds_train = make_dataset(train_balanced, training=True)
ds_val   = make_dataset(val_all, training=False)

# ══════════════════════════════════════════════════════════════════════════════
# Phase 2 — Architecture & Training
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("PHASE 2 — Architecture & Training")
print("=" * 60)

print("\nBuilding EfficientNetB0 model (ImageNet pretrained)...")
# EfficientNetB0: better domain generalization than MobileNetV2 via compound scaling
# and squeeze-and-excitation blocks. Comparable mobile size (~6 MB TFLite).
base_model = tf.keras.applications.EfficientNetB0(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights="imagenet",
)
base_model.trainable = False

inputs  = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
# EfficientNetB0 expects [0, 255]; our data pipeline outputs [0, 1]
x       = tf.keras.layers.Rescaling(scale=255.0)(inputs)  # [0,1]→[0,255]
x       = base_model(x, training=False)
x       = tf.keras.layers.GlobalAveragePooling2D()(x)
x       = tf.keras.layers.BatchNormalization()(x)
x       = tf.keras.layers.Dense(256, activation="relu")(x)
x       = tf.keras.layers.Dropout(0.5)(x)
x       = tf.keras.layers.Dense(128, activation="relu")(x)
x       = tf.keras.layers.Dropout(0.3)(x)
outputs = tf.keras.layers.Dense(NUM_CLASSES, activation="softmax")(x)
model   = tf.keras.Model(inputs, outputs)

# Label smoothing=0.1 prevents the model from becoming overconfident on
# PlantVillage-style training images, which improves generalization to
# real-world field photos that look different from the training set.
model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-3),
    loss=tf.keras.losses.CategoricalCrossentropy(label_smoothing=0.1),
    metrics=["accuracy"],
)
model.summary()

# ── Phase 2a: Train head (backbone frozen) ────────────────────────────────────
# Fresh callbacks for Phase 2a
ckpt_cb_a = tf.keras.callbacks.ModelCheckpoint(
    BEST_CKPT, monitor="val_accuracy", save_best_only=True, verbose=1,
)
reduce_lr_cb_a = tf.keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss", factor=0.5, patience=3, min_lr=1e-6, verbose=1,
)
early_stop_cb_a = tf.keras.callbacks.EarlyStopping(
    monitor="val_accuracy", patience=8, restore_best_weights=True, verbose=1,
)
csv_logger_a = tf.keras.callbacks.CSVLogger(TRAINING_LOG_A, append=False)

print(f"\nPhase 2a — Training classifier head ({EPOCHS_HEAD} epochs)...")
model.fit(
    ds_train, epochs=EPOCHS_HEAD, validation_data=ds_val,
    callbacks=[ckpt_cb_a, reduce_lr_cb_a, early_stop_cb_a, csv_logger_a],
)

# ── Phase 2b: Fine-tune top layers at low LR ─────────────────────────────────
# Fresh callbacks for Phase 2b — avoids stale patience counters / LR state
ckpt_cb_b = tf.keras.callbacks.ModelCheckpoint(
    BEST_CKPT, monitor="val_accuracy", save_best_only=True, verbose=1,
)
reduce_lr_cb_b = tf.keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss", factor=0.5, patience=4, min_lr=1e-7, verbose=1,
)
early_stop_cb_b = tf.keras.callbacks.EarlyStopping(
    monitor="val_accuracy", patience=10, restore_best_weights=True, verbose=1,
)
csv_logger_b = tf.keras.callbacks.CSVLogger(TRAINING_LOG_B, append=False)

print(f"\nPhase 2b — Fine-tuning top 150 layers ({EPOCHS_FINE} epochs, LR=1e-4)...")
base_model.trainable = True
# Unfreeze more layers (150 vs 100) to give EfficientNetB0 more
# capacity to adapt its SE blocks and depthwise convs to corn disease patterns.
for layer in base_model.layers[:-150]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-4),
    loss=tf.keras.losses.CategoricalCrossentropy(label_smoothing=0.1),
    metrics=["accuracy"],
)
model.fit(
    ds_train, epochs=EPOCHS_FINE, validation_data=ds_val,
    callbacks=[ckpt_cb_b, reduce_lr_cb_b, early_stop_cb_b, csv_logger_b],
)

# Reload best checkpoint
if os.path.exists(BEST_CKPT):
    print(f"\nLoading best checkpoint: {BEST_CKPT}")
    model = tf.keras.models.load_model(BEST_CKPT)

print(f"\nTraining logs saved to: {TRAINING_LOG_A} and {TRAINING_LOG_B}")

# ══════════════════════════════════════════════════════════════════════════════
# Phase 3 — Evaluation & Validation
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("PHASE 3 — Evaluation & Validation")
print("=" * 60)

val_loss, val_acc = model.evaluate(ds_val)
print(f"\nValidation accuracy: {val_acc * 100:.2f}%")

# ── Confusion matrix & per-class metrics ──────────────────────────────────────
# Single pass: collect predictions and labels together to avoid misalignment.
print("\nGenerating confusion matrix...")
y_true, y_pred = [], []
for images, labels in ds_val:
    preds = model(images, training=False).numpy()
    y_true.extend(np.argmax(labels.numpy(), axis=1))
    y_pred.extend(np.argmax(preds, axis=1))
y_true = np.array(y_true)
y_pred = np.array(y_pred)

# Confusion matrix
cm = np.zeros((NUM_CLASSES, NUM_CLASSES), dtype=int)
for t, p in zip(y_true, y_pred):
    cm[t][p] += 1

print(f"\n{'':25s}", end="")
for c in CLASSES:
    print(f"{c:>15s}", end="")
print()
for i, row_label in enumerate(CLASSES):
    print(f"{row_label:25s}", end="")
    for j in range(NUM_CLASSES):
        print(f"{cm[i][j]:15d}", end="")
    print()

# Per-class precision, recall, F1
print(f"\n{'Class':25s} {'Precision':>10s} {'Recall':>10s} {'F1':>10s} {'Support':>10s}")
print("-" * 65)
for i, cls in enumerate(CLASSES):
    tp = cm[i][i]
    fp = sum(cm[j][i] for j in range(NUM_CLASSES)) - tp
    fn = sum(cm[i]) - tp
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall    = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1        = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0
    support   = sum(cm[i])
    print(f"{cls:25s} {precision:10.4f} {recall:10.4f} {f1:10.4f} {support:10d}")

if val_acc >= TARGET_VAL_ACC:
    print(f"\n✅  Validation accuracy {val_acc*100:.2f}% meets >{TARGET_VAL_ACC*100:.0f}% target!")
else:
    print(f"\n⚠️  Validation accuracy {val_acc*100:.2f}% below >{TARGET_VAL_ACC*100:.0f}% target.")
    print("    Consider: more data, stronger augmentation, or more fine-tune epochs.")

# ══════════════════════════════════════════════════════════════════════════════
# Phase 4 — TFLite Conversion & INT8 Quantization
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("PHASE 4 — TFLite Conversion & INT8 Quantization")
print("=" * 60)

# ── Representative dataset for INT8 calibration (stratified) ──────────────────
# rep_by_class / rep_paths were built in Phase 1 before val_all was shuffled.
print(f"\nUsing stratified representative dataset (~{REP_PER_CLASS} per class)...")
print(f"  Collected {len(rep_paths)} calibration images across {NUM_CLASSES} classes")
for i in range(NUM_CLASSES):
    print(f"    {CLASSES[i]:25s}: {len(rep_by_class[i])}")

def representative_dataset():
    for path in rep_paths:
        raw = open(path, "rb").read()
        try:
            img = tf.image.decode_jpeg(raw, channels=3)
        except Exception:
            img = tf.image.decode_png(raw, channels=3)
        img = tf.image.resize(img, [IMG_SIZE, IMG_SIZE])
        img = tf.cast(img, tf.float32) / 255.0
        yield [np.expand_dims(img.numpy(), axis=0).astype(np.float32)]

# ── Convert with INT8 quantization ────────────────────────────────────────────
print("Converting to TFLite with INT8 quantization...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_dataset
# Keep float32 input/output for easier Android integration
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
    tf.lite.OpsSet.TFLITE_BUILTINS,
]
converter.inference_input_type = tf.float32
converter.inference_output_type = tf.float32

try:
    tflite_quant = converter.convert()
    print(f"INT8 quantized model size: {len(tflite_quant) // 1024} KB")
except Exception as e:
    print(f"⚠️  INT8 quantization failed: {e}")
    print("Falling back to float32 (no quantization)...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_quant = converter.convert()
    print(f"Float32 model size: {len(tflite_quant) // 1024} KB")

# ── Validate quantized model accuracy on a sample ─────────────────────────────
print(f"\nValidating quantized model accuracy (sample of {QUANT_VAL_SAMPLE})...")
interpreter = tf.lite.Interpreter(model_content=tflite_quant)
interpreter.allocate_tensors()
inp_detail = interpreter.get_input_details()[0]
out_detail = interpreter.get_output_details()[0]

quant_correct = 0
quant_total   = 0
for images, labels in ds_val:
    for i in range(images.shape[0]):
        img = np.expand_dims(images[i].numpy(), axis=0).astype(np.float32)
        interpreter.set_tensor(inp_detail['index'], img)
        interpreter.invoke()
        pred = interpreter.get_tensor(out_detail['index'])[0]
        if np.argmax(pred) == np.argmax(labels[i].numpy()):
            quant_correct += 1
        quant_total += 1
        if quant_total >= QUANT_VAL_SAMPLE:
            break
    if quant_total >= QUANT_VAL_SAMPLE:
        break

quant_acc = quant_correct / quant_total if quant_total > 0 else 0
print(f"Quantized model accuracy: {quant_acc * 100:.2f}% ({quant_correct}/{quant_total})")

if quant_acc >= TARGET_QUANT_ACC:
    print(f"✅  Quantized accuracy meets >{TARGET_QUANT_ACC*100:.0f}% target!")
else:
    print(f"⚠️  Quantized accuracy {quant_acc*100:.2f}% below >{TARGET_QUANT_ACC*100:.0f}% target.")
    print("    Falling back to float32 model...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_quant = converter.convert()
    print(f"    Float32 model size: {len(tflite_quant) // 1024} KB")

# ══════════════════════════════════════════════════════════════════════════════
# Save outputs
# ══════════════════════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("Saving outputs")
print("=" * 60)

# Save Keras model first
print(f"\nSaving Keras model: {OUTPUT_KERAS}")
keras_saved = False
try:
    model.save(OUTPUT_KERAS)
    keras_saved = True
    print("  Keras model saved.")
except Exception as e:
    print(f"  Warning: Could not save Keras model: {e}")

# Only clean up checkpoint AFTER Keras save succeeds
if keras_saved and os.path.exists(BEST_CKPT) and \
        os.path.abspath(BEST_CKPT) != os.path.abspath(OUTPUT_KERAS):
    os.remove(BEST_CKPT)
    print(f"  Cleaned up checkpoint: {BEST_CKPT}")
elif not keras_saved:
    print(f"  ⚠️  Keeping checkpoint {BEST_CKPT} (Keras save failed)")

# Save TFLite
with open(OUTPUT_TFLITE, "wb") as f:
    f.write(tflite_quant)
print(f"Saved: {OUTPUT_TFLITE}  ({os.path.getsize(OUTPUT_TFLITE) // 1024} KB)")

# Save labels
with open(OUTPUT_LABELS, "w") as f:
    for label in CLASSES:
        f.write(label + "\n")
print(f"Saved: {OUTPUT_LABELS}")

# Auto-copy to Android assets
if os.path.isdir(ASSETS_DIR):
    shutil.copy(OUTPUT_TFLITE, os.path.join(ASSETS_DIR, "corn_disease_model.tflite"))
    shutil.copy(OUTPUT_LABELS, os.path.join(ASSETS_DIR, "labels.txt"))
    print(f"\n✅ Auto-copied to {ASSETS_DIR}/")
else:
    print(f"\n⚠️  Assets dir not found. Copy manually to mobile/app/src/main/assets/")

# ── Summary ───────────────────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("TRAINING COMPLETE — Summary")
print("=" * 60)
print(f"  Architecture   : MobileNetV2 (ImageNet pretrained)")
print(f"  Input          : {IMG_SIZE}×{IMG_SIZE} RGB, center-square-crop, normalized [0,1]")
print(f"  Augmentation   : crop 40-100%, zoom-out pad, JPEG artifacts, strong HSV jitter")
print(f"  Classes        : {NUM_CLASSES} (4 corn + Invalid)")
print(f"  Val accuracy   : {val_acc * 100:.2f}%")
print(f"  Quant accuracy : {quant_acc * 100:.2f}%")
print(f"  Model size     : {os.path.getsize(OUTPUT_TFLITE) // 1024} KB")
print(f"  Training logs  : {TRAINING_LOG_A}")
print(f"                   {TRAINING_LOG_B}")
print(f"\n  Class order (must match labels.txt & Android):")
for i, c in enumerate(CLASSES):
    print(f"    {i} - {c}")
print(f"\n  Mobile pipeline:")
print(f"    1. Green pixel ratio check    (fast, no model)")
print(f"    2. TFLite inference            (this model, 5 classes)")
print(f"    3. Reject if: class=='Invalid' OR confidence<70% OR entropy>0.8")
