# Container Track — Android

Native Kotlin-App zur lokalen Erfassung von Lieferungen und Returns. Ein `:app`-Modul mit Compose, Room, CameraX und gebündeltem ML Kit. Kein WebView, kein Backend, keine Anmeldung.

**Stand:** Native App implementiert. Build, Debug-/Release-Unit-Tests, Lint und Merged-Manifest-Prüfung haben über GitHub Actions bereits erfolgreich ausgeführt. Eine Debug-APK steht als Workflow-Artefakt zur Verfügung. Die ersten 16 Emulatorprüfungen sind bestanden; der Screenshot-Transfer im Workflow wird noch korrigiert und die zusätzliche Receiver-/Neustartabdeckung geprüft. Reale Kamera-, Flugmodus- und Standbytests bleiben offen. Aktueller Nachweisstand: [QA_STATUS.md](docs/QA_STATUS.md).

## Projekt öffnen und APK bauen

1. Ordner in Android Studio öffnen.
2. JDK 17 oder neuer, Android SDK Platform 36 und Build Tools 36.0.0 bereitstellen. Gradle 8.13 wird über den mitgelieferten Wrapper geladen. Nur der Entwicklungs-Build benötigt Internet für Werkzeuge und Abhängigkeiten.
3. Gradle synchronisieren und ausführen:

```sh
./gradlew test lint assembleDebug
python3 tools/check_privacy.py --merged
```

Windows: `gradlew.bat test lint assembleDebug`.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Über Android Studio installieren oder mit `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Android 8.0 / API 26 oder höher erforderlich. SDK 36 ist festgesetzt; die Vorgabe „aktuell stabil“ muss vor Freigabe gegen das dann verfügbare Android SDK geprüft werden.

Bei Push/PR führt [android.yml](.github/workflows/android.yml) Build, Unit-Tests und Lint aus und lädt die Debug-APK bei Erfolg hoch. Ein weiterer Job startet einen API-35-Emulator für Integration, Navigation und Screenshot-Aufnahmen. Repository: [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Umsetzung auf Branch `codex/native-android-app`. Aktueller Nachweisstand: [QA_STATUS.md](docs/QA_STATUS.md). Keine Release-Schlüssel enthalten.

## Bedienung

- **Start:** Lieferung oder Return anlegen. Alle Pflichtfelder ausfüllen; `Nothing` und `Empty` sind gültig. Quantity: 1–50.
- **Return:** zuerst den gestoppten Logger bestätigen. Der Zeitstempel wird genau bei der Bestätigung gesetzt. Erst danach sind die Formularfelder aktiv.
- **Scannen:** Auftrag auswählen; vierstellige Nummer in den Rahmen halten. Der erste gültige OCR-Treffer wird übernommen. Manuelle Eingabe und spätere Nummernkorrektur sind möglich. Führende Nullen bleiben erhalten; doppelte Nummern im Auftrag werden blockiert.
- **Fotos:** Lieferung benötigt sechs Fotos in vorgeschriebener Reihenfolge, Return genau ein Inhaltsfoto. Foto prüfen und ausdrücklich verwenden. Neuaufnahme ersetzt erst nach Bestätigung das bisherige Foto. Löschen ab einem Schritt entfernt auch die folgenden Fotos und setzt den Abschlussstatus zurück.
- **Aufträge:** lokal gespeicherte Vorgänge fortsetzen, ansehen oder nach gesonderter Bestätigung löschen. Exportierte Aufträge sind zur Ansicht und zum erneuten Export verfügbar.
- **Export:** nur vollständige Aufträge exportieren. Androids Dateiauswahl legt den Speicherort fest. Für vollständigen Offline-Betrieb einen lokalen Ordner auswählen; externe Dokumentanbieter werden vom Benutzer gewählt und liegen ausserhalb der App.
- **Mehr:** Kamera, Benachrichtigungen und genaue Alarme verwalten.

## Exportformat

```text
<Referenz>.zip
└── <Referenz>/
    ├── 0708/
    │   ├── 0708_01.jpg
    │   └── ...
    ├── <Referenz>_EXCEL.txt
    └── <Referenz>_META.json
```

Lieferung: sechs JPEGs `01`–`06`. Return: nur `01`. Referenz ist Order Number bzw. Return Number. Nummern sind keine Ganzzahlen. Interne Dateien verwenden UUIDs; nur der Export bekommt die vorgegebenen Dateinamen.

TXT: UTF-8, tabgetrennt, eine Kopfzeile und eine Datenzeile mit allen Container-Nummern in Reihenfolge, kommagetrennt. Kopfzeile:

```text
Datum\tIn/Out\tKT\tOrder\tQuantity\tadditional Work\tContainer Number\tQuick
```

Lieferung: `OUT`, KT = ZRH Number. Return: `IN`, KT = Return Number. Order = manuell eingegebener Container-Typ. Datum = lokales Gerätedatum `dd.MM.yyyy`. Tabs werden zu Leerzeichen, Zeilenumbrüche zu ` / `. Die TXT-Datei kann beim Import als Tab-getrennt angegeben werden. In Tabellenprogrammen Container-Nummern als Text importieren, um führende Nullen zu bewahren.

META enthält Auftragswerte, Fotozeitpunkte und relative Exportnamen, keine internen Dateipfade. `archiveCreatedAt` bezeichnet die Archiverstellung; `exportedAt` im Archiv ist der zuvor bekannte erste Exportzeitpunkt, beim ersten Archiv noch `null`. Der tatsächliche erfolgreiche Exportzeitpunkt wird erst nach dem Schliessen des Zielstreams in Room gespeichert.

## Logger und Berechtigungen

Nach dem ersten erfolgreichen Delivery-Export wird genau eine Room-Erinnerung angelegt: erster Exportzeitpunkt + 30 Minuten. Erneuter Export erzeugt keine neue Frist. Countdown und Fortschritt bleiben in der App sichtbar; bei Fälligkeit erscheint ein nicht wegwischbarer Bestätigungsdialog. Eine Benachrichtigung bietet die Aktion „Logger gestartet“.

`CAMERA` für Scan/Fotos, `POST_NOTIFICATIONS` ab Android 13, optionaler Zugriff `SCHEDULE_EXACT_ALARM` für zeitgenaue Standby-Alarme. Ohne genauen Alarmzugriff nutzt die App `setAndAllowWhileIdle`; Android darf die Zustellung verzögern. Ohne Benachrichtigungsfreigabe bleibt der Dialog beim Öffnen der App wirksam. `RECEIVE_BOOT_COMPLETED` stellt offene Alarme nach Neustart wieder her. Nach explizitem Android-„Beenden erzwingen“ muss die App erneut geöffnet werden; Android verhindert vorher Alarm-/Broadcast-Zustellung.

## Offline und Speicherung

Room und Fotos liegen intern, Fotos nicht als BLOB. Fotos werden als JPEG mit aufgelöster EXIF-Orientierung, maximal 2200 Pixel längster Kante und Qualität 85 gespeichert. Unbestätigte Vorschauen liegen ebenfalls intern; Pfad und Schritt werden für Android-Zustandswiederherstellung gesichert. Ein neuer Auftrag wird erst nach erfolgreicher Formularvalidierung gespeichert.

Netzwerk-, Standort-, Mikrofon- und breite Speicherrechte werden nicht angefordert. Manifest-Merger-Regeln entfernen Netzwerkberechtigungen aus Bibliotheksmanifesten. Android-Backup ist deaktiviert. Kein Analytics-/Cloud-SDK direkt eingebunden. Transitive ML-Kit-Abhängigkeiten müssen beim ersten erfolgreichen Build zusätzlich geprüft werden; der Quelltextcheck ersetzt keine APK-/Abhängigkeitsprüfung.

Prozessabbruch kann vor bestätigter Speicherung eine Aufnahme verlieren. Bei abruptem Prozessabbruch genau zwischen erfolgreichem Export und Room-Commit kann eine ZIP existieren, ohne dass ein Exportabschluss gespeichert ist. Innerhalb des laufenden Prozesses sind Fotoabschluss und Erinnerungsplanung gegen Coroutine-Abbruch geschützt. Offene Erinnerungen werden beim App-Start und Boot erneut geplant.

## Tests und Freigabe

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
python3 tools/check_privacy.py
```

Unit: Validierung, OCR-Normalisierung, Duplikate, Fotofolge, TXT, ZIP-Pfade, Dateinamen und Timer. Android: Room-Wiederöffnung, Dateien/Löschen, Return-Gate, Fotozustände, Export/Re-Export und Navigation. ScreenshotCaptureTest erzeugt acht PNGs unter `Android/data/ch.zerogen.containertrack/files/screenshots`.

Nach Sichtprüfung auf festem Emulator die acht Bilder nach `design/goldens/` übernehmen. Danach prüft CI diese Baselines; lokale Prüfung mit `python3 tools/compare_screenshots.py design/goldens app/build/screenshots` (Pillow erforderlich). Ohne freigegebene Baselines ist Screenshot-Regression noch nicht abgenommen. Das im Auftrag erwähnte UI-Referenzbild wurde nicht mitgeliefert.

Vor produktiver Nutzung: beide Workflows mit realer Kamera im Flugmodus, Permission-Denial, Rotation/Prozesstod, Standby/Neustart, Benachrichtigungsaktion und tatsächliche ZIP/TXT-Dateien prüfen. Die [Acceptance Matrix](docs/ACCEPTANCE_MATRIX.md) bleibt bis zur Verifikation offen.
