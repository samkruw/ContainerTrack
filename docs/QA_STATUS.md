# Prüfstatus — Update 1.3.0 — 22.10.2026

Repository [samkruw/ContainerTrack](https://github.com/samkruw/ContainerTrack), Branch `codex/native-android-app`, [Draft-PR #1](https://github.com/samkruw/ContainerTrack/pull/1). Nur Debug-APK; kein Merge, kein Release.

## Prüfungen

Vollständiger aktueller GitHub-Lauf: [39804758790](https://github.com/samkruw/ContainerTrack/actions/runs/39804758790), Ergebnis bei Erstellung dieses Dokuments noch offen. Artefakte: `Container-Track-debug-APK`, `Android-reports`, `Android-device-reports-and-screenshots`.

38 reine Kotlin/JUnit-Tests lokal bestanden; sieben Python-QA-Tests sowie Quell- und Merged-Manifest-Prüfungen bestanden. Vollständiger Gradle-Build inklusive Unit-Tests (36 pro Variante vor zwei zusätzlichen Retry-/Konfigurationstests), Lint und Android-Test-APK lokal erfolgreich. Lokale Emulator-Ausführung wird nicht als bestanden gezählt (kein KVM/writable ADB-Konfigurationspfad).

Der erste V1.3-Lauf 39799206863 kompilierte MSAL/App und führte 37 Android-Tests aus, ohne übersprungene Tests; zwei scheiterten. Korrigiert: alte Offline-Berechtigungsannahme durch echten Netz-aus-OCR-Test ersetzt; Serialization-Versionen für Room-Migration angeglichen. Unnötiges transitives NFC-Recht explizit entfernt. Dieser Lauf gilt ausdrücklich nicht als erfolgreicher V1.3-Nachweis.

Neue Tests prüfen zweiphasige Lieferung über drei Container, einmaligen Completion-/Timer-/Queue-Effekt, Return, echte Room-1→2-Migration, per-Foto-Bestätigung über Wiederöffnung, falsches Konto, partiellen Retry und Fotoersatz. Native Navigation, Activity-Neustart, manuelle JPEG-Freigaben und ZIP-Chooser bleiben geprüft. Screenshot-Szenarien umfassen die neue Auftragsliste, fehlendes Befüllt-Foto, Upload-Fehler, leere lokale Galerie und Profil.

Unabhängiger statischer Abschlussreview: keine P1-Befunde; zwei P2 behoben (dauerhafte Fehler nur explizit erneut versuchen; installierten Teams-Empfänger vor Angebot prüfen).

## Noch nicht abgenommen

- Echte Microsoft-Anmeldung und Graph-Übertragung ins Firmen-Team: öffentliche Tenant-/Client-/Redirect-/Team-/Channel-Konfiguration fehlt. Die App zeigt diesen Zustand ehrlich; keine Testdaten oder simulierte Erfolge in Produktion. Einrichtung und Abnahme: [MICROSOFT_SETUP.md](MICROSOFT_SETUP.md).
- Update-Installation über bestehender APK mit identischem Debug-Zertifikat; bei frischem Runner nicht automatisch gewährleistet. Keine Deinstallation zum vermeintlichen Update empfehlen.
- Reale Kamera/Licht/Fotoqualität, Standby/Boot/OEM-Verhalten und Benachrichtigungsfreigaben auf Hardware.
- TalkBack, große Schrift und visuelle Freigabe. Screenshots sind Prüfartefakte, keine freigegebenen Goldens.

INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK sind absichtlich für Microsoft vorhanden. Keine Standort-/Mikrofon-/NFC-/breiten Speicherrechte. Quellen-Gate und APK-Permission-Gate sind getrennt; keine Behauptung einer technischen Netzwerksperre für Bibliotheken.
