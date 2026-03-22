"""
CornEye - Corn Leaf Detector Trainer (5-Class, New Model)
==========================================================
Trains a MobileNetV2 corn LEAF DETECTOR — Stage 1 of the two-model pipeline.
This model answers the question: "Is this image a corn leaf?"

It uses the same PlantVillage dataset but includes ALL plant folders:
  - The 4 corn disease folders are labelled as corn (classes 0-3)
  - ALL other plant folders (tomato, potato, grape, mango, etc.) are "Other" (class 4)

The trained model is used by CornLeafDetector.kt in the Android app to validate
the image BEFORE running the more expensive disease classifier.

Classes produced (5):
  0 - Common Rust           ← corn leaf
  1 - Gray Leaf Spot        ← corn leaf
  2 - Healthy               ← corn leaf
  3 - Northern Leaf Blight  ← corn leaf
  4 - Other                 ← NOT a corn leaf

Dataset expected at:
    C:\\Users\\Admin\\Downloads\\PlantsLeafs\\Full\\
        train/
            Corn_(maize)___Common_rust_/
            Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot/
            Corn_(maize)___healthy/
            Corn_(maize)___Northern_Leaf_Blight/
            <other plant folders> ...
        val/     (same structure)

Output:
  corn_leaf_detector.tflite   →  copied to mobile/app/src/main/assets/
  leaf_labels.txt             →  copied to mobile/app/src/main/assets/

Two-model pipeline:
  corn_leaf_detector.tflite  (this script)             ← Stage 1: is it a corn leaf?
  corn_disease_model.tflite  (train_model.py)          ← Stage 2: which disease?
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
EPOCHS_HEAD   = 15   # Increased from 10
EPOCHS_FINE   = 12   # Increased from 8
SEED          = 42
MODELS_DIR    = os.path.join(os.path.dirname(__file__), "..", "models")
OUTPUT_MODEL  = os.path.join(MODELS_DIR, "corn_leaf_detector.tflite")
OUTPUT_LABELS = os.path.join(MODELS_DIR, "leaf_labels.txt")

CORN_FOLDER_MAP = {
    "Corn_(maize)___Common_rust_"                        : "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot" : "Gray Leaf Spot",
    "Corn_(maize)___healthy"                             : "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight"                : "Northern Leaf Blight",
}

# 5 classes: 4 corn disease types + 1 "Other" for everything else
CLASSES     = sorted(CORN_FOLDER_MAP.values()) + ["Other"]
NUM_CLASSES = len(CLASSES)   # 5
OTHER_IDX   = CLASSES.index("Other")  # 4
print(f"Classes ({NUM_CLASSES}): {CLASSES}")

# ── Collect image paths ───────────────────────────────────────────────────────
def collect_paths(split_dir):
    corn_paths  = []
    other_paths = []
    for folder in os.listdir(split_dir):
        folder_path = os.path.join(split_dir, folder)
        if not os.path.isdir(folder_path):
            continue
        images = [
            os.path.join(folder_path, f)
            for f in os.listdir(folder_path)
            if f.lower().endswith((".jpg", ".jpeg", ".png", ".JPG", ".JPEG"))
        ]
        if folder in CORN_FOLDER_MAP:
            label_idx = CLASSES.index(CORN_FOLDER_MAP[folder])
            corn_paths.extend((p, label_idx) for p in images)
        else:
            other_paths.extend((p, OTHER_IDX) for p in images)
    return corn_paths, other_paths

print(f"\nLoading train split: {TRAIN_DIR}")
train_corn, train_other = collect_paths(TRAIN_DIR)
print(f"  corn={len(train_corn)}  other={len(train_other)}")

print(f"Loading val split:   {VAL_DIR}")
val_corn, val_other = collect_paths(VAL_DIR)
print(f"  corn={len(val_corn)}  other={len(val_other)}")

# Balance Other to match corn count so the model doesn't learn "always Other"
random.seed(SEED)
random.shuffle(train_other); train_other = train_other[:len(train_corn)]
random.shuffle(val_other);   val_other   = val_other[:len(val_corn)]

train_all = train_corn + train_other
val_all   = val_corn   + val_other
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
    """Heavy augmentation to bridge PlantVillage → real phone photos."""
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_flip_up_down(image)
    k = tf.random.uniform(shape=[], minval=0, maxval=4, dtype=tf.int32)
    image = tf.image.rot90(image, k)
    crop_size = tf.random.uniform(shape=[], minval=int(IMG_SIZE * 0.6),
                                  maxval=IMG_SIZE, dtype=tf.int32)
    image = tf.image.random_crop(image, [crop_size, crop_size, 3])
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    # Add Gaussian noise
    noise = tf.random.normal(shape=tf.shape(image), mean=0.0, stddev=0.05)
    image = tf.add(image, noise)
    image = tf.image.random_brightness(image, max_delta=0.4)
    image = tf.image.random_contrast(image, lower=0.6, upper=1.4)
    image = tf.image.random_saturation(image, lower=0.5, upper=1.5)
    image = tf.image.random_hue(image, max_delta=0.08)
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
print("\nBuilding MobileNetV2 leaf detector model...")
base_model = tf.keras.applications.MobileNetV2(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights="imagenet",
)
base_model.trainable = False

inputs  = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
x       = base_model(inputs, training=False)
x       = tf.keras.layers.GlobalAveragePooling2D()(x)
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

# ── Phase 1: train head ───────────────────────────────────────────────────────
print(f"\nPhase 1 — training classifier head ({EPOCHS_HEAD} epochs)...")
model.fit(ds_train, epochs=EPOCHS_HEAD, validation_data=ds_val)

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
model.fit(ds_train, epochs=EPOCHS_FINE, validation_data=ds_val)

val_loss, val_acc = model.evaluate(ds_val)
print(f"\nFinal validation accuracy: {val_acc * 100:.1f}%")

# ── Export TFLite ─────────────────────────────────────────────────────────────
print("\nConverting to TFLite (float32, no quantization)...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open(OUTPUT_MODEL, "wb") as f:
    f.write(tflite_model)
print(f"Saved: {OUTPUT_MODEL}  ({os.path.getsize(OUTPUT_MODEL) // 1024} KB)")

with open(OUTPUT_LABELS, "w") as f:
    for label in CLASSES:
        f.write(label + "\n")
print(f"Saved: {OUTPUT_LABELS}")

# ── Auto-copy to assets ───────────────────────────────────────────────────────
import shutil
assets_dir = os.path.join(os.path.dirname(__file__), "..", "mobile", "app", "src", "main", "assets")
if os.path.isdir(assets_dir):
    shutil.copy(OUTPUT_MODEL,  os.path.join(assets_dir, "corn_leaf_detector.tflite"))
    shutil.copy(OUTPUT_LABELS, os.path.join(assets_dir, "leaf_labels.txt"))
    print(f"\n✅ Auto-copied both files to {assets_dir}/")
else:
    print(f"\n✅ Done! Copy manually to mobile/app/src/main/assets/")

print("\nClass order:")
for i, c in enumerate(CLASSES):
    print(f"  {i} - {c}")
