"""
plot_training.py — Plots accuracy and loss curves from CSV training logs.

Usage:
    python scripts/plot_training.py

Reads the CSV logs produced by train_model.py:
    models/training_log_phase2a.csv  (head training)
    models/training_log_phase2b.csv  (fine-tuning)

Generates:
    models/training_curves.png
"""

import os
import sys

try:
    import pandas as pd
    import matplotlib.pyplot as plt
except ImportError:
    print("⚠️  Missing dependencies. Run: pip install pandas matplotlib")
    sys.exit(1)

MODELS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "models")
LOG_A      = os.path.join(MODELS_DIR, "training_log_phase2a.csv")
LOG_B      = os.path.join(MODELS_DIR, "training_log_phase2b.csv")
OUTPUT_PNG = os.path.join(MODELS_DIR, "training_curves.png")


def load_log(path, phase_label):
    """Load a CSVLogger output and add a phase column."""
    if not os.path.isfile(path):
        print(f"⚠️  Log file not found: {path}")
        return None
    df = pd.read_csv(path)
    df["phase"] = phase_label
    return df


def main():
    df_a = load_log(LOG_A, "Phase 2a (Head)")
    df_b = load_log(LOG_B, "Phase 2b (Fine-tune)")

    if df_a is None and df_b is None:
        print("No training logs found. Run train_model.py first.")
        return

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    fig.suptitle("CornEye Training Curves", fontsize=14, fontweight="bold")

    # --- Accuracy plot ---
    ax_acc = axes[0]
    ax_acc.set_title("Accuracy")
    ax_acc.set_xlabel("Epoch")
    ax_acc.set_ylabel("Accuracy")
    ax_acc.set_ylim(0, 1.05)
    ax_acc.grid(True, alpha=0.3)

    # --- Loss plot ---
    ax_loss = axes[1]
    ax_loss.set_title("Loss")
    ax_loss.set_xlabel("Epoch")
    ax_loss.set_ylabel("Loss")
    ax_loss.grid(True, alpha=0.3)

    epoch_offset = 0
    for df, color_train, color_val, label in [
        (df_a, "#2196F3", "#90CAF9", "2a"),
        (df_b, "#4CAF50", "#A5D6A7", "2b"),
    ]:
        if df is None:
            continue

        epochs = df["epoch"] + epoch_offset

        if "accuracy" in df.columns:
            ax_acc.plot(epochs, df["accuracy"], color=color_train,
                       label=f"{label} train", linewidth=1.5)
        if "val_accuracy" in df.columns:
            ax_acc.plot(epochs, df["val_accuracy"], color=color_val,
                       label=f"{label} val", linewidth=1.5, linestyle="--")

        if "loss" in df.columns:
            ax_loss.plot(epochs, df["loss"], color=color_train,
                        label=f"{label} train", linewidth=1.5)
        if "val_loss" in df.columns:
            ax_loss.plot(epochs, df["val_loss"], color=color_val,
                        label=f"{label} val", linewidth=1.5, linestyle="--")

        epoch_offset = epochs.iloc[-1] + 1 if len(epochs) > 0 else epoch_offset

    # Add 95% target line
    ax_acc.axhline(y=0.95, color="red", linestyle=":", linewidth=1, alpha=0.7, label="95% target")

    ax_acc.legend(loc="lower right", fontsize=8)
    ax_loss.legend(loc="upper right", fontsize=8)

    plt.tight_layout()
    plt.savefig(OUTPUT_PNG, dpi=150, bbox_inches="tight")
    print(f"✅ Saved: {OUTPUT_PNG}")
    plt.show()


if __name__ == "__main__":
    main()
