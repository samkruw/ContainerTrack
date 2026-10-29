# Return und E-Mail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Return-Zusatz-/Abschlussfotos mit persistenter 24h-Erinnerung und vollständiger E-Mail-Vorbereitung.
**Architecture:** Native Compose/Room-Erweiterung, lokale Revision und explizite Versandbestätigung. Mailziele aus SENDTO/mailto registrierten Apps, JPEG-Anhänge via FileProvider.
**Tech Stack:** Kotlin, Compose, Room3, AlarmManager, Android-Mail-Intents.
**Spec:** docs/superpowers/specs/2026-10-28-return-email-design.md

## Global Constraints

- Kein KT, keine Benutzer/Rollen, kein eigener Online-Dienst und keine aktiven Teams-Aktionen.
- Lieferung sechs Fotos, Return gleiche INITIAL-/CLEAN-Anzahl pro Container, Inhaltsfoto erforderlich.
- Return-Frist createdAt + 86400000, Ende nur durch manuelle Versandbestätigung für die aktuelle Revision.
- Bestehende Daten erhalten; neue Room-Migration 2→3 und alte abgeschlossene Returns kompatibel.
- Nur Debug-APK, keine automatische Veröffentlichung/kein Merge.

## Review Focus

- Migration offener/abgeschlossener Alt-Returns: Bestandsdaten und Fristen bewahren.
- Späte Zusatzfotos/Retakes: alte Mailvorbereitung ungültig machen.
- Keine Mail-App: keine falsche Versandbestätigung und kein Teams-Fallback.
- Mehrere Container: alle benannten Anhänge mit exakt einem Auftragsbetreff.
- Neustart/verweigerte Alarmrechte: ausstehende Erinnerung bleibt dauerhaft gespeichert und sichtbar.

### Task 1: Datenmodell und Regeln
- [x] Pure ReturnProgress-/Dateinamentests schreiben; Datenbankmigration testen.
- [x] Enums INITIAL/CLEAN, CONTENT/DAMAGE/OTHER/CLEAN und ReminderKind ergänzen.
- [x] Job-Mailrevision, Photo-Phasen, neue Return-Phasen und Migration implementieren.
- [x] Pure Tests sowie neue Room-Schemas prüfen.

### Task 2: Repository und Erinnerungen
- [x] ReturnWorkflowRepositoryTest für 1+2 → 3, alle Container und feste Frist.
- [x] saveReturnPhoto/deleteReturnPhoto, prepareOrderEmail und confirmEmailSent implementieren.
- [x] Revision bei jeder Anhangsänderung erhöhen; verspätete Bestätigung verhindern.
- [x] Return-Erinnerung erstellen/wiederherstellen; LOGGER-Bestätigung darf Return nicht umgehen.

### Task 3: E-Mail und native Oberfläche
- [x] E-Mail-Vertragstests für Betreff, alle Anhänge, Dateinamen, Leserechte und fehlende Mail-App.
- [x] emailPhotoIntent und ausschließlich Mail-Handler implementieren.
- [x] Return-Fotomaske, Zähler, E-Mail-Vorbereitung/Bestätigung und nicht blockierende Return-Erinnerung integrieren.
- [x] Bestehende Android-Regressionen an neue Return-/Mail-Regeln anpassen.

### Task 4: Verifikation und Debug-Ausgabe
- [x] test lint assembleDebug assembleDebugAndroidTest mit vorhandenem Gradle-Helper.
- [x] QA-/Berechtigungsprüfung und unabhängiger Review.
- [x] Bestehenden Branch-Push für GitHub-Build nutzen; API-Nachweis bleibt gesperrt.
- [x] Dokumentation mit tatsächlichen Ergebnissen und offenen externen Grenzen aktualisieren.
- [ ] Vier abschließende UI-Prüfungen/Fotoauswahl-Capture nach Cloud-Neustart bzw. im KVM-CI bestätigen; nicht als bestanden ausgeben.
