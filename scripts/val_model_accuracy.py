import os
import tensorflow as tf
import numpy as np
from PIL import Image
import random

# Configuration
BASE_DIR = r"C:\Users\camsy\Downloads\PlantsLeafs\New Plant Diseases Dataset(Augmented)\New Plant Diseases Dataset(Augmented)"
VAL_DIR = os.path.join(BASE_DIR, "valid")
_MODELS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
KERAS_MODEL_PATH = os.path.join(_MODELS_DIR, "corn_disease_keras_model.keras")
TFLITE_MODEL_PATH = os.path.join(_MODELS_DIR, "corn_disease_model.tflite")
LABELS_PATH = os.path.join(_MODELS_DIR, "labels.txt")
IMG_SIZE = 224

# Folders to test (Kaggle/PlantVillage mapping)
CORN_FOLDERS = {
    "Corn_(maize)___Common_rust_": "Common Rust",
    "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot": "Gray Leaf Spot",
    "Corn_(maize)___healthy": "Healthy",
    "Corn_(maize)___Northern_Leaf_Blight": "Northern Leaf Blight",
}

def load_labels(path):
    with open(path, 'r') as f:
        return [line.strip() for line in f.readlines()]

def load_model():
    """Load the Keras model (preferred) or fall back to TFLite."""
    if os.path.exists(KERAS_MODEL_PATH):
        print(f"Loading Keras model: {KERAS_MODEL_PATH}")
        model = tf.keras.models.load_model(KERAS_MODEL_PATH)
        return ("keras", model)
    elif os.path.exists(TFLITE_MODEL_PATH):
        print(f"Loading TFLite model: {TFLITE_MODEL_PATH}")
        interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
        interpreter.allocate_tensors()
        return ("tflite", interpreter)
    else:
        raise FileNotFoundError("No model found. Run train_model.py first.")

def predict(model_info, img_array):
    """Run inference on a single image (1, 224, 224, 3), normalized to [0,1]."""
    kind, model = model_info
    if kind == "keras":
        return model.predict(img_array, verbose=0)[0]
    else:  # tflite
        inp = model.get_input_details()[0]
        out = model.get_output_details()[0]
        model.set_tensor(inp['index'], img_array)
        model.invoke()
        return model.get_tensor(out['index'])[0]

def test_accuracy():
    model_info = load_model()
    labels = load_labels(LABELS_PATH)
    print(f"Labels: {labels}")

    total_correct = 0
    total_samples = 0
    results_by_class = {label: {"correct": 0, "total": 0} for label in CORN_FOLDERS.values()}

    print("\nStarting Validation on PlantVillage Dataset...")
    for folder_name, true_label in CORN_FOLDERS.items():
        folder_path = os.path.join(VAL_DIR, folder_name)
        if not os.path.exists(folder_path):
            print(f"Warning: Folder {folder_name} not found.")
            continue

        images = [f for f in os.listdir(folder_path) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
        random.seed(42)
        random.shuffle(images)

        # Batch-load all images for this class
        batch_arrays = []
        valid_names = []
        for img_name in images:
            img_path = os.path.join(folder_path, img_name)
            try:
                img = Image.open(img_path).convert('RGB').resize((IMG_SIZE, IMG_SIZE))
                img_array = np.array(img, dtype=np.float32) / 255.0
                batch_arrays.append(img_array)
                valid_names.append(img_name)
            except Exception as e:
                print(f"Error loading {img_name}: {e}")

        if not batch_arrays:
            continue

        batch = np.array(batch_arrays)
        # Keras batch prediction is much faster than per-image
        kind, model = model_info
        if kind == "keras":
            all_outputs = model.predict(batch, batch_size=32, verbose=0)
        else:
            all_outputs = np.array([predict(model_info, batch[i:i+1]) for i in range(len(batch))])

        for i, output_data in enumerate(all_outputs):
            predicted_label = labels[np.argmax(output_data)]
            results_by_class[true_label]["total"] += 1
            total_samples += 1
            if predicted_label == true_label:
                results_by_class[true_label]["correct"] += 1
                total_correct += 1

        class_stats = results_by_class[true_label]
        class_acc = (class_stats["correct"] / class_stats["total"]) * 100
        print(f"  {true_label}: {class_acc:.1f}% ({class_stats['correct']}/{class_stats['total']})")

    print("\n--- Model Accuracy Report (Kaggle Test Set) ---")
    for label, stats in results_by_class.items():
        if stats["total"] > 0:
            acc = (stats["correct"] / stats["total"]) * 100
            print(f"{label:25}: {acc:6.2f}% ({stats['correct']}/{stats['total']})")
        else:
            print(f"{label:25}: No samples found.")

    overall_acc = (total_correct / total_samples) * 100 if total_samples > 0 else 0
    print(f"\nOverall Accuracy: {overall_acc:.2f}%")

if __name__ == "__main__":
    test_accuracy()
