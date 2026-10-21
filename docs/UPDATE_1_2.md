# Update 1.2.0

Inkrementelles Update von 1.1.0 auf `codex/native-android-app`; kein Neubau. `versionCode 3`, `versionName 1.2.0`.

## Änderungen

- Formulardaten und Exportzuordnung liegen jetzt auf derselben zentralen `GlassSurface` wie Aufträge und Logger. Top App Bar und Bottom Navigation verwenden sie mit gerader Kante; Navigation und Ziele bleiben erhalten.
- Fotoabläufe erhalten eine gemeinsame Statusfläche. Scan-Hinweise verwenden dieselbe Oberfläche; Kamerabild, OCR-Rahmen und Fotoansicht bleiben ohne Blur.
- Primäraktionen verwenden Electric Blue und kontrastreiche dunkle Beschriftung. Container-/Kamera-Vektormarke für Launcher und App Bar, angelehnt an die mitgelieferten visuellen Vorlagen. Kein fremdes Logo.
- Acceptance Matrix korrigiert: Delivery benötigt weder Quick noch additional Work; Return benötigt beide.
- Unit-Test ergänzt: Return akzeptiert Nothing und Empty in beiden Feldern und Kombinationen.
- Screenshot-Suite um Job Detail, History, Return-Fotos, Berechtigungen und aktiven Reminder erweitert: 13 Szenarien / 14 PNGs.
- Bestehenden CI-Fehler im ShareChooserTest behoben: expliziter Unit-Rückgabetyp, damit JUnit die eigentliche Chooser-Prüfung ausführt. Der vorherige Lauf scheiterte an `Method exportShareButtonLaunchesAndroidChooser() should be void`, nicht an einer ausgeführten Share-Prüfung.

## Bereits vorhandenes Verhalten geprüft und beibehalten

Delivery blendet Quick/additional Work aus und validiert diese nicht. Historische Werte blockieren nicht; TXT und META exportieren beide Werte leer. Return behält Pflichtfelder und Logger-Stopp-Gate. Room-Schema und App-ID `ch.zerogen.containertrack` bleiben unverändert; keine destruktive Migration.

ZIP teilen erstellt ein lokales Archiv im privaten Cache. FileProvider übergibt ACTION_SEND / application/zip / EXTRA_STREAM mit ClipData und temporärem Lesezugriff an den Android Chooser. ZIP speichern bleibt als eigene Sekundäraktion bestehen. Gmail, Outlook oder andere installierte Apps übernehmen einen möglichen Versand. Die App sendet keine E-Mail, nutzt keine Mail-API und hat kein INTERNET-Recht. Chooser-Abbruch ist keine Versandbestätigung.

Re-Share und Re-Export erhalten die erste Exportzeit sowie ID und Frist des bestehenden 30-Minuten-Reminders. Die bestehenden Integrationstests prüfen auch alte Delivery-Werte, lesbare FileProvider-Anhänge, TXT/META, Speichern und erneutes Teilen.

## Kompatibilität und Signatur

Schema-Version 1, Datenbankname, App-ID, interne Fotopfade und Logger bleiben erhalten. Das exportierte unveränderte Room-Schema ist zur Nachvollziehbarkeit aufgenommen. Android akzeptiert `adb install -r` nur mit derselben Signatur wie die installierte APK. Frische GitHub-Runner erzeugen standardmässig einen neuen Debug-Schlüssel. Ohne den ursprünglichen Signaturschlüssel ist eine datenerhaltende Update-Installation daher **nicht bestätigt**. Keine Deinstallation als vermeintliches Update vornehmen; sie löscht interne Aufträge/Fotos. Signaturschlüssel gehören in sichere Build-Konfiguration, niemals ins Repository oder in den Chat.

## Prüfung

Aktuelle Resultate und Hardwaregrenzen stehen in [QA_STATUS.md](QA_STATUS.md). Die bestehende GitHub-CI mit Unit-Tests, Lint, APK, Privacy-/Permission-Gates, Emulator und Screenshots bleibt erhalten. Artefaktname: `Container-Track-debug-APK`.

Referenzbilder wurden im Auftrag visuell bereitgestellt; `design/UI_REFERENCE.png` und freigegebene Screenshot-Goldens liegen nicht im Repository. Die Bildschirmaufnahmen dienen zur Sichtprüfung; ein automatischer Vergleich mit freigegebenen Goldens wird erst bei vorhandenen Baselines behauptet.
