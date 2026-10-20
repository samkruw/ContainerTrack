# Prüfstatus — 20.10.2026

## Nachgewiesener GitHub-Build

- Repository: [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1).
- [CI-Lauf 39426728286](https://github.com/samkruw/ContainerTrack/actions/runs/39426728286), Commit `cf1db39adef78c1515ccd6a560f1fe44eb4c22b7`: `test`, `lint`, `assembleDebug` und Merged-Manifest-Privacy-Prüfung erfolgreich.
- 16 Emulatorprüfungen (5 Persistenz, 3 Navigation, 8 Screenshot-Aufnahmen) erfolgreich. Device-Job scheiterte anschliessend am Kopieren der Screenshots nach dem Testlauf. Die APK ist bereits als Artefakt vorhanden.
- Korrekturen aus echten Compiler-/CI-Ausgaben: entferntes SDK-Paket `tools` ersetzt; Room-Gradle-Plugin koordiniert Schemaexport je Variante; Bitmap-Höhe und JSON-Erzeugung korrigiert.
- Aktuelle Fortsetzung: Screenshot-Transfer, zwei Notification-/Receiver-Tests, ein Activity-Neustarttest, fünf Python-QA-Tests und APK-/Abhängigkeitsbericht.

Die folgenden ursprünglichen Audit-/Blockadeangaben dokumentieren den lokalen Ausgangszustand; sie werden durch die obigen CI-Nachweise ergänzt.

## Tatsächlich ausgeführt

- Workspace-/Werkzeug-Audit: kein Git-Repository, keine PWA, kein SDK, kein Compiler und keine Gradle-Distribution vorgefunden. Java 21 Runtime vorhanden.
- Beide Masterprompt-Dateien mittels `cmp` als identisch geprüft.
- Gradle-Wrapper-JAR über die verbundene GitHub-Leseverbindung aus dem offiziellen Gradle-Repository, Tag v8.13.0, übernommen. Git-Blob-SHA und ZIP-Integrität lokal geprüft.
- Unabhängige Quellenprüfung: Foto-Commit bei Coroutine-Abbruch, Alarmplanung nach Export, Vorschauwiederherstellung und dokumentierte CameraX-Setter-Nutzung korrigiert. Keine Compilerbestätigung dadurch.
- Vier Tests der Python-Prüfwerkzeuge bestanden: tatsächliche Netzwerkberechtigung und Produktions-Netzcode werden abgewiesen, reine Manifest-Entfernungsmarker akzeptiert und Screenshot-Übereinstimmung/Abweichung/fehlende Bilder korrekt erkannt. Das sind keine Android-App-Tests.
- Quellmanifest-/Privacy-Check, XML-/TOML-Parse und Projektstruktur werden vor Übergabe lokal geprüft; abschliessende Ausgaben siehe `verification.log`.

## Vorhandene, noch nicht ausgeführte Android-Tests

- 11 JUnit-Tests: Pflichtfelder/Quantity, Return-Gate, sichere Referenznummer, OCR, führende Null, Duplikate, Fotofolge, TXT OUT/IN, Timer, ZIP und unvollständiger Export.
- 5 Persistenz-/Workflow-Instrumentationstests: Datenbank wieder öffnen, Fotos und Reminder wiederherstellen, Kaskadenlöschung, Fotoreihenfolge, Export/Re-Export, Return, Abbruch nach Foto-Commit.
- 3 Compose-Navigationstests.
- 8 Screenshot-Aufnahmen und Vergleichswerkzeug. Keine freigegebenen Golden-Bilder vorhanden; damit noch keine bestandene Screenshot-Regression.

## Milestones

| Milestone | Implementierung | Verifikation |
|---|---|---|
| M0 Audit | abgeschlossen | lokal geprüft |
| M1 Native Foundation | Quellcode vorhanden | Build offen |
| M2 Workflow State | Quellcode vorhanden | JUnit/Room/Gerät offen |
| M3 Camera/OCR | Quellcode vorhanden | echte Kamera und ROI offen |
| M4 Fotos | Quellcode vorhanden | Rotation, Neuaufnahme, EXIF und Prozessabbruch auf Gerät offen |
| M5 Export | Quellcode vorhanden | ZIP/TXT auf Android offen |
| M6 Logger | Quellcode vorhanden | Standby, Boot und Benachrichtigungsaktion offen |
| M7 Design | Navy/Blue-UI vorhanden | Referenzbild fehlt; Screenshots/Accessibility offen |
| M8 Final QA | CI und Prüfanleitung vorhanden | beide Quality Gates offen |

## Freigabeblockaden

1. `./gradlew test lint assembleDebug` muss in einer Android-Build-Umgebung erfolgreich durchlaufen. Hier scheitert schon die Werkzeugbereitstellung. Keine getestete APK vorhanden.
2. Merged Manifest und transitive Abhängigkeiten nach Build prüfen. Entfernen von Netzwerkrechten ist im Quellmanifest erzwungen, aber das Paket wurde noch nicht geprüft.
3. SDK 36 ist gepinnt; neueste stabile SDK-Version zum Releasezeitpunkt prüfen.
4. Auf einem realen Gerät: Lieferung mit mindestens zwei Containern und Return vollständig im Flugmodus, führende Null, Duplikat, manuelle Eingabe, Torch, alle Fotos und resultierendes ZIP prüfen.
5. Logger mit erlaubten/verweigerten Notifications und genauen Alarmen; Display aus, Neustart und Notification-Aktion testen. Android-Force-Stop-Grenze beachten.
6. App-/Prozessneustart, Rotation und Zurück-/Tab-Navigation prüfen. Nicht bestätigte Kameraaufnahmen können bei abruptem Tod vor interner Ablage verloren gehen. Ein bestätigter Foto-Commit darf keine fehlende Datei hinterlassen.
7. Acht Screenshots aufnehmen, mit nachgelieferter Designreferenz vergleichen und als Goldens freigeben. Schriftvergrösserung und Touchziele auf Gerät prüfen.

Acceptance-Matrix bleibt bis zu diesen Nachweisen bewusst unmarkiert. Es wurde nichts gepusht, veröffentlicht, gemergt oder auf ein Gerät installiert.

Der Abbruch-Instrumentationstest reagiert auf sichtbar gewordene Room-Daten. Weil Flow-Benachrichtigungen asynchron sind, beweist er nicht deterministisch jedes einzelne Zeitfenster direkt nach dem Commit; dieser enge Grenzfall benötigt zusätzliche gezielte Geräteprüfung.
