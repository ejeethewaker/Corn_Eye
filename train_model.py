"""
CornEye - TFLite Model Trainer
================================
Trains a MobileNetV2 corn disease classifier using the PlantVillage dataset
from TensorFlow Datasets (no manual download needed).

Classes produced (4):
  0 - Common Rust
  1 - Gray Leaf Spot
  2 - Healthy
  3 - Northern Leaf Blight

Output files:
  corn_disease_model.tflite  ← copy this to  mobile/app/src/main/assets/
  labels.txt                 ← already updated in the assets folder

Requirements:
  pip install tensorflow tensorflow-datasets

Run:
  python train_model.py
"""

import os
import numpy as np
import tensorflow as tf
import tensorflow_datasets as tfds

# ── Config ────────────────────────────────────────────────────────────────────
IMG_SIZE    = 224
BATCH_SIZE  = 32
EPOCHS      = 10
OUTPUT_MODEL = "corn_disease_model.tflite"

# PlantVillage corn class names → our display labels
LABEL_MAP = {
    "Corn_(maize)___Common_rust_"                       : "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot": "Gray Leaf Spot",
    "Corn_(maize)___healthy"                            : "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight"               : "Northern Leaf Blight",
}

# Sorted alphabetically — must match assets/labels.txt
CLASSES = sorted(LABEL_MAP.values())   # ['Common Rust', 'Gray Leaf Spot', 'Healthy', 'Northern Leaf Blight']
NUM_CLASSES = len(CLASSES)
print(f"Classes ({NUM_CLASSES}):", CLASSES)


# ── Load PlantVillage dataset ─────────────────────────────────────────────────
print("\nDownloading PlantVillage dataset (first run may take a few minutes)...")
ds_full, info = tfds.load(
    "plant_village",
    split="train",
    with_info=True,
    as_supervised=True,
    shuffle_files=True,
)

# Map original integer labels to class names using dataset info
original_label_names = info.features["label"].names   # full list of all plant/disease names

# Keep only corn classes we care about
corn_original_indices = {
    original_label_names.index(k): CLASSES.index(v)
    for k, v in LABEL_MAP.items()
    if k in original_label_names
}
print("Corn class index mapping:", corn_original_indices)


def filter_corn(image, label):
    return tf.reduce_any([label == orig for orig in corn_original_indices.keys()])

def remap_label(image, label):
    new_label = tf.constant(0, dtype=tf.int64)
    for orig_idx, new_idx in corn_original_indices.items():
        new_label = tf.where(label == orig_idx, tf.cast(new_idx, tf.int64), new_label)
    return image, new_label

def preprocess(image, label):
    image = tf.cast(image, tf.float32) / 255.0
    image = tf.image.resize(image, [IMG_SIZE, IMG_SIZE])
    return image, tf.one_hot(label, NUM_CLASSES)


# Filter → remap → preprocess
ds_corn = (
    ds_full
    .filter(filter_corn)
    .map(remap_label, num_parallel_calls=tf.data.AUTOTUNE)
    .map(preprocess,  num_parallel_calls=tf.data.AUTOTUNE)
)

# Count total samples so we can split 80/20
total = sum(1 for _ in ds_corn)
print(f"\nTotal corn samples: {total}")
train_size = int(total * 0.8)
val_size   = total - train_size

ds_corn = ds_corn.shuffle(total, seed=42)
ds_train = ds_corn.take(train_size).batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)
ds_val   = ds_corn.skip(train_size).batch(BATCH_SIZE).prefetch(tf.data.AUTOTUNE)
print(f"Train: {train_size}  Val: {val_size}")


# ── Build model (MobileNetV2 transfer learning) ───────────────────────────────
print("\nBuilding model...")
base_model = tf.keras.applications.MobileNetV2(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights="imagenet",
)
base_model.trainable = False   # freeze base during first training phase

inputs  = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
x       = base_model(inputs, training=False)
x       = tf.keras.layers.GlobalAveragePooling2D()(x)
x       = tf.keras.layers.Dropout(0.2)(x)
outputs = tf.keras.layers.Dense(NUM_CLASSES, activation="softmax")(x)
model   = tf.keras.Model(inputs, outputs)

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-3),
    loss="categorical_crossentropy",
    metrics=["accuracy"],
)
model.summary()


# ── Phase 1: train head ───────────────────────────────────────────────────────
print("\nPhase 1 — training classifier head...")
history = model.fit(ds_train, epochs=EPOCHS, validation_data=ds_val)

# ── Phase 2: fine-tune top layers ─────────────────────────────────────────────
print("\nPhase 2 — fine-tuning top 30 layers...")
base_model.trainable = True
for layer in base_model.layers[:-30]:
    layer.trainable = False

model.compile(
    optimizer=tf.keras.optimizers.Adam(1e-5),
    loss="categorical_crossentropy",
    metrics=["accuracy"],
)
model.fit(ds_train, epochs=5, validation_data=ds_val)

val_loss, val_acc = model.evaluate(ds_val)
print(f"\nFinal validation accuracy: {val_acc * 100:.1f}%")


# ── Export to TFLite ──────────────────────────────────────────────────────────
print("\nConverting to TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]   # float16 quantization
tflite_model = converter.convert()

with open(OUTPUT_MODEL, "wb") as f:
    f.write(tflite_model)

size_kb = os.path.getsize(OUTPUT_MODEL) / 1024
print(f"Saved: {OUTPUT_MODEL}  ({size_kb:.0f} KB)")
print(f"\n✅ Done! Copy  {OUTPUT_MODEL}  to:")
print("   mobile/app/src/main/assets/corn_disease_model.tflite")
print("\nThe assets/labels.txt already contains the matching class order:")
for i, c in enumerate(CLASSES):
    print(f"  {i} - {c}")
