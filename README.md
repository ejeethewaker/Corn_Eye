# CornEye 🌽

**CornEye** is a corn leaf disease detection system built for Filipino farmers. It combines an on-device TensorFlow Lite model, a Firebase Realtime Database backend, an Android mobile app, and a React web admin dashboard.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Disease Classes](#disease-classes)
- [Project Structure](#project-structure)
- [Mobile App (Android)](#mobile-app-android)
- [Web Admin Dashboard](#web-admin-dashboard)
- [ML Model](#ml-model)
- [Firebase Database Schema](#firebase-database-schema)
- [Setup & Running](#setup--running)
- [Tech Stack](#tech-stack)

---

## Overview

CornEye allows farmers to photograph a corn leaf with their Android phone and receive an instant diagnosis — identifying whether the leaf is **healthy**, affected by one of three common diseases, or **not a valid corn leaf**. Scan results, farmer accounts, and notifications are all synced to Firebase Realtime Database, accessible in real-time by an admin web dashboard.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CornEye System                           │
│                                                                 │
│  ┌─────────────────┐          ┌──────────────────────────────┐  │
│  │  Android App    │          │   React Admin Dashboard      │  │
│  │  (Kotlin /      │◄────────►│   (web/)                     │  │
│  │  Jetpack Compose│          │                              │  │
│  │                 │          │  • Login                     │  │
│  │ • Scan Leaf     │          │  • Dashboard & Stats         │  │
│  │ • View Results  │          │  • Farmers / Users List      │  │
│  │ • Scan History  │          │  • Farmer Detail View        │  │
│  │ • Notifications │          │  • Notifications Feed        │  │
│  │ • Subscription  │          │  • Admin Profile             │  │
│  │ • Settings      │          │  • Documentation / Q&A       │  │
│  └────────┬────────┘          └──────────────┬───────────────┘  │
│           │                                  │                  │
│           └────────────┬─────────────────────┘                  │
│                        ▼                                        │
│               Firebase Realtime Database                        │
│                        +                                        │
│               Firebase Authentication                           │
│                        +                                        │
│               Firebase Storage                                  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              TFLite Model (on-device)                    │   │
│  │   Single 5-class classifier · MobileNetV2 · INT8        │   │
│  │   224×224 RGB · ~3.5 MB                                  │   │
│  │                                                          │   │
│  │   Mobile inference pipeline:                             │   │
│  │     1. Green pixel ratio check (fast color gate)         │   │
│  │     2. TFLite inference (5 classes)                      │   │
│  │     3. Reject if Invalid OR confidence <70% OR           │   │
│  │        entropy >0.8                                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Disease Classes

The model classifies corn leaf images into 5 categories (4 corn + 1 rejection class):

| # | Label | Description |
|---|---|---|
| 0 | **Common Rust** | Orange/brown pustules on leaf surfaces |
| 1 | **Gray Leaf Spot** | Rectangular gray/tan lesions |
| 2 | **Healthy** | No disease detected |
| 3 | **Northern Leaf Blight** | Long cigar-shaped gray-green lesions |
| 4 | **Invalid** | Not a corn leaf (out-of-distribution rejection) |

---

## Project Structure

```
CornEye/
├── README.md                       ← This file
├── vercel.json                     ← Web dashboard deployment config
├── firebase.json                   ← Firebase project config (Functions + Hosting)
│
├── functions/                      ← Firebase Cloud Functions (Node.js 20)
│   ├── index.js                    ← sendOtp + verifyOtpAndReset callable functions
│   ├── package.json
│   └── .env                        ← Gmail SMTP credentials (gitignored)
│
├── scripts/                        ← ML training & utility scripts
│   ├── train_model.py              ← 5-phase training pipeline (MobileNetV2, INT8)
│   ├── val_model_accuracy.py       ← Validate model accuracy on dataset
│   ├── test_tflite.py              ← Test TFLite model inference
│   ├── plot_training.py            ← Plot training curves from CSV logs
│   ├── prepare_invalid.py          ← Prepare Invalid class from non-corn folders
│   └── verify_dataset.py           ← Verify dataset structure and counts
│
├── models/                         ← Trained model artifacts
│   ├── corn_disease_keras_model.keras
│   ├── corn_disease_model.tflite   ← INT8 quantized (~3.5 MB)
│   ├── labels.txt                  ← 5 class labels
│   ├── training_phase2a_log.csv    ← Phase 2a training log (head only)
│   └── training_phase2b_log.csv    ← Phase 2b training log (fine-tune)
│
├── mobile/                         ← Android app
│   └── app/
│       ├── build.gradle.kts
│       └── src/main/
│           ├── assets/
│           │   ├── corn_disease_model.tflite
│           │   └── labels.txt
│           └── java/com/corneye/app/
│               ├── MainActivity.kt
│               ├── CornEyeApplication.kt
│               ├── data/
│               │   ├── FirebaseHelper.kt
│               │   └── UserPreferences.kt
│               ├── navigation/
│               │   ├── NavGraph.kt
│               │   └── Screen.kt
│               └── ui/
│                   ├── theme/
│                   │   ├── Color.kt
│                   │   ├── Theme.kt
│                   │   └── Type.kt
│                   └── screens/
│                       ├── SplashScreen.kt
│                       ├── LoginScreen.kt
│                       ├── RegisterScreen.kt
│                       ├── HomeScreen.kt
│                       ├── ScanScreen.kt
│                       ├── CornDiseaseClassifier.kt
│                       ├── CornLeafDetector.kt
│                       ├── AnalyzingScreen.kt
│                       ├── ResultScreen.kt
│                       ├── FullReportScreen.kt
│                       ├── InvalidScanScreen.kt
│                       ├── HistoryScreen.kt
│                       ├── NotificationsScreen.kt
│                       ├── ProfileScreen.kt
│                       ├── EditProfileScreen.kt
│                       ├── ChangePasswordScreen.kt
│                       ├── SettingsScreen.kt
│                       ├── ManageSubscriptionScreen.kt
│                       ├── SubscriptionScreen.kt
│                       ├── SubscriptionSuccessScreen.kt
│                       ├── PaymentScreen.kt
│                       ├── DiseaseListScreen.kt
│                       ├── DiseaseDetailScreen.kt
│                       ├── FAQScreen.kt
│                       ├── PrivacyPolicyScreen.kt
│                       ├── ForgotPasswordScreen.kt
│                       ├── OtpScreen.kt
│                       ├── PasswordResetScreen.kt
│                       ├── PasswordSuccessScreen.kt
│                       ├── SetNewPasswordScreen.kt
│                       └── AccountCreatedScreen.kt
│
└── web/                            ← React admin dashboard
    ├── package.json
    └── src/
        ├── App.js
        ├── firebase.js
        ├── Login.js / Login.css
        ├── Dashboard.js / Dashboard.css
        ├── Users.js / Users.css
        ├── UserProfile.js / UserProfile.css
        ├── Notifications.js / Notifications.css
        ├── Profile.js
        ├── Documentation.js / Documentation.css
        └── DocumentationQuestions.js / DocumentationQuestions.css
```

---

## Mobile App (Android)

### Requirements
- Android Studio Hedgehog or newer
- Android SDK 35 (minSdk 26 / Android 8.0+)
- Kotlin 1.9.24
- Java 17 (compile & JVM target)
- `google-services.json` placed in `mobile/app/`

### Features

| Screen | Description |
|---|---|
| Splash / Login / Register | Authentication with Firebase Auth |
| Home | Overview stats (total scans, diseases, healthy), recent history |
| Scan | CameraX live preview → capture → green pixel check → TFLite inference (5-class) |
| Analyzing | On-device MobileNetV2 inference with confidence score |
| Result | Disease label, confidence percentage, quick actions |
| Full Report | Scanned image, detailed disease info, causes, treatments, prevention |
| Invalid Scan | Graceful handler when image is not a corn leaf |
| History | Paginated scan history list with disease / healthy filter |
| Notifications | Scan result alerts and new-farmer notifications with read/unread indicators |
| Profile | View farmer info and profile photo (Base64 from Firebase) |
| Edit Profile | Update name, contact, profile photo |
| Change Password | Validates current password, enforces strength rules |
| Settings | App settings, navigation to sub-pages |
| Manage Subscription | View current plan (Free/Basic/Premium), payment method, change payment modal |
| Subscription | Plan comparison and upgrade flow |
| Payment | GCash / Maya / Card / COD payment entry |
| Disease List | Browsable catalog of all corn diseases |
| Disease Detail | Per-disease info with risk level, symptoms, treatment |
| FAQ | Frequently asked questions |
| Privacy Policy | In-app privacy policy |
| Forgot / OTP / Reset Password | Full password recovery flow |

### Building

1. Open `mobile/` in Android Studio.
2. Place `google-services.json` in `mobile/app/`.
3. Place `corn_disease_model.tflite` and `labels.txt` in `mobile/app/src/main/assets/` (or run `python scripts/train_model.py` to generate and auto-copy them).
4. Click **Run** or build APK via `Build → Generate Signed Bundle/APK`.

---

## Web Admin Dashboard

### Requirements
- Node.js 18+
- npm 9+

### Features

| Page | Route | Description |
|---|---|---|
| Login | `/` | Admin email/password login with **Forgot Password** OTP flow (Firebase Functions + Gmail) |
| Dashboard | `/dashboard` | Total users, total scans, diseases detected, healthy scans; date filter for trends |
| Users | `/users` | All registered farmers, status badges, profile photos (real-time) |
| User Profile | `/user/:id` | Individual farmer detail — scans, subscription, account deactivation toggle |
| Notifications | `/notifications` | Real-time feed of scan alerts and new-farmer notifications; detail modal with scan photo |
| Profile | `/profile` | Admin profile view and update |

### Running locally

```bash
cd web
npm install
npm start        # opens http://localhost:3000
```

### Production build

```bash
cd web
npm run build
```

### Deploying to Vercel

The repo root contains a `vercel.json` that handles the build and SPA routing automatically.

```bash
npm install -g vercel
vercel --prod      # run from the repo root
```

Vercel will run `cd web && npm install && npm run build`, serve `web/build/`, and rewrite all routes to `index.html` for client-side routing.

---

## ML Model

### 5-Phase Training Pipeline

The model is trained via `scripts/train_model.py` which runs a 5-phase pipeline:

| Phase | Description |
|---|---|
| **Phase 1** — Data Collection | PlantVillage corn subset + **469 real-world field photos** (Gray Leaf Spot, Healthy, Northern Leaf Blight) + non-corn folders as "Invalid" class; heavy augmentation (flip, rotate, crop, HSV jitter, Gaussian noise); oversampling for class balance |
| **Phase 2** — Training | MobileNetV2 (ImageNet pretrained); two-stage: frozen head (40 epochs) → fine-tune top 100 layers (35 epochs, LR 1e-4) |
| **Phase 3** — Evaluation | Confusion matrix, per-class precision/recall/F1; target >95% validation accuracy |
| **Phase 4** — TFLite Conversion | INT8 quantization with 500-image stratified representative dataset; validates quantized accuracy >94%; fallback to float32 if needed |
| **Phase 5** — Mobile UX | Handled by Android app: green pixel check → TFLite inference → reject if Invalid / low confidence / high entropy |

### Training a new model

```bash
pip install tensorflow
python scripts/train_model.py
```

Real-world field photos must be placed in the following structure before training:
```
public/
  Gray Leaf Spot/Gray leaf spot/      ← 204 JPG photos
  Healthy Leaf/healthy leaf/           ← 65 JPG photos
  Northern Leaf Blight/Northern Blight/ ← 200 JPG photos
```
The script automatically merges them with the PlantVillage data (80% train / 20% val split).

Output: `models/corn_disease_model.tflite` and `models/labels.txt` — auto-copied to `mobile/app/src/main/assets/`.

Training logs are saved to `models/training_phase2a_log.csv` and `models/training_phase2b_log.csv`.

### Other scripts

| Script | Purpose |
|---|---|
| `val_model_accuracy.py` | Validate model accuracy on a dataset split |
| `test_tflite.py` | Test TFLite model inference on sample images |
| `plot_training.py` | Plot training/validation curves from CSV logs |
| `prepare_invalid.py` | Prepare Invalid class images from non-corn plant folders |
| `verify_dataset.py` | Verify dataset folder structure and image counts |

### Model specs

| Property | Value |
|---|---|
| Architecture | MobileNetV2 (ImageNet pretrained) |
| Input | 224 × 224 RGB, center-square-crop, normalized [0,1] |
| Output | 5-class softmax |
| Quantization | INT8 (float32 input/output for Android compatibility) |
| Format | TFLite (~3.0 MB) |
| Validation accuracy | **99.43%** (full model) · **99.40%** (INT8 quantized) |
| Classes | Common Rust, Gray Leaf Spot, Healthy, Northern Leaf Blight, Invalid |
| Training data | PlantVillage dataset + 469 real-world field photos (80/20 train/val split) |

---

## Firebase Database Schema

```
/admins/{adminId}/
    fullName              String
    email                 String
    password              String

/otps/{emailKey}/
    otp                   String       6-digit OTP code
    expiresAt             Long         epoch ms (10-minute expiry)

/farmers/{userId}/
    fullname              String
    email_address         String
    status                String       "active" | "inactive"  ← toggled by admin; enforced at login & in-app
    profile_photo_url     String       raw Base64
    farm_location         String
    farm_area             String
    createdAt             Long         epoch ms
    subscription/
        active_plan           String   "Free Plan" | "Basic Plan" | "Premium Plan"
        plan_price            Long     price in PHP
        renewal_date_text     String
        payment_method        String   "GCash" | "Maya" | "Credit / Debit Card" | "Cash on Delivery"
        subscription_status   String   "active" | "cancelled"

/notifications/{notifId}/
    farmer_id             String
    notif_type            String       "scan_disease" | "scan_healthy" | "new_farmer"
    notif_title           String
    notif_message         String
    is_read               Boolean
    timestamp             Long         epoch ms  (new_farmer)
    time_scanned          Long         epoch ms  (scan_*)
    analysis_id           String
    analysis_label        String       disease name | "healthy"
    confidence_score      Float

/analysis_results/{resultId}/
    farmer_id             String
    analysis_id           String
    analysis_label        String
    confidence_score      Float
    time_scanned          Long         epoch ms
    image_url             String       raw Base64 of scanned photo
```

---

## Setup & Running

### Prerequisites

| Tool | Purpose |
|---|---|
| Android Studio | Build and run the mobile app |
| Node.js 18+ | Run the web dashboard |
| Python 3.9+ | Train the TFLite model |
| Firebase project | Backend (Realtime Database + Auth + Storage) |

### Full setup steps

1. **Clone the repo**
   ```bash
   git clone <repo-url>
   cd CornEye
   ```

2. **Firebase**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable **Realtime Database**, **Authentication** (Email/Password), and **Storage**
   - Download `google-services.json` → place in `mobile/app/`
   - Update `web/src/firebase.js` with your project's config

3. **Mobile app**
   ```bash
   # Open mobile/ in Android Studio, then Run on device or emulator
   ```

4. **Web dashboard**
   ```bash
   cd web
   npm install
   npm start
   ```

5. **Firebase Functions** (required for Forgot Password OTP emails)
   - Upgrade Firebase project to **Blaze plan** (pay-as-you-go) — required for outbound network calls
   - Create `functions/.env` with your Gmail App Password:
     ```
     GMAIL_USER=your@gmail.com
     GMAIL_PASS=your-app-password
     ```
   - Deploy:
     ```bash
     cd functions
     npm install
     npx firebase-tools deploy --only functions --project <your-project-id>
     ```

6. **ML model** (optional — pre-built `.tflite` is already in assets)
   ```bash
   pip install tensorflow
   python scripts/train_model.py
   # Output: models/corn_disease_model.tflite → auto-copied to mobile/app/src/main/assets/
   ```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin 1.9.24 · Jetpack Compose (BOM 2024.12.01) · CameraX 1.4.1 · TensorFlow Lite 2.17.0 · ML Kit Subject Segmentation · Coil 2.7 · DataStore · Firebase |
| Web Admin | React 19 · React Router 7 · Firebase JS SDK 12 |
| ML Model | MobileNetV2 · TFLite INT8 quantized · PlantVillage dataset |
| Backend | Firebase Realtime Database · Firebase Auth · Firebase Storage · Firebase Functions v6 (Node.js 20) · Nodemailer 6.9 (Gmail SMTP) (BOM 33.7.0) |
| Build | Gradle 8 (KTS) · AGP 8.7.3 · Android SDK 35 |
| Deployment | Vercel (web dashboard) · Firebase Functions (us-central1) |
