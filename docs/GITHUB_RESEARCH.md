# GitHub-Recherche für Container Track Android

Stand: 2026-10-20

## Empfohlen

### android/architecture-templates
https://github.com/android/architecture-templates

- offizieller Android-Starter
- Compose, Room, ViewModel, Navigation, Coroutines/Flow, Tests
- `base` ist single-module
- Empfehlung: Konzepte der Base-Variante übernehmen, keine unnötige Multi-Modul-Struktur

### android/camera-samples
https://github.com/android/camera-samples

- offizieller CameraX-/Camera2-Katalog
- Compose-first
- Fotoaufnahme, ImageAnalysis, Torch, ML-Beispiele, Capability Checks
- Empfehlung: CameraX-Muster übernehmen, kein eigenes Kamera-Framework

### android/architecture-samples
https://github.com/android/architecture-samples

- Compose, Single Activity, ViewModel, Flow, Room, Tests
- Empfehlung: State-/Repository-/Testmuster übernehmen
- keine Fake-Remote-Data-Source in Container Track

### android/compose-samples
https://github.com/android/compose-samples

- reale Compose UI, Navigation, Material 3, UI Tests
- Empfehlung: UI-/Testreferenz

### android/nowinandroid
https://github.com/android/nowinandroid

- produktionsnahe Kotlin/Compose-App
- nützlich für Offline-first und Build-/Testideen
- nicht dessen umfangreiche Modularisierung kopieren

### takahirom/roborazzi
https://github.com/takahirom/roborazzi

- Screenshot Tests
- gut gegen visuelle Regressionen / AI-Slop
- nur wenige wichtige Golden Screens

## Nicht als Default

### adaptech-cz/Tesseract4Android
https://github.com/adaptech-cz/Tesseract4Android

Positiv:
- echtes lokales Tesseract OCR
- offline

Warum nicht V1:
- zweite OCR-Engine wäre zusätzliche Komplexität
- zuerst bundled ML Kit testen
- nur hinzufügen, wenn reale Container-Schilder trotz sauberem ROI mit ML Kit ungenügend erkannt werden

## Bewusst nicht empfohlen

- Firebase
- Supabase
- Retrofit/OkHttp
- GitHub Pages
- WebView
- externe ZIP-Library
- Cloud OCR
- zweite Datenbank
- KMP
- Multi-Modul Clean Architecture
- generisches MVI-Framework
