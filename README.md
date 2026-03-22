# CornEye 🌽

**CornEye** is an AI-powered corn leaf disease detection system built for Filipino farmers. It combines an on-device TensorFlow Lite model, a Firebase Realtime Database backend, an Android mobile app, and a React web admin dashboard.

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

CornEye allows farmers to photograph a corn leaf with their Android phone, and receive an instant AI diagnosis — identifying whether the leaf is **healthy** or affected by one of three common diseases. Scan results, farmer accounts, and notifications are all synced to Firebase Realtime Database, accessible in real-time by an admin web dashboard.

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
│  │ • Settings      │          └──────────────┬───────────────┘  │
│  └────────┬────────┘                         │                  │
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
│  │               TFLite Models (on-device)                  │   │
│  │   Stage 1: Corn Leaf Detector (5-class)                  │   │
│  │   Stage 2: Disease Classifier · MobileNetV2 · 4 classes  │   │
│  │   224×224 · float32                                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Disease Classes

The model classifies corn leaf images into 4 categories:

| Label | Description |
|---|---|
| **Healthy** | No disease detected |
| **Common Rust** | *Puccinia sorghi* — orange/brown pustules on leaf surfaces |
| **Gray Leaf Spot** | *Cercospora zeae-maydis* — rectangular gray/tan lesions |
| **Northern Leaf Blight** | *Exserohilum turcicum* — long cigar-shaped gray-green lesions |

---

## Project Structure

```
CornEye/
├── README.md                       ← This file
├── vercel.json                     ← Web dashboard deployment config
│
├── scripts/                        ← ML training & utility scripts
│   ├── train_model.py              ← Train MobileNetV2 disease classifier
│   ├── train_leaf_detector.py      ← Train corn leaf detector (Stage 1)
│   ├── convert_model.py            ← Convert Keras → TFLite
│   └── val_model_accuracy.py       ← Validate model accuracy on dataset
│
├── models/                         ← Trained model artifacts
│   ├── corn_disease_keras_model.keras
│   ├── corn_disease_model.tflite
│   ├── corn_leaf_detector.tflite
│   ├── labels.txt
│   └── leaf_labels.txt
│
├── mobile/                         ← Android app
│   └── app/
│       ├── build.gradle.kts
│       └── src/main/
│           ├── assets/
│           │   ├── corn_disease_model.tflite
│           │   ├── corn_leaf_detector.tflite
│           │   ├── labels.txt
│           │   └── leaf_labels.txt
│           └── java/com/corneye/app/
│               ├── data/
│               │   ├── FirebaseHelper.kt
│               │   └── UserPreferences.kt
│               ├── navigation/
│               │   └── Screen.kt
│               └── ui/
│                   ├── theme/
│                   │   └── Color.kt
│                   └── screens/
│                       ├── SplashScreen.kt
│                       ├── LoginScreen.kt
│                       ├── RegisterScreen.kt
│                       ├── HomeScreen.kt
│                       ├── ScanScreen.kt
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
        └── Profile.js
```

---

## Mobile App (Android)

### Requirements
- Android Studio Hedgehog or newer
- Android SDK 35 (minSdk 26 / Android 8.0+)
- Kotlin 1.9
- `google-services.json` placed in `mobile/app/`

### Features

| Screen | Description |
|---|---|
| Splash / Login / Register | Authentication with Firebase Auth |
| Home | Overview stats (total scans, diseases, healthy), recent history |
| Scan | CameraX live preview → capture → two-stage TFLite inference (leaf detection → disease classification) |
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
3. Place `corn_disease_model.tflite` in `mobile/app/src/main/assets/` (or run `python scripts/train_model.py` to generate it).
4. Click **Run** or build APK via `Build → Generate Signed Bundle/APK`.

---

## Web Admin Dashboard

### Requirements
- Node.js 18+
- npm 9+

### Features

| Page | Route | Description |
|---|---|---|
| Login | `/` | Admin email/password login (stored in localStorage) |
| Dashboard | `/dashboard` | Total users, total scans, diseases detected, healthy scans; weekly/monthly trends |
| Users | `/users` | All registered farmers, status badges, profile photos |
| User Profile | `/user/:id` | Individual farmer detail — scans, subscription, contact |
| Notifications | `/notifications` | Real-time feed of all scan alerts and new-farmer notifications; click to mark read |
| Profile | `/profile` | Admin profile view |

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

---

## ML Model

### Training a new model

The model is trained on the **PlantVillage** dataset (corn classes only) using MobileNetV2 transfer learning.

```bash
pip install tensorflow tensorflow-datasets
python scripts/train_model.py
```

Output: `models/corn_disease_model.tflite` — auto-copied to `mobile/app/src/main/assets/`.

### Converting a pre-existing Keras model

If you already have a Keras `.keras` file in `models/`:

```bash
pip install tensorflow
python scripts/convert_model.py
```

This exports a **float32** TFLite model.

### Model specs

| Property | Value |
|---|---|
| Architecture | MobileNetV2 (ImageNet pretrained) |
| Input | 224 × 224 RGB |
| Output | 4-class softmax |
| Quantization | Float32 (no quantization) |
| Format | TFLite |
| Classes | Common Rust, Gray Leaf Spot, Healthy, Northern Leaf Blight |
| Validation accuracy | 98.31% |

---

## Firebase Database Schema

```
/farmers/{userId}/
    name                  String
    email                 String
    contact               String
    address               String
    status                String       "active" | "inactive"
    profile_photo_url     String       raw Base64
    created_at            Long         epoch ms
    subscription/
        active_plan           String   "Free Plan" | "Basic Plan" | "Premium Plan"
        plan_price            Long     price in PHP
        renewal_date_text     String
        payment_method        String   "GCash" | "Maya" | "Credit / Debit Card" | "Cash on Delivery"
        subscription_status   String   "active" | "cancelled"

/notifications/{notifId}/
    farmer_id             String
    notif_type            String       "scan_disease" | "scan_healthy" | "new_farmer"
    title                 String
    description           String
    is_read               Boolean
    timestamp             Long         epoch ms  (new_farmer)
    time_scanned          Long         epoch ms  (scan_*)
    analysis_id           String
    label                 String       "healthy" | disease name
    confidence_score      Float

/scan_results/{resultId}/
    farmer_id             String
    label                 String
    confidence            Float
    is_healthy            Boolean
    time_scanned          Long         epoch ms
    image_url             String
```

---

## Setup & Running

### Prerequisites

| Tool | Purpose |
|---|---|
| Android Studio | Build and run the mobile app |
| Node.js 18+ | Run the web dashboard |
| Python 3.9+ | Train or convert the TFLite model |
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

5. **ML model** (optional — pre-built `.tflite` is already in assets)
   ```bash
   pip install tensorflow tensorflow-datasets
   python scripts/train_model.py
   # Output: models/corn_disease_model.tflite → auto-copied to mobile/app/src/main/assets/
   ```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin · Jetpack Compose · CameraX · TensorFlow Lite · Firebase |
| Web Admin | React 19 · React Router 7 · Firebase JS SDK 12 |
| AI Model | MobileNetV2 · TFLite float32 · PlantVillage dataset |
| Backend | Firebase Realtime Database · Firebase Auth · Firebase Storage |
| Build | Gradle 8 (KTS) · Android SDK 35 |
