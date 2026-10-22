# Prüfstatus — Update 1.3.2 — 22.10.2026

Repository [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Branch `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Nur Debug-APK; kein Merge, kein Release.

## Nachweis

[GitHub-Lauf 39843067438](https://github.com/samkruw/ContainerTrack/actions/runs/39843067438) ist vollständig erfolgreich. Geprüfter App-/Test-Commit: `98c3e4e1768e3feefb3072efb1fa9a757e6420a8`. Nachfolgende Dokumentationsänderungen verändern diesen App-Stand nicht.

[Debug-APK herunterladen](https://github.com/samkruw/ContainerTrack/actions/runs/39843067438/artifacts/12454336918). Das Artefakt enthält `app-debug.apk`, Version 1.3.2 / versionCode 6. GitHub-Artefakte haben begrenzte Aufbewahrungszeit.

- 39 Unit-Tests je Debug/Release; keine Fehler oder übersprungenen Tests. Lint, Debug-APK, Android-Test-APK und neun Python-QA-Tests erfolgreich.
- 58 Android-Tests erfolgreich, keine übersprungenen Tests; 25 Screenshot-PNGs erzeugt. Weitere Artefakte: Android-reports und Android-device-reports-and-screenshots.
- Reale Room-Migration 1→2 bewahrt Aufträge, Foto-Pfade und Reminder ohne historischen Massen-Upload. Schema 2 bleibt unverändert.
- Gebündelte ML-Kit-Erkennung von 0708 bei deaktiviertem validierten Netzwerk. Führende Null des optionalen Erstcontainers bleibt erhalten; Job und Erstcontainer werden atomar gespeichert.
- Zweiphasige Delivery, sechs Pflichtfotos, einmaliger Completion-/Timer-/Queue-Effekt, Return, Konto-Bindung, per-Foto-Bestätigung, partieller Retry, persistiertes Retry-After und Fotoersatz geprüft.
- Echte lokale JPEG-Galerie, Vollbild, Auswahl/Share-ID, ausgewählte Originalbytes, Cache-Bereinigung, ZIP-Chooser, Export, Alarm-Aktionen und zehn Container bei großer Schrift geprüft.

## Manuelles Teams-Teilen

„In Teams teilen“ öffnet den Teams-JPEG-Empfänger direkt, ohne ContainerTrack-Firmenkonfiguration. Galerie: ausschließlich ausgewählte Fotos; Übertragungsansicht: vorhandene Auftragsfotos. Bei fehlendem Teams-Empfänger wird Android-Teilen geöffnet. Tests prüfen Direktstart, Fehler-Fallback mit unveränderten URI-Leserechten, Originalbytes und Galerie-Button/Foto-IDs. Der Button ist im echten unverbundenen Upload-Screen aktiv. Timer und bestätigter Uploadstatus bleiben unverändert. Die konkrete Empfänger-/Kanalauswahl in der installierten Teams-App muss auf dem Nutzergerät geprüft werden; ContainerTrack erzwingt keine Kanalliste.

## Visueller Abgleich

Alle acht ausgewählten Referenzansichten wurden im echten MainActivity inklusive Markenleiste/Navigation erfasst und manuell mit den gelieferten Bildern verglichen. [Bildschirmnachweis mit synthetischen Testdaten](../design/verification/reference-ui-v1.3.2.jpg), [Zuordnung und fachliche Unterschiede](../design-qa.md).

Graphit-/Lime-Palette, Inter mit echten variablen Schriftgewichten, flaches Container-Logo, große KT-/Container-Zahlen, rechteckige Aktionen, Trennlinien, Foto-Raster und drei Tabs mit ausgewähltem Unterstrich sind umgesetzt. Die Testabbildung ist ausschließlich in androidTest/assets; sie ist nicht im Produkt-APK enthalten. Keine freigegebenen Goldens: manueller Vergleich, kein behaupteter automatischer Pixelvergleich.

Der vorangegangene Designstand erhielt einen unabhängigen statischen Review ohne verbleibende wichtige Befunde nach Korrektur der Share-Cache-Bereinigung. Original-JPG-Speichern über Androids Ordnerauswahl ist implementiert und statisch geprüft; eine reale SAF-Provider-/Hardware-Abnahme steht aus.

## Noch nicht abgenommen

- Echte Microsoft-Anmeldung und Graph-Übertragung ins Firmen-Team: öffentliche Tenant-/Client-/Redirect-/Team-/Channel-Konfiguration fehlt. Keine simulierte Anmeldung oder Übertragung. Einrichtung: [MICROSOFT_SETUP.md](MICROSOFT_SETUP.md).
- Zentraler Team-Galerie-Zugriff und administrative Benutzerverwaltung: ohne verifizierte Rechte keine erfundenen Benutzer oder Rollen. Die vorhandene Galerie ist ausdrücklich lokal.
- Update-Installation über bestehender APK mit identischem Debug-Zertifikat; ein frischer Runner garantiert dies nicht. Keine Deinstallation zum vermeintlichen Update empfehlen.
- Reale Kamera/Licht/Fotoqualität, Standby/Boot/OEM-Verhalten, TalkBack und vollständige Hardware-Abnahme.

INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK sind vorgesehen. Keine Standort-/Mikrofon-/NFC-/breiten Speicherrechte. Direkter Artefakt-Download in die Cloud ist durch den Netzwerk-Proxy eingeschränkt; die Bildschirmübersicht wurde aus dem regulären CI-Protokoll extrahiert. Der GitHub-Download-Link bleibt für berechtigte Benutzer verfügbar.
