"""
CornEye - Disease Classifier Trainer (4-Class, First Model)
============================================================
Trains a MobileNetV2 corn DISEASE classifier — Stage 2 of the two-model pipeline.
This is the "first model": it only knows the 4 corn disease classes and has NO
"Other" class. Non-corn validation is handled separately by the leaf detector
(see train_leaf_detector.py).

Dataset expected at:
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\
        train/
            Corn_(maize)___Common_rust_/
            Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot/
            Corn_(maize)___healthy/
            Corn_(maize)___Northern_Leaf_Blight/
            <other plant folders are ignored by this script>
        val/     (same structure)

Classes produced (4):
  0 - Common Rust
  1 - Gray Leaf Spot
  2 - Healthy
  3 - Northern Leaf Blight

Output:
  corn_disease_model.tflite   →  copied to mobile/app/src/main/assets/
  labels.txt                  →  copied to mobile/app/src/main/assets/

Two-model pipeline:
  corn_leaf_detector.tflite  (train_leaf_detector.py)  ← Stage 1: is it a corn leaf?
  corn_disease_model.tflite  (this script)             ← Stage 2: which disease?
"""

import os
import random
import numpy as np
import tensorflow as tf

# ── Config ────────────────────────────────────────────────────────────────────
BASE_DIR      = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
TRAIN_DIR     = os.path.join(BASE_DIR, "train")
VAL_DIR       = os.path.join(BASE_DIR, "val")

IMG_SIZE      = 224
BATCH_SIZE    = 32
EPOCHS_HEAD   = 25
EPOCHS_FINE   = 20
SEED          = 42
MODELS_DIR    = os.path.join(os.path.dirname(__file__), "..", "models")
OUTPUT_MODEL  = os.path.join(MODELS_DIR, "corn_disease_model.tflite")
OUTPUT_LABELS = os.path.join(MODELS_DIR, "labels.txt")
BEST_CKPT     = os.path.join(MODELS_DIR, "best_disease_model.keras")

CORN_FOLDER_MAP = {
    "Corn_(maize)___Common_rust_"                        : "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot" : "Gray Leaf Spot",
    "Corn_(maize)___healthy"                             : "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight"                : "Northern Leaf Blight",
}

# 4 classes only — NO "Other" class in the disease classifier
CLASSES     = sorted(CORN_FOLDER_MAP.values())
NUM_CLASSES = len(CLASSES)   # 4
print(f"Classes ({NUM_CLASSES}): {CLASSES}")

# ── Collect image paths ───────────────────────────────────────────────────────
def collect_paths(split_dir):
    corn_paths = []
    for folder in os.listdir(split_dir):
        folder_path = os.path.join(split_dir, folder)
        if not os.path.isdir(folder_path):
            continue
        if folder not in CORN_FOLDER_MAP:
            continue  # skip non-corn folders — this model doesn't need them
        images = [
            os.path.join(folder_path, f)
            for f in os.listdir(folder_path)
            if f.lower().endswith((".jpg", ".jpeg", ".png", ".JPG", ".JPEG"))
        ]
        label_idx = CLASSES.index(CORN_FOLDER_MAP[folder])
        corn_paths.extend((p, label_idx) for p in images)
    return corn_paths

print(f"\nLoading train split: {TRAIN_DIR}")
train_all = collect_paths(TRAIN_DIR)
print(f"  corn samples: {len(train_all)}")

print(f"Loading val split:   {VAL_DIR}")
val_all = collect_paths(VAL_DIR)
print(f"  corn samples: {len(val_all)}")

random.seed(SEED)
random.shuffle(train_all)
random.shuffle(val_all)

print(f"\nTrain total: {len(train_all)}  Val total: {len(val_all)}")

# ── tf.data pipeline ─────────────────────────────────────────────────────────
def decode_image(path):
    raw   = tf.io.read_file(path)
    image = tf.image.decode_image(raw, channels=3, expand_animations=False)
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    image = tf.cast(image, tf.float32) / 255.0
    return image

def augment(image):
    """Moderate augmentation to bridge PlantVillage → real phone photos."""
    # Spatial
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_flip_up_down(image)
    # Random 90° rotation
    k = tf.random.uniform(shape=[], minval=0, maxval=4, dtype=tf.int32)
    image = tf.image.rot90(image, k)
    # Random crop then resize back (simulates zoom / field-of-view variation)
    crop_size = tf.random.uniform(shape=[], minval=int(IMG_SIZE * 0.75),
                                  maxval=IMG_SIZE, dtype=tf.int32)
    image = tf.image.random_crop(image, [crop_size, crop_size, 3])
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    # Colour jitter (handles indoor/outdoor/different phone cameras)
    image = tf.image.random_brightness(image, max_delta=0.25)
    image = tf.image.random_contrast(image, lower=0.75, upper=1.25)
    image = tf.image.random_saturation(image, lower=0.7, upper=1.3)
    image = tf.image.random_hue(image, max_delta=0.05)
    # Clip to valid range
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
        ds = ds.shuffle(buffer_size=2048, seed=SEED)
    ds = ds.batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)
    return ds

ds_train = make_dataset(train_all, training=True)
ds_val   = make_dataset(val_all,   training=False)

# ── Build model ───────────────────────────────────────────────────────────────
print("\nBuilding MobileNetV2 model...")
base_model = tf.keras.applications.MobileNetV2(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights="imagenet",
)
base_model.trainable = False

inputs  = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
# Rescale [0,1] → [-1,1] to match MobileNetV2 pretrained weight expectations
x       = tf.keras.layers.Rescaling(scale=2.0, offset=-1.0)(inputs)
x       = base_model(x, training=False)
x       = tf.keras.layers.GlobalAveragePooling2D()(x)
x       = tf.keras.layers.BatchNormalization()(x)
x       = tf.keras.layers.Dense(256, activation="relu")(x)
x       = tf.keras.layers.Dropout(0.4)(x)
outputs = tf.keras.layers.Dense(NUM_CLASSES, activation="softmax")(x)
model   = tf.keras.Model(inputs, outputs)

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-3),
    loss="categorical_crossentropy",
    metrics=["accuracy"],
)
model.summary()

# ── Callbacks ─────────────────────────────────────────────────────────────────
ckpt_cb = tf.keras.callbacks.ModelCheckpoint(
    BEST_CKPT, monitor="val_accuracy", save_best_only=True, verbose=1,
)
reduce_lr_cb = tf.keras.callbacks.ReduceLROnPlateau(
    monitor="val_loss", factor=0.5, patience=3, min_lr=1e-6, verbose=1,
)
early_stop_cb = tf.keras.callbacks.EarlyStopping(
    monitor="val_accuracy", patience=7, restore_best_weights=True, verbose=1,
)

# ── Phase 1: train head ───────────────────────────────────────────────────────
print(f"\nPhase 1 — training classifier head ({EPOCHS_HEAD} epochs)...")
model.fit(
    ds_train, epochs=EPOCHS_HEAD, validation_data=ds_val,
    callbacks=[ckpt_cb, reduce_lr_cb, early_stop_cb],
)

# ── Phase 2: fine-tune top 50 layers ─────────────────────────────────────────
print(f"\nPhase 2 — fine-tuning top 50 layers ({EPOCHS_FINE} epochs)...")
base_model.trainable = True
for layer in base_model.layers[:-50]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-5),
    loss="categorical_crossentropy",
    metrics=["accuracy"],
)
model.fit(
    ds_train, epochs=EPOCHS_FINE, validation_data=ds_val,
    callbacks=[ckpt_cb, reduce_lr_cb, early_stop_cb],
)

# Reload the best checkpoint from either phase
if os.path.exists(BEST_CKPT):
    print(f"\nLoading best checkpoint from {BEST_CKPT}...")
    model = tf.keras.models.load_model(BEST_CKPT)

val_loss, val_acc = model.evaluate(ds_val)
print(f"\nFinal validation accuracy: {val_acc * 100:.1f}%")
if val_acc < 0.93:
    print("⚠️  Accuracy below 93% target — consider adding more data or tuning hyperparameters.")
else:
    print("✅  Accuracy meets the ≥93% target.")

# ── Export TFLite ─────────────────────────────────────────────────────────────
# No quantization → pure float32 model.
# This is universally compatible with TFLite 2.9+ on Android.
# Dynamic-range quantization triggers FULLY_CONNECTED op v12 which requires
# TFLite 2.17+ to run — omitting it keeps the model at v4/v5 (runs anywhere).
print("\nConverting to TFLite (float32, no quantization)...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# ── Save Keras model (so you can re-export without retraining) ───────────────
KERAS_MODEL_FILE = os.path.join(MODELS_DIR, "corn_disease_keras_model.keras")
print(f"\nSaving Keras model to {KERAS_MODEL_FILE}...")
try:
    model.save(KERAS_MODEL_FILE)
    print("Keras model saved.")
except Exception as e:
    print(f"Warning: Could not save Keras model: {e}")

# Clean up temporary best-checkpoint file
if os.path.exists(BEST_CKPT) and BEST_CKPT != KERAS_MODEL_FILE:
    os.remove(BEST_CKPT)
    print(f"Cleaned up temporary checkpoint: {BEST_CKPT}")

with open(OUTPUT_MODEL, "wb") as f:
    f.write(tflite_model)
print(f"Saved: {OUTPUT_MODEL}  ({os.path.getsize(OUTPUT_MODEL)//1024} KB)")

with open(OUTPUT_LABELS, "w") as f:
    for label in CLASSES:
        f.write(label + "\n")
print(f"Saved: {OUTPUT_LABELS}")

# ── Auto-copy to assets ───────────────────────────────────────────────────────
import shutil
assets_dir = os.path.join(os.path.dirname(__file__), "..", "mobile", "app", "src", "main", "assets")
if os.path.isdir(assets_dir):
    shutil.copy(OUTPUT_MODEL, os.path.join(assets_dir, "corn_disease_model.tflite"))
    shutil.copy(OUTPUT_LABELS, os.path.join(assets_dir, "labels.txt"))
    print(f"\n✅ Auto-copied both files to {assets_dir}/")
else:
    print(f"\n✅ Done! Copy manually to mobile/app/src/main/assets/")

print("\nClass order:")
for i, c in enumerate(CLASSES):
    print(f"  {i} - {c}")
