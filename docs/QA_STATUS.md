# Update 1.4.3 — Prüfung läuft

KT entfällt aus Foto-Begleittext, Kopiertext und Übertragungsansicht. Der Dateiname wird zusätzlich am Android-ContentProvider geprüft. Der folgende Nachweis beschreibt noch Version 1.4.2.

# Prüfstatus — Update 1.4.2 — 23.10.2026

Geprüfter App-/Test-Commit: `f45872cd434b105cba383da751ab89921190702e`. [GitHub-Lauf 39913259080](https://github.com/samkruw/ContainerTrack/actions/runs/39913259080): Build und Gerätetests erfolgreich. Spätere Dokumentations-Commits ändern weder App noch Tests.

[Debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39913259080/artifacts/12484590109): ZIP mit `app-debug.apk`, Version 1.4.2 / versionCode 9. Artefakte haben begrenzte Aufbewahrungszeit.

- 41 Unit-Tests je Variante erfolgreich, keine Fehler oder übersprungenen Tests. Zwei neue Textregressionen zunächst fehlgeschlagen, anschließend bestanden. Neun Python-QA-Tests, Lint, Debug-/Instrumentation-Kompilierung sowie Manifest-/APK-Berechtigungsprüfung bestanden.
- 60 Android-Tests auf API 35 erfolgreich, keine übersprungenen Tests. 24 Screenshot-PNGs erzeugt; sieben aktuelle Ansichten in der [visuell geprüften Übersicht](../design/verification/reference-ui-v1.4.2.jpg).
- Nachrichtentext enthält Auftrag, Delivery-KT, ausgewählte Container, Typ, tatsächliche Fotoanzahl und bei Return Quick/Zusatzarbeiten. Repository-Tests prüfen EXTRA_TEXT bei ganzen Aufträgen und ausgewählten Fotos; ungewählte Container werden nicht beschrieben. Der Android-Fallback behält Begleittext und Leserechte.
- Reale Compose-Klicks prüfen die Systemzwischenablage in Galerie und Übertragungsansicht: passende Auftrags-/KT-/Containerdaten und tatsächliche Fotoanzahl. Kopieren erfolgt nur nach explizitem Tastendruck; die Galerieaktion ist ohne Auswahl deaktiviert. Der vollständige Text ist in der Übertragungsansicht sichtbar.
- Navigation, alte lokale/Microsoft-/leere Eigentümerkennungen, vollständige Fotoabläufe, Export, Migration, OCR, Erinnerungen und Activity-Neuerstellung bleiben geprüft. Keine Anmeldung, PINs oder Rollen. Vorhandene Daten bleiben erhalten, neue automatische Upload-Queues und alte Graph-Worker deaktiviert.
- Teilen oder Kopieren bestätigt keinen Versand und ändert keine Completion-/Reminder-Effekte. Das lokal erzeugte APK wurde als `ch.zerogen.containertrack`, Code 9, Version 1.4.2 geprüft.

Die native Screenshot-Übersicht wurde tatsächlich geöffnet und visuell geprüft. Sie enthält ausschließlich synthetische Testdaten. Galeriebuttons passen nebeneinander; keine zusätzliche volle Buttonzeile im Fotobereich. Kein automatischer Pixelvergleich: dafür liegen keine freigegebenen Goldens vor.

## Grenzen

Die Tests prüfen die Übergabe an Teams/Android, nicht die interne Darstellung in einer echten Teams-Installation. Teams kann bei Fotos den Nachrichtentext ignorieren und beim Teilen nur Chats anbieten. ContainerTrack kann die Teams-Zielauswahl nicht erzwingen. Der ausdrückliche Kopierweg bleibt verfügbar. Für Kanalbeiträge: Fotos in der Galerie herunterladen, Teams direkt öffnen und Fotos sowie kopierte Infos dort einfügen. Die frühere Zusage einer Kanalauswahl im Share-Dialog ist aufgehoben.

Es gibt keine zentrale Anmeldung, Rollenverwaltung, Team-Galerie oder Synchronisierung zwischen Geräten. Reale Teams-Ziele, Textübernahme, SAF-Provider, Kamera/OEM und TalkBack benötigen Prüfung auf dem Nutzergerät. Der Teilen-Dialog bestätigt keinen Versand.

Eine Update-Installation benötigt dasselbe Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Nicht zur Aktualisierung deinstallieren: das würde lokale Aufträge und Fotos löschen. Die Datenbank bleibt Schema 2, ohne destruktive Migration.

Details: [UPDATE_1_4_2.md](UPDATE_1_4_2.md). Der frühere Microsoft-Adapter bleibt als deaktivierter Kompatibilitätscode erhalten; für diesen Ablauf ist keine Firmenkonfiguration nötig. Der Bildschirmnachweis wurde aus dem regulären CI-Protokoll gewonnen.
