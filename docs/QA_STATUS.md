# Update 1.5.0 — laufende Verifikation

Neue Return-Phasen, feste 24h-Erinnerung und vollständige Auftrags-E-Mail sind implementiert. Lokale Prüfung: 50 Unit-Tests je Variante, neun Python-QA-Tests, Lint, Debug-/Test-APK-Kompilierung und APK-Berechtigungsprüfung bestanden. Die neuen Android-Tests sind kompiliert, aber ihr Lauf und die Sichtprüfung noch nicht abgeschlossen. Ein lokaler Software-Emulator startet; der GitHub-API-Zugriff bleibt durch die aktive Entwicklungs-Netzwerkregel gesperrt.

Unabhängiger statischer Review des neuen Repository-/Reminder-/Migrationsablaufs: keine wesentlichen Restbefunde. Keine behauptete echte Mail-App-Zustellprüfung. Weitere Ergebnisse werden hier nach tatsächlicher Ausführung ergänzt.

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
