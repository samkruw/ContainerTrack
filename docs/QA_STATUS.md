# Prüfstatus — Update 1.4.1 — 23.10.2026

Aktueller Auftrag: lokale Benutzerverwaltung, PINs und Rollen entfernen; direkter Start, alte Daten behalten, Teams-Teilen unverändert. Lokale Prüfungen und GitHub-Geräteprüfung laufen. Der folgende Nachweis beschreibt ausschließlich den vorherigen Stand; er bestätigt noch nicht Version 1.4.1.

# Prüfstatus — Update 1.4.0 — 23.10.2026

Repository [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Branch `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Nur Debug-APK; kein Merge, kein Release.

## Nachweis

[GitHub-Lauf 39905918718](https://github.com/samkruw/ContainerTrack/actions/runs/39905918718) vollständig erfolgreich. Geprüfter App-/Test-Commit: `ed7340c267bcef8a6bc670df3dc27c6e01a75aeb`. Nachfolgende Dokumentationsänderungen verändern diesen App-Stand nicht.

[Debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39905918718/artifacts/12481320545): ZIP mit `app-debug.apk`, Version 1.4.0 / versionCode 7. Artefakte haben begrenzte Aufbewahrungszeit.

- 42 Unit-Tests je Debug/Release ohne Fehler oder übersprungene Tests. Lint, Debug-APK, Android-Test-APK, Berechtigungsprüfung und neun Python-QA-Tests erfolgreich.
- 67 Android-Tests erfolgreich, keine übersprungenen Tests. 25 Screenshot-PNGs erzeugt, darunter alle acht vollständigen nativen Ansichten. Die finale Übersicht wurde tatsächlich geöffnet und visuell geprüft: [Bildschirmnachweis mit Testdaten](../design/verification/reference-ui-v1.4.0.jpg).
- Lokale Profile überstehen einen Store-/Prozessneustart; die Anmeldung bleibt ausschließlich im laufenden Prozess. PINs werden gesalzen gehasht, Fehlversuchssperren persistiert. Tests prüfen falsche PINs, PIN-Wechsel/-Reset, doppelte Namen, letzten Admin, Deaktivierung und unzulässige Mitarbeiter-Administration.
- Reale Repository-Zugriffe prüfen Eigentümer, ausgewählte Foto-IDs, fremdes Teilen/Löschen, leere Sicht nach Logout und Admin-Zugriff auf vorhandene Aufträge. Editor-Test legt einen Benutzer an und deaktiviert ihn; echte MainActivity prüft Logout/PIN-Login und Navigation inklusive Activity-Neuerstellung.
- Benachrichtigungsaktionen öffnen die App. Direkte alte CONFIRM-Aufrufe als fremder Mitarbeiter oder ohne Anmeldung verändern keine Reminder. Hinweise enthalten keine Auftragsreferenz.
- Vollständiger Abschluss öffnet Teams-Sharing ohne Firmenanmeldung. Unvollständige Aufträge können den Abschluss nicht erzwingen. Dateien haben temporäre Lesefreigaben; bei fehlendem Teams-Empfänger öffnet sich Android-Teilen. Übergabe setzt keine Upload-Bestätigung und verändert Completion-/Timer-Marker nicht.
- Neue lokale Aufträge erzeugen keine automatische Upload-Queue; alte Worker senden nicht mehr. Historische Daten bleiben erhalten. Explizite Room-Migration 1→2, Schema 2 unverändert, echte Offline-OCR von 0708, zweiphasige Delivery mit sechs Pflichtfotos, Return-Gate, ZIP-Export und Logger-Timer weiterhin geprüft.

Unabhängiger statischer Review: keine wichtigen Restbefunde nach Korrektur von Reminder-Zugriff und unpassenden Transport-Warteanzeigen. Die Instrumentation authentifiziert vor jedem Test einen echten, ausschließlich testseitig angelegten lokalen Admin. Das ausgelieferte APK enthält keine Standardbenutzer oder Standard-PIN. Die synthetische Containerabbildung liegt ausschließlich in androidTest/assets. Kein behaupteter automatischer Pixelvergleich; es gibt keine freigegebenen Goldens.

## Grenzen und Hardware-Abnahme

Konten und Rollen gelten ausschließlich für die jeweilige App-Installation. Es gibt keine zentrale Anmeldung, Team-Galerie oder Synchronisierung zwischen Geräten. Rollen vergeben keine Microsoft-/Teams-Rechte. Eigene Teams-Anmeldung, angebotene Empfänger/Kanäle, reale SAF-Provider und Kamera/OEM/TalkBack-Verhalten müssen auf dem Nutzergerät geprüft werden. Ein lokaler PIN-Schutz ersetzt keinen Betriebssystemschutz auf einem kompromittierten Gerät.

Die letzte vergessene Admin-PIN kann nicht über einen Online-Dienst zurückgesetzt werden. Eine Update-Installation benötigt dasselbe Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Keine Deinstallation als Update empfehlen: sie würde lokale Daten und Profile löschen.

Vollständiger Ablauf und Voraussetzungen: [UPDATE_1_4_0.md](UPDATE_1_4_0.md). Der alte Microsoft-Adapter bleibt als deaktivierter Kompatibilitätscode erhalten; dessen Konfiguration ist für diesen Ablauf nicht erforderlich. Direkter Artefakt-Download in die Cloud kann durch den Proxy eingeschränkt sein; der Bildschirmnachweis wurde aus dem regulären CI-Protokoll gewonnen.
