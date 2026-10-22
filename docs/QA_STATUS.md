# Prüfstatus — Update 1.3.0 — 22.10.2026

Repository [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Branch `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Nur Debug-APK; kein Merge, kein Release.

## Abschließender Nachweis

[GitHub-Lauf 39807919494](https://github.com/samkruw/ContainerTrack/actions/runs/39807919494) ist vollständig erfolgreich. Geprüfter App-/Test-Commit: `3da0c6f90b467b59048e5d742bad760df8d7033e`. Nachfolgende Dokumentationsänderungen verändern diesen App-Stand nicht.

[Container-Track-debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39807919494/artifacts/12438786748). Das Artefakt ist ein ZIP mit `app-debug.apk`, Version 1.3.0 / versionCode 4. GitHub-Artefakte haben begrenzte Aufbewahrungszeit. Weitere Artefakte: `Android-reports`, `Android-device-reports-and-screenshots`.

- Lokal: 38 Gradle-Unit-Tests je Debug/Release, jeweils 0 Fehler und 0 übersprungene Tests. Vollständiger Build mit Lint, Debug-APK und Android-Test-APK erfolgreich. Nach der visuellen Korrektur nochmals `test lint assembleDebug` erfolgreich.
- GitHub: Build, Unit-Tests, Lint, Quell-/Merged-Manifest-/APK-Berechtigungsprüfung sowie sieben Python-QA-Tests erfolgreich. 46 Android-Tests erfolgreich, keine übersprungenen Tests; 17 Screenshot-PNGs erzeugt.
- Unverändertes Schema 1 erhalten; reales Room-Migrationstest-Szenario 1→2 bewahrt Aufträge, Foto-Pfade, Reminder-ID und Frist und erzeugt keinen historischen Massen-Upload.
- Echte gebündelte ML-Kit-Erkennung von `0708` bei deaktiviertem validierten Netzwerk bestanden. INTERNET ist für Microsoft absichtlich vorhanden; Offline wird tatsächlich getestet.
- Integration prüft dreifache verzögerte Delivery, einmaligen Completion-/Timer-/Queue-Effekt, Return, per-Foto-Bestätigung über Wiederöffnung, falsches Konto, partiellen Retry, persistiertes Retry-After und Fotoersatz. Dauerhafte Fehler benötigen expliziten Retry.
- Native Navigation, Activity-Neustart, reale lokale JPEG-Galerie/Vollbild/Share-ID, zehn Container bei großer Schrift, JPEG-Freigaben, ZIP-Chooser, Export und Alarm-Aktionen geprüft.

Unabhängiger statischer Abschlussreview: keine P1-Befunde; zwei P2 behoben und nachgeprüft (dauerhafte Fehler nur explizit erneut versuchen; installierten Teams-Empfänger vor Angebot prüfen). Erste tatsächliche Screenshot-Sichtprüfung zeigte Material-Standardviolett und zu runde Fotoplätze; beide korrigiert und im abschließenden CI erneut geprüft. Die visuelle Übersicht umfasst sechs synthetische Testansichten. Keine freigegebenen Goldens; der entsprechende Vergleichsschritt wird deshalb übersprungen, nicht die Android-Tests.

Direkter Artefakt-Download in die Cloud ist durch den Netzwerk-Proxy für `productionresultssa6.blob.core.windows.net` blockiert. Die Screenshot-Übersicht wurde aus dem rechtmäßig erhaltenen vollständigen CI-Protokoll-ZIP extrahiert; keine Sperre umgangen. Der GitHub-Download-Link bleibt für angemeldete berechtigte Benutzer verfügbar.

## Noch nicht abgenommen

- Echte Microsoft-Anmeldung und Graph-Übertragung ins Firmen-Team: öffentliche Tenant-/Client-/Redirect-/Team-/Channel-Konfiguration fehlt. Keine Testadapter oder simulierten Erfolge im Produkt. Einrichtung: [MICROSOFT_SETUP.md](MICROSOFT_SETUP.md).
- Update-Installation über bestehender APK mit identischem Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Keine Deinstallation zum vermeintlichen Update empfehlen.
- Reale Kamera/Licht/Fotoqualität, Standby/Boot/OEM-Verhalten und Benachrichtigungsfreigaben auf Hardware.
- TalkBack, große Systemschrift auf Hardware und visuelle Benutzerfreigabe.

INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK sind ausdrücklich vorgesehen. Keine Standort-/Mikrofon-/NFC-/breiten Speicherrechte. Der Quellcode-Audit ist keine technische Netzwerksperre für Bibliotheken.
