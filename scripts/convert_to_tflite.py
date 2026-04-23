"""
convert_to_tflite.py
Loads the saved best checkpoint and converts it to TFLite (INT8 quantized).
Run this instead of retraining when the full train script crashed after Phase 3.
"""
import os
import sys
import numpy as np
import tensorflow as tf

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")

MODELS_DIR    = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
ASSETS_DIR    = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                             "mobile", "app", "src", "main", "assets")
BEST_CKPT     = os.path.join(MODELS_DIR, "best_disease_model.keras")
OUTPUT_KERAS  = os.path.join(MODELS_DIR, "corn_disease_keras_model.keras")
OUTPUT_TFLITE = os.path.join(MODELS_DIR, "corn_disease_model.tflite")
OUTPUT_LABELS = os.path.join(MODELS_DIR, "labels.txt")
CLASSES       = ["Common Rust", "Gray Leaf Spot", "Healthy", "Northern Leaf Blight", "Invalid"]
IMG_SIZE      = 224

print(f"Loading checkpoint: {BEST_CKPT}")
model = tf.keras.models.load_model(BEST_CKPT)
print("Model loaded OK.")

# Save full Keras model
print(f"Saving Keras model: {OUTPUT_KERAS}")
model.save(OUTPUT_KERAS)
print("  Saved.")

# Convert to TFLite INT8
print("Converting to TFLite with INT8 quantization...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
    tf.lite.OpsSet.TFLITE_BUILTINS,
]
converter.inference_input_type  = tf.float32
converter.inference_output_type = tf.float32

try:
    tflite_model = converter.convert()
    print(f"INT8 model size: {len(tflite_model) // 1024} KB")
except Exception as e:
    print(f"INT8 failed ({e}), falling back to float32...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    print(f"Float32 model size: {len(tflite_model) // 1024} KB")

# Save TFLite
with open(OUTPUT_TFLITE, "wb") as f:
    f.write(tflite_model)
print(f"Saved: {OUTPUT_TFLITE}")

# Save labels
with open(OUTPUT_LABELS, "w") as f:
    for label in CLASSES:
        f.write(label + "\n")
print(f"Saved: {OUTPUT_LABELS}")

# Copy to Android assets
import shutil
if os.path.isdir(ASSETS_DIR):
    shutil.copy(OUTPUT_TFLITE, os.path.join(ASSETS_DIR, "corn_disease_model.tflite"))
    shutil.copy(OUTPUT_LABELS, os.path.join(ASSETS_DIR, "labels.txt"))
    print(f"Copied to Android assets: {ASSETS_DIR}/")
else:
    print(f"Assets dir not found. Copy manually:\n  {OUTPUT_TFLITE}\n  {OUTPUT_LABELS}")

print("\nDone.")
