# Container Track — Android

Native Kotlin-App zur lokalen Erfassung von Lieferungen und Returns. Ein `:app`-Modul mit Compose, Room, CameraX und gebündeltem ML Kit. Kein WebView. Direkter Start ohne Anmeldung, Benutzerverwaltung oder Rollen; Fotos werden bewusst an die Teams-App übergeben.

**Stand:** Update 1.4.2 / versionCode 9. Fotos mit Begleittext und „Auftragsinfos kopieren“: [UPDATE_1_4_2.md](docs/UPDATE_1_4_2.md). Ohne Benutzerverwaltung, PIN und Rollen; Teams-Teilen beim Abschluss: [UPDATE_1_4_1.md](docs/UPDATE_1_4_1.md). Kein zusätzlicher Online-Dienst erforderlich. Bestehende native Architektur und App-ID bleiben erhalten; Room migriert ohne Datenlöschung von Schema 1 auf 2. Direktes manuelles Teams-Teilen: [UPDATE_1_3_2.md](docs/UPDATE_1_3_2.md). Designkorrektur nach den acht Bildvorlagen: [UPDATE_1_3_1.md](docs/UPDATE_1_3_1.md). Fachliche Änderungen: [UPDATE_1_3.md](docs/UPDATE_1_3.md). Historische Microsoft-Einrichtung (für den aktuellen Ablauf nicht nötig): [MICROSOFT_SETUP.md](docs/MICROSOFT_SETUP.md). Aktuelle Prüfnachweise: [QA_STATUS.md](docs/QA_STATUS.md).

## Projekt öffnen und APK bauen

1. Ordner in Android Studio öffnen.
2. JDK 17 oder neuer, Android SDK Platform 36 und Build Tools 36.0.0 bereitstellen. Gradle 8.13 wird über den mitgelieferten Wrapper geladen. Der Entwicklungs-Build benötigt Internet für Werkzeuge und Abhängigkeiten; die eigentliche Übertragung durch Teams benötigt die Teams-App und deren Verbindung.
3. Gradle synchronisieren und ausführen:

```sh
./gradlew test lint assembleDebug
python3 tools/check_privacy.py --merged
```

Windows: `gradlew.bat test lint assembleDebug`.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Über Android Studio installieren oder mit `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Android 8.0 / API 26 oder höher erforderlich. SDK 36 ist festgesetzt; die Vorgabe „aktuell stabil“ muss vor Freigabe gegen das dann verfügbare Android SDK geprüft werden.

Bei Push/PR führt [android.yml](.github/workflows/android.yml) Build, Unit-Tests und Lint aus und lädt die Debug-APK bei Erfolg hoch. Ein weiterer Job startet einen API-35-Emulator für Integration, Navigation und Screenshot-Aufnahmen. Repository: [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Umsetzung auf Branch `codex/native-android-app`. Aktueller Nachweisstand: [QA_STATUS.md](docs/QA_STATUS.md). Keine Signaturschlüssel enthalten. Für eine Installation als Update muss die APK mit demselben Schlüssel wie die bereits installierte App signiert sein. Der Standard-Debug-Schlüssel eines frischen GitHub-Runners garantiert das nicht. Eine Deinstallation würde lokale Daten löschen; sie ist kein Update-Verfahren.

## Bedienung

- **Plus-Button:** Lieferung oder Return anlegen. Optional die erste Container-Nummer direkt eingeben oder scannen. Weitere Pflichtangaben sind ausklappbar. Alle Pflichtfelder ausfüllen. `Quick` und `additional Work` werden ausschliesslich bei Return angegeben; `Nothing` und `Empty` sind dort gültig. Quantity: 1–50.
- **Return:** zuerst den gestoppten Logger bestätigen. Der Zeitstempel wird genau bei der Bestätigung gesetzt. Erst danach sind die Formularfelder aktiv.
- **Kamera:** Auftrag auswählen; vierstellige Nummer in den Rahmen halten. Der erste gültige OCR-Treffer wird übernommen. Manuelle Eingabe und spätere Nummernkorrektur sind möglich. Führende Nullen bleiben erhalten; doppelte Nummern im Auftrag werden blockiert.
- **Fotos:** Lieferung benötigt sechs Fotos: zuerst je Container 01–05, anschließend darf 06 (befüllt) später folgen. Return benötigt genau ein Inhaltsfoto. Foto prüfen und ausdrücklich verwenden. Neuaufnahme ersetzt erst nach Bestätigung das bisherige Foto. Löschen ab einem Schritt entfernt auch die folgenden Fotos und setzt den Abschlussstatus zurück.
- **Aufträge:** lokal gespeicherte Vorgänge fortsetzen, ansehen oder nach gesonderter Bestätigung löschen. Exportierte Aufträge sind zur Ansicht und zum erneuten Export verfügbar.
- **Export:** nur vollständige Aufträge exportieren. Mit **ZIP teilen** die Android-Auswahl öffnen und beispielsweise ein E-Mail-Programm wählen. Mit **ZIP speichern** legt Androids Dateiauswahl den Speicherort fest. Für vollständigen Offline-Betrieb einen lokalen Ordner auswählen; externe Dokumentanbieter werden vom Benutzer gewählt und liegen ausserhalb der App.
- **Galerie:** vorhandene lokale Fotos ansehen, filtern und einzeln teilen. **Zahnrad / Einstellungen:** Geräteeinstellungen und Berechtigungen öffnen.

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

Lieferung: `OUT`, KT = ZRH Number; die TXT-Spalten `additional Work` und `Quick` bleiben leer, auch bei älteren Aufträgen mit gespeicherten Werten. Return: `IN`, KT = Return Number. Order = manuell eingegebener Container-Typ. Datum = lokales Gerätedatum `dd.MM.yyyy`. Tabs werden zu Leerzeichen, Zeilenumbrüche zu ` / `. Die TXT-Datei kann beim Import als Tab-getrennt angegeben werden. In Tabellenprogrammen Container-Nummern als Text importieren, um führende Nullen zu bewahren.

META enthält Auftragswerte, Fotozeitpunkte und relative Exportnamen, keine internen Dateipfade. `archiveCreatedAt` bezeichnet die Archiverstellung; `exportedAt` im Archiv ist der zuvor bekannte erste Exportzeitpunkt, beim ersten Archiv noch `null`. Der erfolgreiche Exportzeitpunkt wird nach dem Schliessen des Zielstreams bzw. nach vollständiger Archiverstellung zum Teilen in Room gespeichert.

## Logger und Berechtigungen

Beim ersten vollständigen Delivery-Abschluss (alle sechs Fotos jedes Containers) wird genau eine Room-Erinnerung angelegt: Abschlusszeitpunkt + 30 Minuten. Export, Teilen und Wiederholungen setzen sie nicht zurück. Die App bestätigt keinen Versand durch eine andere App. Countdown und Fortschritt bleiben in der App sichtbar; bei Fälligkeit erscheint ein nicht wegwischbarer Bestätigungsdialog. Eine Benachrichtigung bietet die Aktion „Logger gestartet“.

`CAMERA` für Scan/Fotos, `POST_NOTIFICATIONS` ab Android 13, optionaler Zugriff `SCHEDULE_EXACT_ALARM` für zeitgenaue Standby-Alarme. Ohne genauen Alarmzugriff nutzt die App `setAndAllowWhileIdle`; Android darf die Zustellung verzögern. Ohne Benachrichtigungsfreigabe bleibt der Dialog beim Öffnen der App wirksam. `RECEIVE_BOOT_COMPLETED` stellt offene Alarme nach Neustart wieder her. Nach explizitem Android-„Beenden erzwingen“ muss die App erneut geöffnet werden; Android verhindert vorher Alarm-/Broadcast-Zustellung.

## Offline und Speicherung

Room und Fotos liegen intern, Fotos nicht als BLOB. Fotos werden als JPEG mit aufgelöster EXIF-Orientierung, maximal 2200 Pixel längster Kante und Qualität 85 gespeichert. Unbestätigte Vorschauen liegen ebenfalls intern; Pfad und Schritt werden für Android-Zustandswiederherstellung gesichert. Ein neuer Auftrag wird erst nach erfolgreicher Formularvalidierung gespeichert.

Zum Teilen liegen separate ZIP- oder nummerierte JPEG-Kopien im privaten Cache. FileProvider gibt nur diese Dateien mit temporärem Lesezugriff an die gewählte App weiter. Jede Freigabe bekommt einen eigenen Pfad. Auftragslöschung entfernt die internen Kopien; weitere Freigaben bereinigen Kopien älter als sieben Tage. Android darf den Cache früher löschen. Das E-Mail-Programm verarbeitet und versendet den Anhang selbst.

INTERNET, ACCESS_NETWORK_STATE und WAKE_LOCK bleiben aus dem bisherigen Microsoft-Adapter vorhanden. Der automatische Transport ist in Version 1.4.2 deaktiviert; die lokale App benötigt keinen eigenen Online-Dienst. Standort-, Mikrofon-, NFC- und breite Speicherrechte werden nicht angefordert. Android-Backup ist deaktiviert. Die Berechtigungsprüfung lässt nur die ausdrücklich benötigten Rechte zu.

Der geprüfte Abhängigkeitsbericht enthält keine Firebase-Analytics-, Crashlytics- oder Cloud-Datenhaltungs-SDKs. Das vorgeschriebene gebündelte ML Kit bringt jedoch technische Hilfsbibliotheken `firebase-components`, `firebase-encoders`, `firebase-encoders-json`, `firebase-annotations` und Google DataTransport transitiv mit. Diese sind im Bericht ausgewiesen; eine völlige Abwesenheit aller Artefakte mit Firebase-Gruppennamen wird daher nicht behauptet. Der Quellcode-Audit begrenzt Netzwerkadapter auf die Microsoft-Anbindung; dies ist keine technische Netzwerksperre für Bibliothekscode. Es ist kein Firebase-Projekt konfiguriert.

Prozessabbruch kann vor bestätigter Speicherung eine Aufnahme verlieren. Bei abruptem Prozessabbruch genau zwischen erfolgreichem Export und Room-Commit kann eine ZIP existieren, ohne dass ein Exportabschluss gespeichert ist. Innerhalb des laufenden Prozesses sind Fotoabschluss und Erinnerungsplanung gegen Coroutine-Abbruch geschützt. Offene Erinnerungen werden beim App-Start und Boot erneut geplant.

## Tests und Freigabe

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
python3 tools/check_privacy.py
```

Unit: Validierung, OCR-Normalisierung, Duplikate, Fotofolge, TXT, ZIP-Pfade, Dateinamen und Timer. Android: Room-Wiederöffnung, Dateien/Löschen, Return-Gate, Fotozustände, Export/Re-Export, Navigation, Activity-Neustart, Alarm-Receiver und Benachrichtigungsaktion mit gesperrten/erlaubten Benachrichtigungen. OfflineOcrTest schaltet den API-35-Emulator in den Flugmodus und prüft die Erkennung von `0708` durch das echte gebündelte ML-Kit-Modell. ScreenshotCaptureTest erzeugt 17 PNGs unter `Android/data/ch.zerogen.containertrack/files/screenshots`. CI lässt die Testinstallation zum Screenshot-Transfer bis zum Ende des Emulator-Jobs bestehen.

Nach Sichtprüfung auf festem Emulator die Bilder nach `design/goldens/` übernehmen. Danach prüft CI diese Baselines; lokale Prüfung mit `python3 tools/compare_screenshots.py design/goldens app/build/screenshots` (Pillow erforderlich). Ohne freigegebene Baselines ist Screenshot-Regression noch nicht abgenommen. Die im Update-Auftrag mitgelieferte Navy-/Glas-Referenz und Container-/Kamera-Marke dienen als visuelle Richtung. Der App-Marker ist eine kompakte native Vektorumsetzung; die hochaufgelösten Vorlagen werden nicht als Screens eingebettet. Das Referenzbild liegt bislang nicht als `design/UI_REFERENCE.png` im Repository vor.

Vor produktiver Nutzung: beide Workflows mit realer Kamera im Flugmodus, Permission-Denial, Rotation/Prozesstod, Standby/Neustart, Benachrichtigungsaktion und tatsächliche ZIP/TXT-Dateien prüfen. Die [Acceptance Matrix](docs/ACCEPTANCE_MATRIX.md) bleibt bis zur Verifikation offen.
