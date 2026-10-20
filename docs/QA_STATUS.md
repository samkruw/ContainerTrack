# Prüfstatus — 20.10.2026

## Nachweise

Repository: [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack). Umsetzung auf `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Kein Merge und keine Veröffentlichung.

[CI-Lauf 39427920313](https://github.com/samkruw/ContainerTrack/actions/runs/39427920313), Commit `11b56ece015ad0e60b4a8b20f2dbb4a228a54e64`, vollständig erfolgreich:

- `./gradlew test lint assembleDebug`: erfolgreich. 11 Kernregeltests jeweils für Debug und Release ausgeführt.
- Merged Manifest: keine unerlaubten Berechtigungen, Backup deaktiviert, keine Produktions-Netzaufrufe und keine direkten Cloud-/Analytics-Abhängigkeiten.
- Gebautes APK: `apkanalyzer manifest permissions`, kein `android.permission.INTERNET`.
- Fünf Python-Tests der QA-Werkzeuge erfolgreich, einschliesslich Ablehnung echter Netzwerkberechtigungen und Unterscheidung von App-/Test-Manifest.
- 19 API-35-Emulatorprüfungen bestanden, keine übersprungen: fünf Persistenz-/Workflowtests, vier Navigation-/Neustarttests, zwei Notification-/Receiver-Tests und acht Screenshot-Aufnahmen.
- Acht Screenshots nach Testende erfolgreich vom Emulator übernommen.
- Debug-APK, Android-Berichte, Room-Schema und Laufzeit-Abhängigkeitsbericht als Workflow-Artefakte gespeichert.

Die abschliessende Änderung zentralisiert zusätzlich die Logger-Bestätigung und das Entfernen der Benachrichtigung in einem gegen Coroutine-Abbruch geschützten Repository-Aufruf. Der aktuelle PR-Check prüft auch diesen Stand; die Statusanzeige des PR ist für den jeweils neuesten Commit massgeblich.

Ein zusätzlicher direkter ML-Kit-Instrumentationstest prüft das gebündelte Modell anhand eines Textbildes mit `0708` bei eingeschaltetem Flugmodus sowie die fehlende INTERNET-Berechtigung des installierten APK. Damit umfasst die Suite jetzt 20 Emulatorprüfungen; der Nachweis des jeweils neuesten Stands steht im PR-Check.

## Geprüftes Verhalten

- Pflichtfelder, Quantity 1–50, Nothing/Empty und Return-Logger-Gate.
- Vierstellige Nummern, führende Nullen, OCR-Normalisierung und Duplikatsperre.
- Fotoreihenfolge 1–6 bzw. 1, Dateien intern, Datenbankwiederöffnung und Kaskadenlöschung.
- TXT OUT/ZRH und IN/Return-Zuordnung, ZIP-Pfade und Foto-Dateinamen.
- Erstexport + 30 Minuten, Re-Export ohne neue Frist, JSON-Metadaten mit Fotoangaben.
- Receiver: fällige Notification, Bestätigung ohne Zeitstempel-Reset, gesperrte Benachrichtigungen lassen Reminder offen.
- Activity-Neustart erhält Return-Freigabe und Formularinhalt; alle fünf Hauptziele erreichbar.

## Offene Freigabeprüfungen

| Bereich | Offen |
|---|---|
| Reale Kamera/OCR | Schilder, verschiedene Lichtverhältnisse, sichtbarer ROI, Torch und OCR-Erkennungsrate auf Hardware |
| Fotos | reale JPEG-Qualität und EXIF-Orientierung auf mehreren Geräten |
| Vollständiger Flugmodus-Test | Lieferung mit mindestens zwei Containern, Return und Export auf Hardware |
| Standby und Reboot | reale Alarmzustellung bei erlaubten/verweigerten Benachrichtigungen und genauen Alarmen; Android-Force-Stop-Grenze |
| Prozesstod | zusätzliche reale Prozess-/Geräteneustarts; die Room-Wiederöffnung ist automatisiert geprüft |
| Visuelle Abnahme | Referenzbild wurde nicht mitgeliefert; acht Aufnahmen vorhanden, noch keine freigegebenen Goldens |
| Accessibility | TalkBack, grosse Schrift und Touchziele auf Hardware |
| SDK-Version | SDK 36 festgesetzt; neueste stabile SDK-Version vor produktiver Freigabe abgleichen |

Die ursprüngliche Acceptance-Matrix bleibt bis zur vollständigen Abnahme unmarkiert. Bestätigte Fotoabschlüsse und die Alarm-/Bestätigungsfinalisierung sind gegen Coroutine-Abbruch geschützt; unbestätigte Kameraaufnahmen vor interner Ablage und ein abrupter Prozessabbruch zwischen externem ZIP-Schreiben und Room-Commit unterliegen den in der README beschriebenen Grenzen.

Der Abbruch-Instrumentationstest reagiert auf sichtbar gewordene Room-Daten. Da Flow-Benachrichtigungen asynchron sind, beweist er nicht deterministisch jedes Zeitfenster direkt nach dem Commit.

## Ausgangszustand

Im lokalen Workspace fehlten SDK, JDK-Compiler und Gradle-Distribution. Der lokale Download war sandboxbedingt nicht ausführbar. Der lokale fehlgeschlagene Buildversuch ist in `verification.log` erhalten; er ist durch die erfolgreichen GitHub-Builds überholt. Die alte PWA wurde auf main-Stand `ce930e822cfd5d43cf9e6b1005e80bf0716ee1c4` gelesen und unter `legacy-pwa/` unverändert archiviert. Sie ist kein APK-Bestandteil.
