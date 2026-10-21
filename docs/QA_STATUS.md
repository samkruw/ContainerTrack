# Prüfstatus — Update 1.2.0 — 21.10.2026

Repository: [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Branch `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Kein Merge und keine Release-Veröffentlichung. Änderungen: [UPDATE_1_2.md](UPDATE_1_2.md).

## Lokal ausgeführt

- `test lint assembleDebug assembleDebugAndroidTest`: erfolgreich. 14 Kernregeltests jeweils für Debug und Release, ohne Fehler oder übersprungene Tests. App- und Instrumentation-APK kompiliert.
- Lint erfolgreich; Warnungen zu neueren Abhängigkeiten und bestehenden Plattform-/API-Hinweisen bleiben ausgewiesen.
- Fünf Tests der Python-QA-Werkzeuge bestanden.
- Room-Schema 1 stimmt als JSON vollständig mit dem Schema-Artefakt des GitHub-Builds von 1.1.0 überein.
- Quell- und Merged-Manifest-Privacy-Gate bestanden. Auch das gebaute APK wurde geprüft: kein INTERNET-Recht, App-ID unverändert, Version 1.2.0 / versionCode 3.
- Der vorhandene ShareChooserTest hatte einen nicht-void Rückgabetyp: Ursache des fehlgeschlagenen [CI-Laufs 39557005372](https://github.com/samkruw/ContainerTrack/actions/runs/39557005372). Mit explizitem Unit-Rückgabetyp kompiliert er als öffentliche void-Methode. Der neue CI-Lauf muss die tatsächliche Chooser-Interaktion bestätigen.

## GitHub / Emulator

Der neue PR-Check ist für den aktualisierten Commit massgeblich. Die Suite umfasst 33 Instrumentationstests einschliesslich 13 Screenshot-Szenarien / 14 PNGs. Build-Artefakt: `Container-Track-debug-APK`; Gerätetests und Screenshots: `Android-device-reports-and-screenshots`.

Lokale Emulatortests wurden nicht als bestanden gezählt: kein KVM; Software-Emulation wurde gestartet, ADB scheiterte an seinem schreibgeschützten Benutzer-Konfigurationspfad. Der selbst gestartete Emulator wurde wieder beendet. GitHub bietet den vorgesehenen KVM-Testweg.

## Geprüfte bestehende Regeln

Delivery ohne Quick/additional Work; historische Werte bleiben im Delivery-TXT/META leer. Return verlangt beide Felder und Logger-Stopp, akzeptiert Nothing/Empty. Nummern, Duplikate, Fotofolge, ZIP und Timer sind durch Unit- und Integrationstests abgedeckt. Re-Share/Re-Export darf Reminder-ID und Frist nicht verändern.

## Offen vor produktiver Freigabe

- Update-Installation über der ersten APK mit identischem ursprünglichem Signaturschlüssel; lokale Daten, Fotos und Reminder danach prüfen. Signaturvergleich bestätigt: lokale 1.2.0 und bisherige GitHub-1.1.0-APK haben verschiedene Zertifikate. Der ursprüngliche private Schlüssel ist nicht verfügbar; GitHub verweigert dieser Integration zudem das Lesen der Secret-/Variablennamen (HTTP 403). Room/App-ID unverändert ist notwendig, beweist allein jedoch keine Installationskompatibilität.
- Reale Kamera/OCR, Lichtverhältnisse, Torch, EXIF und Fotoqualität auf Android-Hardware.
- Vollständige Lieferung/Return im Flugmodus; reale Mail-App mit ZIP-Anhang, Speichern über lokalen Dokumentanbieter und Chooser-Abbruch.
- Standby, Neustart, genaue Alarme und verweigerte Berechtigungen auf Hardware; Android-Force-Stop-Grenze bleibt bestehen.
- TalkBack, grosse Schrift und visuelle Freigabe der Screenshots anhand der mitgelieferten Referenzen. Noch keine freigegebenen Goldens.

Gebündeltes ML Kit bringt technische Firebase-Komponenten-/Encoder-/Annotations-Hilfsartefakte und Google DataTransport mit. Keine Firebase Analytics, Crashlytics, Cloud-Datenhaltung oder konfigurierte Firebase-Projekte; kein INTERNET im App-Manifest. Der CI-Abhängigkeitsbericht dokumentiert transitive Artefakte.

Historische vollständig erfolgreiche Baselines: [39427920313](https://github.com/samkruw/ContainerTrack/actions/runs/39427920313), [39428951222](https://github.com/samkruw/ContainerTrack/actions/runs/39428951222). Diese ersetzen keine Prüfung des neuen Commits.
