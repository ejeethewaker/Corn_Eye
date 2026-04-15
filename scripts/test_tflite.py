"""
test_tflite.py — Runs the TFLite model on test images and prints predictions.

Usage:
    python scripts/test_tflite.py                          # test on val set sample
    python scripts/test_tflite.py path/to/image.jpg        # test single image
    python scripts/test_tflite.py path/to/folder/           # test all images in folder

Loads:
    models/corn_disease_model.tflite
    models/labels.txt

Reports per-image: predicted class, confidence, entropy, and ACCEPT/REJECT decision
using the same thresholds as the Android app.
"""

import os
import sys
import glob
import random
import numpy as np
import tensorflow as tf

MODELS_DIR     = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
TFLITE_PATH    = os.path.join(MODELS_DIR, "corn_disease_model.tflite")
LABELS_PATH    = os.path.join(MODELS_DIR, "labels.txt")

IMG_SIZE       = 224
CONFIDENCE_MIN = 0.70      # must match Android gate
ENTROPY_MAX    = 0.80      # must match Android gate
INVALID_LABEL  = "Invalid"  # OOD class name

# Default: sample from val set
BASE_DIR       = r"C:\Users\Admin\Downloads\PlantsLeafs\Full"
VAL_DIR        = os.path.join(BASE_DIR, "val")
SAMPLE_SIZE    = 20  # images per class when testing val set


def load_model():
    if not os.path.isfile(TFLITE_PATH):
        print(f"❌ Model not found: {TFLITE_PATH}")
        print("   Run train_model.py first.")
        sys.exit(1)

    interpreter = tf.lite.Interpreter(model_path=TFLITE_PATH)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    out = interpreter.get_output_details()[0]
    return interpreter, inp, out


def load_labels():
    if not os.path.isfile(LABELS_PATH):
        print(f"❌ Labels not found: {LABELS_PATH}")
        sys.exit(1)
    with open(LABELS_PATH) as f:
        return [line.strip() for line in f if line.strip()]


def preprocess(path):
    raw = tf.io.read_file(path)
    img = tf.cond(
        tf.io.is_jpeg(raw),
        lambda: tf.image.decode_jpeg(raw, channels=3),
        lambda: tf.image.decode_png(raw, channels=3),
    )
    img.set_shape([None, None, 3])
    img = tf.image.resize(img, [IMG_SIZE, IMG_SIZE])
    img = tf.cast(img, tf.float32) / 255.0
    return np.expand_dims(img.numpy(), axis=0).astype(np.float32)


def entropy(probs):
    """Shannon entropy of probability distribution."""
    probs = np.clip(probs, 1e-10, 1.0)
    return -np.sum(probs * np.log(probs))


def predict(interpreter, inp_detail, out_detail, image_np):
    interpreter.set_tensor(inp_detail['index'], image_np)
    interpreter.invoke()
    return interpreter.get_tensor(out_detail['index'])[0]


def decision(label, confidence, ent):
    """Apply the same OOD gate as the Android app."""
    if label == INVALID_LABEL:
        return "REJECT (Invalid class)"
    if confidence < CONFIDENCE_MIN:
        return f"REJECT (confidence {confidence:.2f} < {CONFIDENCE_MIN})"
    if ent > ENTROPY_MAX:
        return f"REJECT (entropy {ent:.2f} > {ENTROPY_MAX})"
    return "ACCEPT"


def collect_val_sample():
    """Collect a stratified sample from the val set."""
    paths = []
    if not os.path.isdir(VAL_DIR):
        return paths
    for folder in sorted(os.listdir(VAL_DIR)):
        folder_path = os.path.join(VAL_DIR, folder)
        if not os.path.isdir(folder_path):
            continue
        images = [
            os.path.join(folder_path, f)
            for f in os.listdir(folder_path)
            if f.lower().endswith((".jpg", ".jpeg", ".png"))
        ]
        sample = random.sample(images, min(SAMPLE_SIZE, len(images)))
        paths.extend(sample)
    random.shuffle(paths)
    return paths


def main():
    labels = load_labels()
    interpreter, inp_detail, out_detail = load_model()

    print(f"Model: {TFLITE_PATH}")
    print(f"Labels ({len(labels)}): {labels}")
    print(f"Thresholds: confidence >= {CONFIDENCE_MIN}, entropy <= {ENTROPY_MAX}")
    print()

    # Determine input images
    if len(sys.argv) > 1:
        target = sys.argv[1]
        if os.path.isfile(target):
            image_paths = [target]
        elif os.path.isdir(target):
            image_paths = sorted(glob.glob(os.path.join(target, "*.*")))
            image_paths = [p for p in image_paths if p.lower().endswith((".jpg", ".jpeg", ".png"))]
        else:
            print(f"❌ Not found: {target}")
            sys.exit(1)
    else:
        print(f"No input specified — sampling {SAMPLE_SIZE} per class from val set\n")
        image_paths = collect_val_sample()

    if not image_paths:
        print("No images found.")
        return

    # Run predictions
    correct = 0
    total = 0
    accept_count = 0
    reject_count = 0

    print(f"{'File':50s} {'Predicted':20s} {'Conf':>6s} {'Entropy':>8s} {'Decision'}")
    print("-" * 110)

    for path in image_paths:
        try:
            img = preprocess(path)
        except Exception as e:
            print(f"  ⚠️ Failed to load {path}: {e}")
            continue

        probs = predict(interpreter, inp_detail, out_detail, img)
        pred_idx = np.argmax(probs)
        pred_label = labels[pred_idx] if pred_idx < len(labels) else "?"
        conf = probs[pred_idx]
        ent = entropy(probs)
        dec = decision(pred_label, conf, ent)

        fname = os.path.basename(path)
        # Truncate long filenames
        if len(fname) > 48:
            fname = fname[:45] + "..."

        status = "✅" if "ACCEPT" in dec else "❌"
        print(f"  {fname:48s} {pred_label:20s} {conf:6.3f} {ent:8.3f}   {status} {dec}")

        if "ACCEPT" in dec:
            accept_count += 1
        else:
            reject_count += 1
        total += 1

    print(f"\n{'=' * 60}")
    print(f"  Total:    {total}")
    print(f"  Accepted: {accept_count}")
    print(f"  Rejected: {reject_count}")
    if total > 0:
        print(f"  Accept rate: {accept_count / total * 100:.1f}%")


if __name__ == "__main__":
    main()
