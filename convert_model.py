"""
Converts the MobileNetV2 SavedModel to TFLite for Android use.
Run from workspace root:
    python convert_model.py
"""
import os
import sys

MODEL_DIR  = r"model\model_mobnetv2"
OUTPUT_DIR = r"mobile\app\src\main\assets"
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "corn_disease_model.tflite")

try:
    import tensorflow as tf
except ImportError:
    print("ERROR: tensorflow is not installed.")
    print("Run:  pip install tensorflow-cpu==2.14.0")
    sys.exit(1)

print(f"TensorFlow version: {tf.__version__}")
print(f"Loading model from: {MODEL_DIR}")

model = tf.keras.models.load_model(MODEL_DIR)
model.summary()

print("\nConverting to TFLite (float16 quantisation for best accuracy/size balance)...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]
tflite_model = converter.convert()

os.makedirs(OUTPUT_DIR, exist_ok=True)
with open(OUTPUT_FILE, "wb") as f:
    f.write(tflite_model)

size_kb = os.path.getsize(OUTPUT_FILE) / 1024
print(f"\nSaved {OUTPUT_FILE}  ({size_kb:.0f} KB)")
print("Done! Add corn_model.tflite is now in mobile/app/src/main/assets/")
