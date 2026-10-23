# Container Track — MASTERPROMPT für native Android-App

> Diesen Prompt unverändert an den Coding-Agenten mit GitHub-Zugriff geben.
> Ziel: echte native Android-App, vollständig offline/local-only im Betrieb, ohne GitHub Pages und ohne WebView.

---

## 0. Rolle und Arbeitsmodus

Du bist Lead Android Engineer und übernimmst dieses Repository bis zu einer funktionierenden, getesteten Android-App.

Arbeite milestoneweise und führe jede Aufgabe vollständig zu Ende. Wenn das Repository noch die alte PWA enthält, benutze sie nur als Verhaltensreferenz. Die ausgelieferte App darf kein WebView, keine GitHub-Pages-Abhängigkeit und keinen Laufzeit-Netzwerkbedarf haben.

Nach jedem Milestone antworte nur:

`geändert · tests · ergebnis · offen`

---

# 1. Ponytail Gate

Vor jeder Implementierung:

1. Löschen/vereinfachen vor hinzufügen.
2. Android-/Jetpack-Standard-APIs zuerst.
3. Bibliothek nur nutzen, wenn sie konkret Code, Fehlerfläche oder Wartung reduziert.
4. Keine eigene Abstraktion bauen, wenn Android bereits eine passende API hat.
5. Kein Multi-Modul-Projekt ohne echten aktuellen Grund.
6. Keine Future-Proof-Architektur für hypothetische Features.
7. Keine zweite OCR-Engine, Datenbank, Export-Engine oder Navigation „für später“.
8. Keine Dependency nur wegen Komfort.
9. Keine Wrapper um Wrapper.
10. Jede neue Klasse muss einen aktuellen Use Case bedienen.

Default: ein einziges `:app`-Modul.

---

# 2. Anti-AI-Slop Gate

Verboten:

- Platzhalter-Buttons ohne Aktion
- Fake-Daten in Produktionsscreens
- „Coming soon“
- halbfertige Features
- tote Abstraktionen
- ungenutzte Interfaces
- generische Dashboard-Karten ohne operativen Zweck
- riesige Hero-Flächen
- sinnlose Gradients/Glassmorphism/Neon
- zufällige Icons
- unklare Statusmeldungen
- doppelte Wege zum selben Ziel
- Cloud-/Netzwerk-Code vorsorglich
- Analytics, Tracking, Crash-Cloud, Firebase
- `INTERNET` Permission
- WebView
- GitHub Pages zur Laufzeit
- automatische kostenpflichtige Dienste/API-Aufrufe

Jeder Screen muss eine klare Arbeitsaufgabe erfüllen.

---

# 3. Technische Zielarchitektur

- Kotlin
- Jetpack Compose
- Single Activity
- Navigation Compose
- `minSdk 26` oder höher
- `targetSdk` = aktuell stabile Android-Version
- Gradle Kotlin DSL
- Version Catalog
- ein `:app`-Modul
- Coroutines + Flow
- ViewModel
- Room
- DataStore nur für kleine Einstellungen
- CameraX
- ML Kit Text Recognition als bundled model
- native `ZipOutputStream`
- Storage Access Framework / Android-Datei-APIs für Export
- AlarmManager + BroadcastReceiver + NotificationChannel für Logger-Erinnerung
- keine Netzwerkbibliothek

## Runtime-Offlineregel

Nach Installation muss alles ohne Internet funktionieren:

- Scan/OCR
- Fotos
- Datenbank
- Verlauf
- Timer/Alarm
- TXT-Erzeugung
- ZIP-Export

`AndroidManifest.xml` darf keine `android.permission.INTERNET` enthalten.

---

# 4. GitHub-Referenzen

Nur als Muster, nicht blind kopieren:

1. `android/architecture-templates` — base/single-module
2. `android/architecture-samples` — ViewModel, Flow, Room, Tests
3. `android/camera-samples` — CameraX, Compose, ImageAnalysis, Torch
4. `android/compose-samples` — UI und Tests
5. `android/nowinandroid` — nur punktuell, nicht dessen große Modularisierung kopieren
6. `takahirom/roborazzi` — optional für wenige Screenshot-Golden-Tests
7. `adaptech-cz/Tesseract4Android` — nicht in V1; nur späterer Fallback bei nachgewiesen ungenügendem ML Kit

---

# 5. App-ID

App-Name: `Container Track`

Vorgeschlagene Application ID: `ch.zerogen.containertrack`

Wenn das Repository schon eine sinnvolle native ID definiert, nicht unnötig ändern.

---

# 6. Datenmodell

## Job

- `id`
- `type`: `DELIVERY | RETURN`
- `referenceNumber`
- `kt`
- `orderValue`
- `quantity`
- `additionalWork`
- `quick`
- `createdAt`
- `updatedAt`
- `completedAt?`
- `exportedAt?`
- `loggerStoppedAt?`

## Container

- `id`
- `jobId`
- `sequenceIndex`
- `containerNumber`
- `completedAt?`

## Photo

Nicht als BLOB in Room speichern.

- `id`
- `containerId`
- `step`
- `localPath`
- `capturedAt`

Bilder in app-internem Storage.

## LoggerReminder

- `id`
- `jobId`
- `referenceNumber`
- `dueAt`
- `confirmedAt?`
- `notificationId`

---

# 7. Startscreen

Exakt zwei Hauptwege:

## Lieferung
`Container bei Anlieferung erfassen und dokumentieren.`

## Return
`Container bei Rückgabe erfassen und dokumentieren.`

Zusätzlich:

- letzte lokale Vorgänge
- sichtbarer Status
- Bottom Navigation:
  - Start
  - Aufträge
  - Scannen
  - Export
  - Mehr

Kein Marketing-Hero.

---

# 8. Delivery Workflow

## Setup — Pflichtfelder

1. Order Number
   - Batch-/Ordnernummer
   - nicht identisch mit Excel-Spalte `Order`

2. ZRH Number
   - wird als `KT` geschrieben

3. Order / Container-Typ
   - manuell
   - wird als `Order` geschrieben

4. Quantity
   - 1..50

5. Quick
   - freies Pflicht-Textfeld
   - leer verboten
   - `Nothing` / `Empty` gültig

6. additional Work
   - freies Pflicht-Textfeld
   - leer verboten
   - `Nothing` / `Empty` gültig

Kein Job darf vor vollständiger Validierung gespeichert werden.

## Pro Container

`Container scannen -> Foto 1 -> Foto 2 -> Foto 3 -> Foto 4 -> Foto 5 -> Foto 6`

Container-Nummer:

- exakt 4 Ziffern
- führende Null erhalten, z. B. `0708`
- erster gültiger OCR-Wert sofort übernehmen
- Duplicate innerhalb desselben Jobs blockieren
- manuelle Eingabe als Fallback
- erkannter Wert editierbar

## Pflichtfotos Delivery

1. Leerer Innenraum
2. Aussenseite – Front
3. Aussenseite – Rückseite / Türen
4. Aussenseite – links
5. Aussenseite – rechts
6. Befüllter Innenraum

Regeln:

- kein Überspringen
- nächster Schritt erst nach vorherigem Foto
- Vorschau
- kontrollierter Retake
- konsistenter Status bei Löschen
- Fortschritt `x / 6`

Dateinamen:

`<containerNumber>_01.jpg` bis `<containerNumber>_06.jpg`

Keine ZRH Number, Order Number, Datum oder Container-Typ im Bilddateinamen.

---

# 9. Return Workflow

## Schritt 1 zwingend: Logger stoppen

Nach Tippen auf `Return` sind Return-Felder gesperrt.

Erst:

`Logger gestoppt – bestätigen`

Danach:

- `loggerStoppedAt = now`
- Formular freischalten

## Setup — Pflichtfelder

1. Return Number
   - Batch-/Ordnernummer
   - wird als `KT` geschrieben

2. Order / Container-Typ
   - manuell
   - wird als `Order` geschrieben

3. Quantity
   - 1..50

4. Quick
   - freies Pflicht-Textfeld
   - leer verboten
   - `Nothing` / `Empty` gültig

5. additional Work
   - freies Pflicht-Textfeld
   - leer verboten
   - `Nothing` / `Empty` gültig

## Pro Container

- Container-Nummer scannen
- Duplicate blockieren
- manuelle Eingabe als Fallback
- exakt 1 Foto: `Inhalt bei Return`

Dateiname:

`<containerNumber>_01.jpg`

---

# 10. Camera / OCR

## CameraX

Nutze:

- Preview
- ImageAnalysis
- ImageCapture
- Back Camera
- Torch sofern verfügbar
- Runtime Capability Check

ImageAnalysis:

- `STRATEGY_KEEP_ONLY_LATEST`
- nie mehrere OCR-Jobs parallel
- ROI entspricht sichtbarem Scanrahmen
- Analyse nur im ROI

## OCR

Default:

`com.google.mlkit:text-recognition` bundled

Ziel:

- komplett on-device
- sofort verfügbar
- kein Model-Download zur Laufzeit

Logik:

1. ROI croppen.
2. Nur OCR im Scanrahmen.
3. Konservativ normalisieren:
   - O/Q/D -> 0
   - I/L -> 1
   - S -> 5
   - B -> 8
4. alles außer Ziffern entfernen.
5. Nur exakt 4 Ziffern akzeptieren.
6. ersten gültigen Treffer sofort übernehmen.
7. Kamera schließen / Foto-Workflow freigeben.
8. Haptisches Feedback.

Kein 2x/3x Bestätigungsloop.

---

# 11. Fotos

- CameraX `ImageCapture`
- JPEG
- EXIF-Orientation korrekt
- sinnvolle Kompression
- ungefähr 2200 px längste Kante, sofern Qualität passt
- intern speichern
- keine Gallery-Verschmutzung
- kein Cloud Upload

---

# 12. Export

## Delivery ZIP

`<OrderNumber>.zip`

```text
<OrderNumber>/
├── 0708/
│   ├── 0708_01.jpg
│   ├── 0708_02.jpg
│   ├── 0708_03.jpg
│   ├── 0708_04.jpg
│   ├── 0708_05.jpg
│   └── 0708_06.jpg
├── 0710/
│   └── ...
├── <OrderNumber>_EXCEL.txt
└── <OrderNumber>_META.json
```

## Return ZIP

`<ReturnNumber>.zip`

```text
<ReturnNumber>/
├── 0708/
│   └── 0708_01.jpg
├── 0710/
│   └── 0710_01.jpg
├── <ReturnNumber>_EXCEL.txt
└── <ReturnNumber>_META.json
```

Native `ZipOutputStream`.

Export nur bei vollständigem Job.

---

# 13. Excel-kompatible TXT-Datei

UTF-8, tab-getrennt.

Header exakt:

`Datum	In/Out	KT	Order	Quantity	additional Work	Container Number	Quick`

## Delivery

- Datum = Gerätedatum `dd.MM.yyyy`
- In/Out = `OUT`
- KT = ZRH Number
- Order = manuell eingegebener Container-Typ
- Quantity = ausgewählte Container-Anzahl
- additional Work = Pflichtwert
- Container Number = alle Container-Nummern kommagetrennt in einem Feld
- Quick = Pflichtwert

## Return

- Datum = Gerätedatum `dd.MM.yyyy`
- In/Out = `IN`
- KT = Return Number
- Order = manuell eingegebener Container-Typ
- Quantity = ausgewählte Container-Anzahl
- additional Work = Pflichtwert
- Container Number = alle Container-Nummern kommagetrennt in einem Feld
- Quick = Pflichtwert

Tabs/Newlines in Eingaben vor Export in Leerzeichen bzw. ` / ` normalisieren.

---

# 14. Logger Workflow

## Delivery

Erst nach erfolgreichem lokalen Export:

- `dueAt = exportedAt + 30 Minuten`
- Reminder in Room persistieren
- sichtbaren Countdown starten

Anzeige:

- Order Number
- `MM:SS`
- Fortschrittsbalken
- bei mehreren offenen Remindern `+N weitere`
- nach Ablauf deutlich `JETZT`

## Standby / App geschlossen

Native Android verwenden:

- `AlarmManager`
- `BroadcastReceiver`
- NotificationChannel
- `POST_NOTIFICATIONS` ab API 33
- wenn Exact Alarm Access verfügbar: `setExactAndAllowWhileIdle`
- wenn nicht: lokale Erklärung + optional Systemeinstellung öffnen
- Fallback: `setAndAllowWhileIdle`

Optional sinnvoll:
- `RECEIVE_BOOT_COMPLETED`
- offene Reminder aus Room nach Reboot neu planen

## Reminder fällig

- High-Priority Notification `Logger starten`
- Notification Action: `Logger gestartet`
- solange unbestätigt zeigt App beim Öffnen einen nicht wegwischbaren Dialog
- Bestätigung setzt `confirmedAt`
- Reminder nicht doppelt anlegen
- Re-Export desselben Jobs darf Timer nicht resetten

---

# 15. Lokale Verläufe

Anzeige:

- Delivery/Return
- Referenznummer
- Containerfortschritt
- zuletzt geändert
- Status geplant / in Bearbeitung / vollständig / exportiert

Funktionen:

- fortsetzen
- ansehen
- bewusst löschen
- keine Cloud-Synchronisierung

App-Neustart / Prozess-Tod darf Fortschritt nicht verlieren.

---

# 16. UI Designsystem

Nutze:

`design/containertrack-ui-reference.png`

als visuelle Referenz.

Keine Firmenlogos oder fremden Markennamen übernehmen.

## Farben

- Navy `#061F3B`
- Graphite `#1E293B`
- Electric Blue `#3882F6`
- Light Blue `#60A5FA`
- Success `#10B981`
- Warning `#F59E0B`
- Error `#EF4444`
- Secondary Gray `#64748B`
- Text/Icon White `#F8FAFC`

## Stil

- Dark-first
- kompakt
- operativ
- klare Hierarchie
- Cards nur für echte Gruppierung
- Electric Blue nur für Primäraktion/Fokus
- kein Glassmorphism
- keine dekorativen Verläufe
- keine riesigen Emojis
- keine Marketingtexte

Mindestens Screens:

1. Start / Dashboard
2. Delivery Setup
3. Return Logger Stop
4. Return Setup
5. Container OCR Scan
6. Photo Workflow Delivery
7. Photo Workflow Return
8. Container Status / Review
9. Job Status / Batch
10. History / Aufträge
11. Export Review
12. Logger Reminder Dialog

---

# 17. UX Regeln

- Touch Targets >= 48dp
- Fehler direkt am Feld
- keine Toast-only Fehler für kritische Eingaben
- Back Navigation verliert keinen Fortschritt
- klare Scan-Anweisung
- Kamera-Permission Denial mit Fallback
- Notification Permission Denial sauber
- Exact Alarm Access Denial sauber
- alle Texte Deutsch
- keine ungefilterten technischen Fehlermeldungen

---

# 18. Berechtigungen

Nur:

- CAMERA
- POST_NOTIFICATIONS ab API 33
- SCHEDULE_EXACT_ALARM nur falls eingesetzt
- RECEIVE_BOOT_COMPLETED nur für Reboot-Recovery

Nicht:

- INTERNET
- Standort
- Kontakte
- Mikrofon
- breite Storage Permissions

---

# 19. Tests

## Unit

- Delivery Pflichtfelder
- Return Pflichtfelder
- Quick leer -> Fehler
- additional Work leer -> Fehler
- `Nothing` / `Empty` -> gültig
- Container 4-stellig
- führende Null
- Duplicate Block
- Delivery Foto-State-Machine 1..6
- Return 1 Foto
- TXT Delivery Mapping
- TXT Return Mapping
- Dateinamen
- ZIP-Pfade
- Reminder dueAt
- kein doppelter Reminder nach Re-Export

## Integration

- Room speichern/laden
- Prozess-Neustart
- lokale Bilder
- Delete Job
- Pending Reminder bleibt erhalten

## Instrumentation

- Delivery Happy Path
- Return Happy Path
- Kamera Permission Denial
- Resume nach App-Neustart
- Notification Flow
- Alarm Receiver

## Screenshot QA

Golden Screens:

- Start
- Delivery Setup
- OCR Scan
- Delivery Photo Workflow
- Return Logger Gate
- Return Setup
- Export Review
- Logger fällig

Roborazzi nur, wenn Setup überschaubar bleibt.

---

# 20. Offline-/Privacy-Gate

Release-blockierend:

1. Merged Manifest enthält kein `INTERNET`.
2. App funktioniert im Flugmodus.
3. OCR funktioniert im Flugmodus.
4. Delivery vollständig im Flugmodus.
5. Return vollständig im Flugmodus.
6. Export im Flugmodus.
7. Logger Notification im Flugmodus.
8. kein Firebase.
9. kein Analytics SDK.
10. kein Crash-Cloud SDK.
11. keine URL-/HTTP-Aufrufe im Produktionscode.

---

# 21. GitHub Actions

`.github/workflows/android.yml`

Bei Push/PR:

- `./gradlew test`
- `./gradlew lint`
- `./gradlew assembleDebug`

Bei Erfolg Debug-APK als Workflow-Artifact.

Keine Release-Signing-Secrets ins Repo.

---

# 22. Milestones

## M0 — Audit
- Repo lesen
- alte PWA gegen Spezifikation mappen
- keine neue Architektur erfinden
- kurze Reihenfolge festlegen

## M1 — Native Foundation
- Android-Projekt
- Compose Theme
- Navigation
- Room
- Modelle
- Start / Delivery / Return Setup

## M2 — Workflow State
- Validierung
- Batch
- History
- Resume
- Delivery 6-Step
- Return 1-Step

## M3 — Camera + OCR
- CameraX
- bundled ML Kit
- ROI
- 4-digit first-hit
- manual fallback
- torch

## M4 — Photos
- Capture
- preview
- retake
- strict sequence
- internal storage

QUALITY GATE 1:
- build
- unit tests
- targeted instrumentation
- lint
- Anti-Slop Review
- Ponytail Review

## M5 — Export
- TXT
- ZIP
- folder tree
- SAF/local export
- metadata JSON

## M6 — Logger
- sichtbarer Timer
- Room Reminder
- AlarmManager
- Notification Action
- Return Logger Stop Gate
- optional Reboot Recovery

## M7 — Design / Visual QA
- Referenzbild angleichen
- kompakte Dark-Navy UI
- Screenshot Tests
- Default-/Generic-Look entfernen
- Accessibility

## M8 — Final QA
- End-to-End Delivery
- End-to-End Return
- Neustart/Resume
- Flugmodus
- Alarm Paths
- kein Network Permission
- CI APK
- README final

QUALITY GATE 2:
- assembleDebug
- relevante Unit Tests
- Instrumentation Smoke
- lint
- Screenshot Checks
- Offline/Privacy Audit
- Dead-Code Audit
- Dependency Audit
- Anti-Slop Gate
- Ponytail Gate

---

# 23. Definition of Done

Fertig nur wenn:

- keine GitHub Pages nötig
- kein WebView
- kein INTERNET Permission
- APK offline startet
- OCR offline
- Delivery komplett
- Return komplett
- Multi-Container Batch
- Dateinamen korrekt
- ZIP-Struktur korrekt
- TXT korrekt
- Logger lokal persistent
- Standby Reminder via native Android-Mechanismen soweit Berechtigung erlaubt
- Return Logger Stop nicht überspringbar
- Jobs nach Neustart fortsetzbar
- UI entspricht Referenz, ohne fremde Logos/Namen
- keine Fake-/Placeholder-Features
- Tests grün
- README erklärt Installation, Berechtigungen, Offline-Regeln und Exportformat

---

# 24. Schlussregel

Wenn Entscheidung zwischen „mehr Architektur“ und „einfach funktionierend“: kleinere testbare Lösung wählen.

Wenn Dependency nicht zwingend gebraucht wird: nicht hinzufügen.

Wenn Feature nicht in Spezifikation steht: nicht bauen.

Wenn UI-Element keine echte Aktion/Information hat: entfernen.

Baue jetzt M0 und M1 und arbeite danach milestoneweise bis M8.
