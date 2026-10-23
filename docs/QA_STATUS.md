# Update 1.4.2 — Prüfung läuft

Neue Korrektur: Auftragsinformationen als Begleittext und ausdrücklich kopierbar; Teams-Ziele werden nicht von ContainerTrack erzwungen. Der folgende Nachweis gehört noch zu Version 1.4.1.

# Prüfstatus — Update 1.4.1 — 23.10.2026

Geprüfter App-/Test-Commit: `8d82240fdbabe7c583e48eb6cce67017ef915a85`. [GitHub-Lauf 39909135079](https://github.com/samkruw/ContainerTrack/actions/runs/39909135079): Build und Gerätetests erfolgreich. Spätere Dokumentations-Commits ändern weder App noch Tests.

[Debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39909135079/artifacts/12482033738): ZIP mit `app-debug.apk`, Version 1.4.1 / versionCode 8. Artefakte haben begrenzte Aufbewahrungszeit.

- 39 Unit-Tests je Variante erfolgreich, keine Fehler oder übersprungenen Tests. Neun Python-QA-Tests, Lint, Debug-/Instrumentation-Kompilierung sowie Manifest-/APK-Berechtigungsprüfung bestanden.
- 60 Android-Tests auf API 35 erfolgreich, keine übersprungenen Tests. 24 Screenshot-PNGs erzeugt; sieben aktuelle Ansichten in der [visuell geprüften Übersicht](../design/verification/reference-ui-v1.4.1.jpg). Anmeldung und Benutzerverwaltung entfallen auf ausdrücklichen Nutzerwunsch.
- LocalJobAccessTest prüft vollständige Aufträge mit früheren lokalen Admin-/Mitarbeiter-/Microsoft-Kennungen und ohne Kennung: alle zugänglich, Fotos teilbar, Eigentümerfelder und Fotos erhalten, keine neuen Upload-Queues. Keine Authentifizierung oder testseitige Benutzer-Vorbelegung erforderlich.
- NavigationTest und ReferenceScreenshotsTest prüfen den direkten Start in die Aufträge, Geräteeinstellungen ohne PIN/Benutzerverwaltung, bestehende Navigation und Activity-Neuerstellung.
- Abschluss übergibt nummerierte Original-JPGs an Teams beziehungsweise Android-Teilen, ohne Upload-Bestätigung oder erneute Completion-/Reminder-Effekte. Sharing-, Auswahl-, Migration-, OCR-, Export- und Reminder-Regressionen bleiben bestanden. Alte Graph-Worker bleiben deaktiviert.
- Das lokal erzeugte APK wurde als `ch.zerogen.containertrack`, Code 8, Version 1.4.1 geprüft; frühere LocalUser-/Store-/Screen-Klassen sind nicht mehr im DEX enthalten.

Die native Screenshot-Übersicht wurde tatsächlich geöffnet und visuell geprüft. Sie enthält ausschließlich synthetische Testdaten; keine Nutzerfotos. Kein automatischer Pixelvergleich: dafür liegen keine freigegebenen Goldens vor.

## Grenzen

Es gibt keine zentrale Anmeldung, Rollenverwaltung, Team-Galerie oder Synchronisierung zwischen Geräten. Teams verwaltet seine eigene Anmeldung; angebotene Empfänger und Kanäle hängen von Teams-Version und Konto ab. Reale Teams-Zielauswahl, SAF-Provider, Kamera/OEM und TalkBack benötigen Prüfung auf dem Nutzergerät. Der Teilen-Dialog bestätigt keinen Versand.

Eine Update-Installation benötigt dasselbe Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Keine Deinstallation als Update empfehlen: sie würde lokale Aufträge und Fotos löschen. Die App-Datenbank bleibt Schema 2, ohne destruktive Migration.

Details: [UPDATE_1_4_1.md](UPDATE_1_4_1.md). Der frühere Microsoft-Adapter bleibt als deaktivierter Kompatibilitätscode erhalten und benötigt für diesen Ablauf keine Konfiguration. Der Bildschirmnachweis wurde aus dem regulären CI-Protokoll gewonnen.
