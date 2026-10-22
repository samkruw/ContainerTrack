# Teilplan C — Terminal Dark und Auslieferung

Master: [V1.3-Plan](2026-10-22-containertrack-v1-3.md), Teilpläne A/B liefern die hier konsumierten Fach-/Sync-/Kontozustände.

## Task 1: Echte Auftragsliste und drei Hauptziele

**Files:** `main/Theme.kt`, `main/MainActivity.kt`, `main/JobScreens.kt`, `main/TrackViewModel.kt`; neu `main/OrderListScreen.kt`, `unit/OrderFiltersTest.kt`; ändern `androidTest/NavigationTest.kt`.

**Interfaces:** `enum class OrderFilter { ACTIVE, NEEDS_FILL, UPLOAD_ERROR, ALL }`; `fun matchesOrder(batch: JobWithContainers, query: String, filter: OrderFilter, sync: UploadJobEntity?): Boolean`. ViewModel speichert Suchbegriff/Filter in SavedStateHandle; `OrderListScreen` konsumiert Jobs, Synczustände, reale Verbindung und gespeichertes `now` sowie Callbacks für neuen Job, Aufnahme, Details und Foto-Share.

- [ ] Tests zuerst: ACTIVE als Default, KT/Order/Return-Suche mit Gross-/Kleinschreibung, NEEDS_FILL auf tatsächlichen Slot 6, UPLOAD_ERROR nur auf echte Queuefehler, ALL findet Archiv. Navigation zeigt Aufträge/Galerie/Profil und Plus öffnet vorhandenes Delivery-/Return-Setup. Rückkehr aus Details erhält Suche/Filter.
- [ ] Rot verifizieren; Farben auf Graphit/Schwarz und Lime umstellen, Kontrast messen, Touchziele >=48dp. GlassSurface bleibt eine zurückhaltende zentrale Fläche ohne Glow. Liste zeigt echte Container-/Foto-Counts, Workflow- und Syncstatus und denselben Reminderzeitpunkt wie Details.
- [ ] Job-Tap öffnet schliessbares Bottom Sheet: Fotos aufnehmen, Auftragsdetails, Teilen. Fehler-/Offlinezustände erklären konkrete Blocker; keine erfundenen Uploadprozente.
- [ ] Kleine Breite/grosse Schrift/lange Kennungen und 10 Container prüfen; Tests grün. Commit `feat: introduce terminal dark orders and three-tab navigation`.

## Task 2: Aufklappbare Container, spätes Befüllt und Exportzugang

**Files:** `main/JobScreens.kt`, `main/CaptureScreens.kt`, `main/ExportScreens.kt`; ändern `androidTest/TwoPhaseDeliveryTest.kt`, `androidTest/ExportScreenTest.kt`, `androidTest/NavigationTest.kt`.

**Interfaces:** Vorhandene `JobScreen`/`PhotoScreen`-Callbacks; neue Details konsumieren `JobWithContainers`, Queuezustand und aktuellen Reminder. Slotauswahl übergibt unverändert containerId/step an bestehende Fotoaufnahme.

- [ ] Tests zuerst: je Container sechs benannte Delivery-Slots, 5/6 als „Befüllt-Foto fehlt“, Foto 6 antippbar nach 1..5, Return 1/1. Auf-/Zuklappen und Scrollen zu Container 10 bei grosser Schrift lassen Aktionen erreichbar. Lange Referenz schneidet keine Pflichtaktion ab.
- [ ] Rot verifizieren; kompakte Containerkarten und echte Miniaturen implementieren. Aufnahme/Ansehen/Ersetzen benutzen bestehende CameraX-/Vorschau-/Bestätigungsmethoden. Keine Kamerabild-/OCR-Unschärfe. Timerwert stammt aus demselben Reminder wie in Liste.
- [ ] „Teilen“ benutzt Foto-Share-Sheet; „Lokaler Export“ bewahrt vollständige ZIP-Prüfung, ZIP teilen und ZIP speichern. Return-Felder/Logger-Gate bleiben erhalten. Tests grün; Commit `feat: show expandable six-slot container documentation`.

## Task 3: Lokale Galerie und echtes Microsoft-Profil

**Files:** neu `main/GalleryScreen.kt`, `main/ProfileScreen.kt`, `unit/GalleryFiltersTest.kt`, `androidTest/GalleryProfileTest.kt`; ändern `main/TrackViewModel.kt`, `main/MainActivity.kt`, `main/ExportScreens.kt`.

**Interfaces:** `GalleryPhoto(photoId: String, jobId: String, reference: String, containerNumber: String, step: Int, localPath: String, uploaderAccountId: String?)`; `fun galleryPhotos(jobs: List<JobWithContainers>, photoUploads: List<UploadPhotoEntity>): List<GalleryPhoto>`. Uploader kommt nur aus einer bestätigten aktuellen UploadPhotoEntity-Version. Galeriefilter und ausgewählte Bild-ID bleiben bei Rücknavigation erhalten. Profil konsumiert reale `MicrosoftAccountProvider`-Identität und öffentliche Konfigurations-/Fehlerzustände.

- [ ] Tests zuerst: Raster enthält nur reale Fotoentitäten; KT-/Containerfilter, führende Null und leere Ergebnisse; Vollbild und Foto-Share referenzieren dasselbe echte Bild. Keine Fake-Fotos fremder Geräte. Ohne Identity kein Name/Admin; fehlende Konfiguration erklärt gesperrte Anmeldung. Logout/Kontowechsel aktualisiert Identität und respektiert Queue-Bindung.
- [ ] Rot verifizieren; lokales Thumbnail-Raster, Vollbild und klarer Herkunftshinweis „Lokal auf diesem Gerät“. Mitarbeiterfilter nur bei tatsächlich vorhandenen zugeordneten Uploaderdaten. Keine erfundene Cloud-/Adminansicht.
- [ ] Profil integriert vorhandene Kamera-/Notification-/Alarm-/Datenschutzoptionen. Auth-/Share-Aktionen sind nur mit realen Voraussetzungen aktiv; keine Passwortfelder oder Fake-Rollen. Tests grün; Commit `feat: add local photo gallery and Microsoft account profile`.

## Task 4: Vollständige Regression, Dokumentation und Debug-Artefakt

**Files:** `app/build.gradle.kts`, `README.md`, `docs/QA_STATUS.md`, `docs/ACCEPTANCE_MATRIX.md`, neu `docs/UPDATE_1_3.md`, `docs/MICROSOFT_SETUP.md`; `androidTest/ScreenshotCaptureTest.kt`, `androidTest/OfflineOcrTest.kt`, `.github/workflows/android.yml`, `tools/preview_screenshots.py`, `tools/test_quality_tools.py`.

- [ ] Screenshot-Fixtures an neue Navigation/Zustände anpassen: Aufträge, Setup Delivery/Return, 1/3/10 Container, 5/6 und 6/6, Kamera-Fallback, Timer, Offline-/Uploadfehler, Galerie, Profil. Nur synthetische Testfixtures; keine Firmenbilder. Keine ungeprüften Goldens erzeugen.
- [ ] Version auf 1.3.0 / code 4 setzen; Microsoft-Setup beschreibt öffentliche IDs, Redirect/Debug-Signatur, nötigen Consent, stabile Teams-IDs, Kontowechsel und begrenzte Netzwerkberechtigungen. README erklärt neuen Timertrigger, Foto-only Teams und getrennten lokalen Export.
- [ ] Gesamte Testsuite ausführen: `./gradlew test lint assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2`; Python-QA-Suite und neuer Merged-/APK-Permission-Audit müssen bestehen. Echte Testzahlen, Fehler, Skips und nicht ausgeführte Firmenprüfungen getrennt dokumentieren.
- [ ] Relevante Emulator-/Migrations-/MSAL-Adapter-/Sharetests über bestehende GitHub-CI grün ausführen; gebündeltes ML-Kit bleibt trotz INTERNET-Berechtigung im Flugmodus testbar. CI muss unerwartete Rechte ablehnen, nicht pauschal INTERNET verbieten. Screenshot-Artefakte herunterladen und tatsächlich ansehen; blockierte Downloads als offene Sichtprüfung ausweisen.
- [ ] Unabhängige Gesamtprüfung durchführen, wichtige Befunde beheben, betroffene Tests erneut laufen lassen. Secret-/Diff-Prüfung vor jedem Push; kleine Commits und bestehender Draft-PR, kein Merge. Finale APK `Container-Track-debug-APK`, Commit und CI-Link berichten.
- [ ] Ohne öffentliche Microsoft-Konfiguration/Consent/Firmenkonto bleibt der reale Login-/Teams-End-to-End-Test ausdrücklich offen; Debug-Erstellung und lokale Fachprüfung trotzdem abschliessen. Keine Testadapter als Produkt-Erfolg anzeigen. Reale Kamera, Standby/Reboot, TalkBack und Unternehmensfreigabe separat erfassen.
