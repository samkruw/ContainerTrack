# M0 — Audit, 20.10.2026

- Workspace enthält kein Git-Repository und keine alte PWA; Neuanlage unter `container-track/`.
- Die zwei gelieferten Masterprompts sind bytegleich. Ein Exemplar wurde übernommen.
- Referenzbild `design/containertrack-ui-reference.png` wurde nicht mitgeliefert. Farb- und Bedienvorgaben sind verbindlich; Bildabgleich bleibt offen.
- Java 21 vorhanden; keine Android-SDK-, adb-, Gradle- oder Kotlin-CLI gefunden.
- Downloadversuch für Android-Werkzeuge unterbrochen. Keine Build- oder Geräteprüfung vorgetäuscht.
- Architektur gemäß Auftrag: ein app-Modul, Compose/Navigation/ViewModel/Flow, Room, CameraX, gebündeltes ML Kit, ZipOutputStream, SAF und AlarmManager.
- Keine Cloud, keine Netzwerkbibliothek, keine Laufzeit-Netzberechtigung, keine PWA-Migration ohne Quelldaten.
- Reihenfolge M1 Grundlage → M2 Zustände → M3 OCR → M4 Fotos → Gate 1 → M5 Export → M6 Logger → M7 Oberfläche → M8 QA.

## Ergänzung nach GitHub-Zugriff

Das bestehende Repository `samkruw/ContainerTrack` wurde über die verbundene GitHub-App gefunden. Geprüfter main-Stand: `ce930e822cfd5d43cf9e6b1005e80bf0716ee1c4`. Die PWA wurde als Verhaltensreferenz gelesen und unter `docs/legacy-pwa/` archiviert; sie ist kein Bestandteil der Android-App. Sie enthält Pflichtfelder, 6-/1-Fotoabläufe, TXT-Zuordnung und Logger-Fristen bereits als Browserlogik. Abgleich: `PWA_MAPPING.md`.

Die native Umsetzung wird auf `codex/native-android-app` bereitgestellt. GitHub Actions soll den Android-Build und Emulatorprüfungen durchführen; ein Merge erfolgt erst nach den Quality Gates.

Präzisierungen: Container-Duplikate gelten pro Job; Nummern bleiben Strings. Dateien verwenden interne UUID-Pfade; Exportnamen folgen der Vorgabe. Der erste erfolgreiche Exportzeitpunkt bleibt unveränderlich. Nicht bestätigte Fotoaufnahmen zählen nicht. Bestätigte Fotos werden vor Room-Eintrag atomar intern abgelegt. Referenznummern dürfen keine Pfadseparatoren enthalten. Ein verlorenes Kamerabild erzeugt keinen abgeschlossenen Schritt.
