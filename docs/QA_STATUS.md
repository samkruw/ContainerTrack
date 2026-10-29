# Update 1.5.1 — adaptive Kamera

Native Debug-APK 1.5.1 / versionCode 13, `ch.zerogen.containertrack`.

- `test lint assembleDebug assembleDebugAndroidTest` erfolgreich: 50 Unit-Tests je Variante, Lint und beide Debug-APKs gebaut.
- Neun Python-QA-Tests sowie Quellcode-, gemergte Manifest- und tatsächliche APK-Berechtigungsprüfung bestanden.
- Neue Geräteprüfungen für feste Bedienelemente, kleine Viewports, Querformat und Schriftfaktor 1,5 sind kompiliert. Die Ausführung und visuelle Abnahme stehen an diesem Zwischenstand noch aus.
- Die App bleibt lokal; Return-Zählung, E-Mail und Erinnerungen sind unverändert. Details: [UPDATE_1_5_1.md](UPDATE_1_5_1.md).

Der folgende Bericht hält die tatsächlich belegte Prüfung der Vorgängerversion fest.

# Update 1.5.0 — Debug-APK gebaut, UI-Freigabe teilweise offen

App-/Test-Commit: `2b889f5b0da22423ea57a2d9b634ca664e204891`. Native Debug-APK 1.5.0 / versionCode 12, `ch.zerogen.containertrack`, gebaut am 28.10.2026. Lokale Datei: `/workspace/ContainerTrack-1.5.0-debug.apk`. SHA-256: `51e2220b68f10290c82d91d9e18499210fa9cf5d57b2c3cfdb7838286c3cb831`.

## Bestätigte Prüfungen

- Abschließender Build `test lint assembleDebug assembleDebugAndroidTest`: erfolgreich. 50 Unit-Tests je Variante, keine Fehler oder übersprungenen Tests. Neun Python-QA-Tests und Prüfung von Quellcode, gemergtem Manifest und tatsächlichen APK-Berechtigungen bestanden.
- 12 Android-Kernprüfungen auf API 35 bestanden: Return-Migration, gleiche Anfangs-/Abschlussanzahl je Container, vollständiger Auftrag mit allen Containern, feste Frist, Mailrevision und E-Mail-Verträge einschließlich lesbarer, richtig benannter JPEG-URIs.
- Erster Gesamtlauf: 68 von 76 Tests bestanden, acht Fehler in angepassten UI-/Testabläufen. Nach Korrekturen gezielter Lauf: 26 von 30 bestanden. Dabei unter anderem Galerie-Verhalten, fünf Navigationsprüfungen, Return-Fotooberfläche, Mailbestätigungsdialog, sämtliche Return-Repository-Regressionen und die Wiederherstellung einer bereits bestätigten, noch sichtbaren OS-Benachrichtigung erfolgreich.
- Die echte gebündelte OCR hat im Flugmodus `0708` erkannt. Der Software-Emulator benötigte rund 78 Sekunden zur kalten Modellinitialisierung und Erkennung; das frühere 30s-Testlimit wurde auf 120s erweitert. Offline- und Ergebnisprüfungen bleiben erhalten. Dies ist kein Leistungsbenchmark für Smartphones.
- Abschließender Nachlauf nach weiteren Testhilfen-Korrekturen: zwei von sechs Prüfungen bestanden, null gemeldete Fehler, dann durch Neustart der Cloud-Umgebung unterbrochen. Bestätigt sind die angepasste Return-Oberfläche und tatsächlich verweigerte Benachrichtigungsrechte. Die übrigen vier Prüfungen sind **nicht als bestanden belegt**.

## Ausstehende Abnahme

Offen bleiben der aktualisierte Referenz-Fotoauswahltest, der Galerie-Capture sowie die realen UI-Übergaben an die E-Mail- und ZIP-Auswahl. Der vorangegangene Lauf meldete einen Compose-Idle-Timeout auf dem langsamen TCG-Emulator, ein 10s-Übergabezeitlimit, eine fehlende Testberechtigung und ein Activity-Teardown-Problem. Die Testhilfen verwenden nun abfangende Activity-Monitore, 60s-Übergabefristen und die nötigen temporären Testberechtigungen. Diese Korrekturen sind kompiliert; die Berechtigungskorrektur ist bereits auf dem Gerät bestanden. Eine vollständige Android-/UI-Freigabe für 1.5.0 bleibt offen. Der Emulatorprozess ging beim Cloud-Neustart verloren; eine wiederholte langsame Kaltinitialisierung wird nicht als erfolgreicher Testlauf ausgegeben.

Neun aktuelle PNG-Captures sind gesichert und manuell angesehen: sechs Ansichten der nativen App und drei Ausschnitte des neuen Return-Fotobildschirms. Die Schaltflächen wurden nach der Sichtprüfung an die rechteckige Referenzform angeglichen und erneut aufgenommen. [Aktuelle Übersicht](../design/verification/reference-ui-v1.5.0.jpg). Der Fotoauswahl-Capture `24-reference-photo-selection` fehlt weiterhin. Kein automatischer Pixelvergleich und keine freigegebenen Goldens. [Visueller Prüfbericht](../design-qa.md).

## GitHub und praktische Grenzen

Der Branch `codex/native-android-app` löst den vorhandenen [Android-Workflow](https://github.com/samkruw/ContainerTrack/actions/workflows/android.yml) mit KVM-Gerätetests aus. Der Zugriff auf `api.github.com` ist durch die aktive Entwicklungs-Netzwerkregel gesperrt; ein erfolgreicher aktueller CI-Lauf oder eine neue GitHub-Artefakt-ID wird deshalb nicht behauptet. Normales Git funktioniert. Die PR-Metadaten für DraftPR1 sind in `/workspace/.containertrack-setup/pr-1.5.0.json` vorbereitet, aber noch nicht per API aktualisiert. Kein Merge oder Release.

Die App benötigt keinen zusätzlichen Online-Dienst. Empfänger und tatsächlichen Versand übernimmt das Smartphone-Mailprogramm; die App kann weder Versand noch Zustellung feststellen. Reale Mailprogramme, OEM-Kamera, SAF-Provider, TalkBack und große Systemschrift benötigen weitere Geräteprüfung. Android-Rechte und Systembeschränkungen können die tatsächliche Benachrichtigungsanzeige verzögern. Eine Update-Installation benötigt dasselbe Signaturzertifikat; ein neuer CI-Debug-Schlüssel garantiert dies nicht. Nicht zur Aktualisierung deinstallieren: dabei würden lokale Aufträge und Fotos gelöscht.

Build-/Testprotokolle und Rohbilder liegen dauerhaft unter `/workspace/.containertrack-setup/verification/`. Einzelheiten: [UPDATE_1_5_0.md](UPDATE_1_5_0.md).

Der folgende Bericht beschreibt ausdrücklich den älteren Stand 1.4.4, nicht die neue Return-/E-Mail-Funktion.

# Prüfstatus — Update 1.4.4

Geprüfter App-/Test-Commit: `f45788e8c1afaef03189302705f8d582a7696e1e`. [GitHub-Lauf 40330673261](https://github.com/samkruw/ContainerTrack/actions/runs/40330673261) wurde am 26.10.2026 mit erfolgreichem Build und erfolgreichen Gerätetests bestätigt. Dokumentation abgeschlossen am 28.10.2026; seit der Prüfung keine App-/Test-Änderung.

[Debug-APK 1.4.4 herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/40330673261/artifacts/12654208357): ZIP mit `app-debug.apk`, Version 1.4.4 / versionCode 11. Artefakte haben begrenzte Aufbewahrungszeit.

## Bestätigte Prüfungen

- 41 Unit-Tests je Variante erfolgreich, keine Fehler oder übersprungenen Tests. Vier angepasste Regressionen zunächst fehlgeschlagen, nach Implementierung bestanden. Neun Python-QA-Tests, Lint, Debug-/Instrumentation-Kompilierung und Manifest-/APK-Berechtigungsprüfung bestanden.
- 60 Android-Tests auf API 35 erfolgreich, keine übersprungenen Tests. 24 Screenshot-PNGs erzeugt, darunter sieben Referenzansichten: [Geräteberichte und Screenshots](https://github.com/samkruw/ContainerTrack/actions/runs/40330673261/artifacts/12654148709).
- RulesTest, OrderFiltersTest und GalleryFiltersTest prüfen Lieferungen ohne KT-Pflicht, Suche nach Auftrags-/Containernummer statt alter KT-Werte, Anzeige der Auftragsreferenz und TXT mit Auftragsnummer statt KT.
- NavigationTest legt einen Lieferauftrag ohne KT-Feld an. ReferenceScreenshotsTest navigiert anhand der Auftragsreferenzen durch Übersicht, Galerie, Fotoprüfung und Übertragung.
- PersistenceTest öffnet einen alten Datensatz mit gespeichertem KT-Wert erneut: Fotos, Erinnerung und Altwert bleiben erhalten; die Anzeige nutzt die Auftragsreferenz. Neue Aufträge speichern KT leer. Room-Schema und Datenbankversion bleiben unverändert bei 2.
- ExportSharingTest prüft Exporte eines Datensatzes mit altem KT-Wert: kein kt-Feld in META.json, Export-schemaVersion 2, Auftragsreferenz statt KT im TXT. Vorhandene Archivdateien werden nicht umgeschrieben. Export-/Completion-/Reminder-Effekte bleiben einmalig.
- Foto-Begleittext, Kopierfunktion, nummerierte JPG-Namen, temporäre Leserechte, Android-Fallback und deaktivierte automatische Graph-Uploads bleiben durch die bestehenden Regressionen geprüft. Keine Benutzerverwaltung, PINs oder Rollen.
- Das lokal erhaltene APK wurde am 28.10.2026 erneut als `ch.zerogen.containertrack`, Code 11, Version 1.4.4 identifiziert. Die erhaltenen XML-Unit-Berichte bestätigen 41 Tests je Variante ohne Fehler oder übersprungene Tests; dies ist keine erneute Testausführung.

Unabhängiger statischer Code-Review für `0316211..f45788e`: keine kritischen oder wichtigen Restbefunde. Der Review bestätigt keine eigene Geräte- oder Teams-Prüfung.

## Offene Sichtprüfung und Entwicklungszugriff

Die Screenshots wurden im erfolgreichen CI-Lauf erzeugt, aber für 1.4.4 noch nicht manuell visuell geprüft. Die temporär heruntergeladene Übersicht ging bei der Wiederaufnahme der Entwicklungsumgebung verloren. Der erneute Abruf von `api.github.com` wird am 28.10.2026 durch die aktuelle Netzwerk-Positivliste blockiert. Normaler Git-Zugriff funktioniert. Die API-Freigabe ist im Entwicklungsentwurf bereits vorgesehen, aber nicht in der laufenden Umgebung aktiv. Der APK-Link und die Testergebnisse oben wurden vor dieser Unterbrechung bestätigt; kein erneuter API-Abruf wird behauptet.

Die bisherige visuelle Übersicht [1.4.3](../design/verification/reference-ui-v1.4.3.jpg) dokumentiert ausdrücklich den älteren Stand und gilt nicht als Bildnachweis für 1.4.4. Kein automatischer Pixelvergleich; keine freigegebenen Goldens vorhanden. Lange Auftragsreferenzen und große Systemschriften benötigen weiter Geräteprüfung.

Eine finale PR-Beschreibung ist außerhalb des Checkouts unter `/workspace/.containertrack-setup/pr-1.4.4.json` vorbereitet. Das Aktualisieren der GitHub-PR-Metadaten bleibt bis zum API-Zugriff offen; Code und Dokumentation werden über den normalen Git-Weg gespeichert. Kein Merge oder Release.

## Weitere Grenzen

Teams bestimmt seine Ziele und kann bei Fotos den Nachrichtentext ignorieren oder nur Chats anbieten. Kopieren der Auftragsinfos und der manuelle Kanalablauf bleiben verfügbar. Die konkrete „Shared Photos“-Anzeige in Teams ist ohne Rückmeldung zum Gruppen-/Dateinamen weiter ungeklärt. Die App meldet nummerierte JPG-Namen; eine Veränderung durch Teams wird nicht als behoben behauptet.

Reale Teams-Ziele, Textübernahme und Dateinamen, SAF-Provider, Kamera/OEM und TalkBack benötigen Prüfung auf dem Nutzergerät. Eine Update-Installation benötigt dasselbe Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Nicht zur Aktualisierung deinstallieren: das würde lokale Aufträge und Fotos löschen.

Details: [UPDATE_1_4_4.md](UPDATE_1_4_4.md). Die Android-App benötigt keinen Cloud-Dienst; die erwähnte Netzwerkfreigabe betrifft ausschließlich die Codex-Entwicklungsumgebung.
