# ContainerTrack 1.3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die bestehende native App erhält Delivery in zwei Phasen, einen einmaligen Completion-Timer, echten Microsoft-Foto-Sync und Terminal Dark; daraus entsteht eine geprüfte Debug-APK.

**Architecture:** Bestehende Room-/Repository-/CameraX-/OCR-/Alarm-Bausteine bleiben erhalten. Neue Queue, WorkManager und gekapselte MSAL-/Graph-Adapter ergänzen die App; Compose konsumiert echte lokale und bestätigte Remote-Zustände. Drei aufeinander aufbauende Teilpläne liefern jeweils überprüfbare Software.

**Tech Stack:** Kotlin 2.2.21, Compose BOM 2025.10.01, Room 2.8.3, CameraX 1.5.1, SDK 36/minSdk 26, JDK 17, Gradle 8.13; ergänzend stabile kompatible MSAL-Android-/WorkManager-Artefakte, nach Prüfung der offiziellen Dokumentation exakt gepinnt.

**Spec:** [Freigegebene technische Spezifikation](../specs/2026-10-22-containertrack-v1-3-design.md), freigegeben durch „Weiter“ am 22.10.2026.

## Global Constraints

- Vorhandenen isolierten Checkout `/workspace/ContainerTrack` / `codex/native-android-app` verwenden; kein zusätzlicher Worktree.
- App-ID `ch.zerogen.containertrack`; V1.3-Handoff hat Vorrang vor der alten PWA-/Fünf-Foto-UI-Vorgabe.
- Delivery genau sechs Fotos, Return genau ein Inhaltsfoto; Nummern vierstellige Strings mit führenden Nullen.
- Timer 30 Minuten nach erstem vollständigem Delivery-Auftrag, niemals durch Retry/Share/Export neu starten.
- Teams-Ziel `Op530-Lal`, ausschliesslich nummerierte JPGs; ZIP/TXT/META bleiben lokaler Export.
- Room-Migration ohne Datenverlust; kein destructive fallback; bestehende Reminder-ID/Frist/Bestätigung erhalten.
- MSAL Public Client; keine Passwörter, Client-Secrets, Tokenlogs oder privaten Schlüssel im Repository.
- Microsoft-Konfiguration und Firmenfreigabe fehlen bislang; baubare Disconnected-Zustände ersetzen keinen echten Uploadnachweis.
- Debug-APK 1.3.0 / versionCode 4. Keine Release-Signatur als Arbeitsblocker; MSAL-Redirect muss dennoch zur tatsächlich gebauten Signatur passen.
- Nur echte Produktdaten und bestätigte Uploads anzeigen; keine simulierten Konten, Rollen oder Team-Galerien.

## Review Focus

1. Tod direkt nach DB-Commit: Reminder und Queue müssen nach Neustart wieder aufgenommen werden — Teilplan A, Task 3.
2. Foto ersetzt während eines Uploads: ein alter Datei-Erfolg darf die neue Version nicht bestätigen — Teilplan B, Task 3.
3. Kontowechsel bei wartenden Jobs: altes Kontoeigentum bleibt erhalten, kein Upload unter einem fremden Benutzer — Teilplan B, Tasks 1/3.
4. Lange/sonderzeichenhaltige Referenzen und wiederholte Dateinamen: sichere URI-/Remote-Pfade, keine doppelten Remote-Dateien — Teilplan B, Tasks 2/4.
5. Viele Container, grosse Schrift und Rücknavigation: Foto 6 bleibt erreichbar, Such-/Filterzustand und echte Counts bleiben erhalten — Teilplan C, Tasks 1/2.

## Reihenfolge und Schnittstellen

1. [Teilplan A — Delivery, Completion und Migration](2026-10-22-containertrack-v1-3-a-core.md).
2. [Teilplan B — Microsoft, Queue und Foto-Share](2026-10-22-containertrack-v1-3-b-sync.md).
3. [Teilplan C — Terminal Dark und Auslieferung](2026-10-22-containertrack-v1-3-c-ui.md).

Alle neuen Kotlin-Dateien liegen unter `app/src/main/java/ch/zerogen/containertrack/`, Tests entsprechend unter `app/src/test/java/` oder `app/src/androidTest/java/` mit demselben Paket. Teilpläne benutzen diese Basisverzeichnisse als `main/`, `unit/`, `androidTest/`.

## Kommandos und Nachweise

Standard: `./gradlew test lint assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2` aus dem Checkout. In dieser Cloud-Instanz aktiviert `python3 /workspace/.containertrack-setup/run-gradle.py ...` JDK/SDK/Proxy/System-CA. Wenn die Sandbox lokale Gradle-Sockets blockiert, den vorgesehenen Berechtigungsweg verwenden; niemals TLS/Checksummenprüfung abschalten.

Emulator: `./gradlew connectedDebugAndroidTest --no-daemon --stacktrace`. Lokale ADB-Konfiguration ist hier schreibgeschützt, KVM fehlt; die bestehende GitHub-CI ist der nachgewiesene Emulatorweg. Lokale Unit-Tests, Kompilierung und Instrumentation-APK müssen trotzdem ausgeführt werden. Kein Null-Test-Lauf gilt als Erfolg.

Baseline 1.2: 14 Unit-Tests je Debug/Release, Lint und Debug-APK am 22.10.2026 erfolgreich. Historischer CI-Lauf 39568256671 bestätigt 33 Instrumentationstests und 14 übernommene Screenshots für Codecommit 7a3e8f4; dieser Nachweis ersetzt keine V1.3-Prüfung.

## Abschluss

- [ ] Alle drei Teilpläne abarbeiten; Tests pro Änderung erst rot sehen, dann minimal implementieren, dann grün verifizieren.
- [ ] Kleine fachliche Commits; keine Secrets und keine geschützten echten Firmenfotos als Testfixtures.
- [ ] Frische unabhängige Gesamtprüfung; wichtige Befunde beheben und relevante Checks wiederholen.
- [ ] Bestehenden Draft-PR aktualisieren und geprüfte Commits auf denselben Branch pushen; kein Merge und keine Release-Veröffentlichung.
- [ ] CI-Ergebnis, tatsächliche Testzahlen, APK-Artefakt, Screenshots und offene Firmen-/Hardwareabnahme berichten.

**Empfohlene Ausführung:** direkt in dieser Sitzung (`superpowers:executing-plans`), mit unabhängiger Gesamtprüfung zum Schluss. Die Abschnitte teilen Repository-, Room- und ViewModel-Schnittstellen und sind deshalb überwiegend voneinander abhängig. Alternativ ist eine Umsetzung mit frischen Teilagenten und Reviews je Task möglich.
