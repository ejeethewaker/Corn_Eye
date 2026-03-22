"""
Converts SavedModel(s) to TFLite for Android use.
Run from workspace root:
    python convert_model.py

Two-model pipeline:
  corn_leaf_detector.tflite  ← Stage 1: validates the image is a corn leaf
  corn_disease_model.tflite  ← Stage 2: classifies which corn disease

If you have separate SavedModel directories for each, set both MODEL_DIR
variables below. Otherwise, the disease model directory is used for both
until you train the dedicated leaf detector via train_leaf_detector.py.
"""
import os
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.join(SCRIPT_DIR, "..")

DISEASE_MODEL_DIR  = os.path.join(PROJECT_ROOT, "models")  # Stage 2: directory with .keras model
LEAF_MODEL_DIR     = None                                  # Stage 1: set to your leaf detector SavedModel dir
                                                           # Leave None to skip

OUTPUT_DIR                = os.path.join(PROJECT_ROOT, "mobile", "app", "src", "main", "assets")
DISEASE_OUTPUT_FILE       = os.path.join(OUTPUT_DIR, "corn_disease_model.tflite")
LEAF_DETECTOR_OUTPUT_FILE = os.path.join(OUTPUT_DIR, "corn_leaf_detector.tflite")

try:
    import tensorflow as tf
except ImportError:
    print("ERROR: tensorflow is not installed.")
    print("Run:  pip install tensorflow-cpu==2.14.0")
    sys.exit(1)

print(f"TensorFlow version: {tf.__version__}")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ── Convert disease classifier (Stage 2) ─────────────────────────────────────
keras_path = os.path.join(DISEASE_MODEL_DIR, "corn_disease_keras_model.keras")
if os.path.exists(keras_path):
    print(f"\nLoading disease model from: {keras_path}")
    disease_model = tf.keras.models.load_model(keras_path)
    disease_model.summary()

    print("Converting disease model to TFLite (float32)...")
    converter = tf.lite.TFLiteConverter.from_keras_model(disease_model)
    tflite_model = converter.convert()

    with open(DISEASE_OUTPUT_FILE, "wb") as f:
        f.write(tflite_model)
    size_kb = os.path.getsize(DISEASE_OUTPUT_FILE) / 1024
    print(f"Saved {DISEASE_OUTPUT_FILE}  ({size_kb:.0f} KB)")
else:
    print(f"WARNING: Disease model not found: {keras_path}")

# ── Convert leaf detector (Stage 1) ──────────────────────────────────────────
if LEAF_MODEL_DIR and os.path.isdir(LEAF_MODEL_DIR):
    print(f"\nLoading leaf detector model from: {LEAF_MODEL_DIR}")
    leaf_model = tf.keras.models.load_model(LEAF_MODEL_DIR)

    print("Converting leaf detector to TFLite (float32)...")
    converter = tf.lite.TFLiteConverter.from_keras_model(leaf_model)
    tflite_leaf = converter.convert()

    with open(LEAF_DETECTOR_OUTPUT_FILE, "wb") as f:
        f.write(tflite_leaf)
    size_kb = os.path.getsize(LEAF_DETECTOR_OUTPUT_FILE) / 1024
    print(f"Saved {LEAF_DETECTOR_OUTPUT_FILE}  ({size_kb:.0f} KB)")
else:
    print("\nLeaf detector: LEAF_MODEL_DIR not set — keeping existing corn_leaf_detector.tflite in assets.")
    print("  Train the leaf detector with:  python train_leaf_detector.py")

print("\nDone!")

