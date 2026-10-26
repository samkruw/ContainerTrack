# Update 1.4.4 — Prüfung läuft

KT entfällt vollständig aus Erfassung, Pflichtprüfung, Suche, Anzeige und Export. Room bleibt Schema 2; alte Daten bleiben erhalten. Der folgende Nachweis beschreibt noch Version 1.4.3.

# Prüfstatus — Update 1.4.3 — 23.10.2026

Geprüfter App-/Test-Commit: `b19ff733d8d763dc008e894795938a1aea9256ce`. [GitHub-Lauf 39915194786](https://github.com/samkruw/ContainerTrack/actions/runs/39915194786): Build und Gerätetests erfolgreich. Spätere Dokumentations-Commits ändern weder App noch Tests.

[Debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39915194786/artifacts/12484842232): ZIP mit `app-debug.apk`, Version 1.4.3 / versionCode 10. Artefakte haben begrenzte Aufbewahrungszeit.

- 41 Unit-Tests je Variante erfolgreich, keine Fehler oder übersprungenen Tests. Die angepasste Textregression ohne KT zunächst fehlgeschlagen, anschließend bestanden. Neun Python-QA-Tests, Lint, Debug-/Instrumentation-Kompilierung sowie Manifest-/APK-Berechtigungsprüfung bestanden.
- 60 Android-Tests auf API 35 erfolgreich, keine übersprungenen Tests. 24 Screenshot-PNGs erzeugt; sieben aktuelle Ansichten in der [visuell geprüften Übersicht](../design/verification/reference-ui-v1.4.3.jpg).
- Nachrichtentext enthält Auftrag, ausgewählte Container, Typ, tatsächliche Fotoanzahl und bei Return Quick/Zusatzarbeiten. Repository-Tests prüfen EXTRA_TEXT bei ganzen Aufträgen und ausgewählten Fotos; ungewählte Container werden nicht beschrieben. Der Android-Fallback behält Begleittext und Leserechte.
- Reale Compose-Klicks prüfen die Systemzwischenablage in Galerie und Übertragungsansicht: passende Auftrags-/Containerdaten ohne KT und tatsächliche Fotoanzahl. Kopieren erfolgt nur nach explizitem Tastendruck; die Galerieaktion ist ohne Auswahl deaktiviert. Der vollständige Text ist in der Übertragungsansicht sichtbar.
- Navigation, alte lokale/Microsoft-/leere Eigentümerkennungen, vollständige Fotoabläufe, Export, Migration, OCR, Erinnerungen und Activity-Neuerstellung bleiben geprüft. Keine Anmeldung, PINs oder Rollen. Vorhandene Daten bleiben erhalten, neue automatische Upload-Queues und alte Graph-Worker deaktiviert.
- Teilen oder Kopieren bestätigt keinen Versand und ändert keine Completion-/Reminder-Effekte. Das lokal erzeugte APK wurde als `ch.zerogen.containertrack`, Code 10, Version 1.4.3 geprüft.

Die native Screenshot-Übersicht wurde tatsächlich geöffnet und visuell geprüft. Sie enthält ausschließlich synthetische Testdaten. Galeriebuttons passen nebeneinander; keine zusätzliche volle Buttonzeile im Fotobereich. Kein automatischer Pixelvergleich: dafür liegen keine freigegebenen Goldens vor.

Zusätzlicher Dateinamen-Nachweis: PhotoSharingTest fragt für mehrere Fotoanhänge die tatsächlichen Android-ContentProvider-Werte `DISPLAY_NAME` ab und prüft die URI-Dateinamen gegen `0708_01.jpg`/`0708_02.jpg`. Beide Prüfungen sind bestanden. Der private Cacheordner heißt `shared-photos`, wird aber nicht als Dateiname gemeldet. Die konkrete „Shared Photos“-Anzeige innerhalb von Teams wurde damit nicht reproduziert oder geändert; Gruppenüberschrift gegenüber tatsächlich umbenannter Datei bleibt ohne Rückmeldung vom Nutzergerät ungeklärt.

KT entfällt aus Foto-Begleittext, Kopierinfos und Übertragungsansicht. Das KT-Feld in Erfassung/Übersicht und gespeicherte KT-Werte bleiben erhalten; es wurde keine vollständige Entfernung aus dem Datenmodell durchgeführt.

## Grenzen

Die Tests prüfen die Übergabe an Teams/Android, nicht die interne Darstellung in einer echten Teams-Installation. Teams kann bei Fotos den Nachrichtentext ignorieren und beim Teilen nur Chats anbieten. ContainerTrack kann die Teams-Zielauswahl nicht erzwingen. Der ausdrückliche Kopierweg bleibt verfügbar. Für Kanalbeiträge: Fotos in der Galerie herunterladen, Teams direkt öffnen und Fotos sowie kopierte Infos dort einfügen. Die frühere Zusage einer Kanalauswahl im Share-Dialog ist aufgehoben.

Es gibt keine zentrale Anmeldung, Rollenverwaltung, Team-Galerie oder Synchronisierung zwischen Geräten. Reale Teams-Ziele, Textübernahme, SAF-Provider, Kamera/OEM und TalkBack benötigen Prüfung auf dem Nutzergerät. Der Teilen-Dialog bestätigt keinen Versand.

Eine Update-Installation benötigt dasselbe Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Nicht zur Aktualisierung deinstallieren: das würde lokale Aufträge und Fotos löschen. Die Datenbank bleibt Schema 2, ohne destruktive Migration.

Details: [UPDATE_1_4_3.md](UPDATE_1_4_3.md). Der frühere Microsoft-Adapter bleibt als deaktivierter Kompatibilitätscode erhalten; für diesen Ablauf ist keine Firmenkonfiguration nötig. Der Bildschirmnachweis wurde aus dem regulären CI-Protokoll gewonnen.
