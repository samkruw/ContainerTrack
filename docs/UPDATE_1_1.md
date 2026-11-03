# Update 1.1.0

Die gewünschte Änderung vom 21.10.2026 ersetzt die frühere Vorgabe gegen Glassmorphism sowie die Pflicht zu Quick/additional Work bei Lieferungen.

- Exportprüfung bietet **ZIP teilen** (Android-Auswahl, beispielsweise E-Mail-App) und **ZIP speichern** (SAF).
- Teilen erzeugt ein vollständiges Archiv im privaten Cache mit eng begrenztem FileProvider und temporärem Lesezugriff. Keine neue Berechtigung und kein eigener Netzwerkzugriff. Die App sendet keine E-Mail selbst.
- Ein vollständig zum Teilen erstelltes ZIP zählt als Export. Abbruch der App-Auswahl ist keine Versandbestätigung und setzt die Logger-Frist nicht zurück. Die Oberfläche erklärt diesen Zeitpunkt ausdrücklich.
- Jede Freigabe verwendet einen eigenen Pfad, damit vorherige Anhänge lesbar bleiben. Beim Löschen des Auftrags werden zugehörige interne Exportkopien entfernt. Bei weiterer Freigabe werden Kopien älter als sieben Tage bereinigt; Android kann den Cache früher löschen. Empfänger-Apps verwalten eigene Kopien selbst.
- Quick und additional Work erscheinen ausschliesslich bei Return und bleiben dort Pflichtfelder nach der Logger-Bestätigung. Bei Delivery sind auch historische Werte in TXT/META leer. Die acht TXT-Spalten und Room-Schema 1 bleiben kompatibel.
- Die bestehende Navy-/Blau-Oberfläche erhält transparente Glasflächen, diagonale Lichtkanten, weich beleuchtete Hintergründe und grössere Ecken. Text und Kamera bleiben scharf. Keine plattformabhängige Hintergrundunschärfe erforderlich.
- VersionCode 2, VersionName 1.1.0; vorhandene lokale Daten bleiben beim Update mit demselben Signaturschlüssel erhalten.

## Prüfung

Der Test-Commit `1ccda3c10a83a66bf15f70330b97f10775c40bfc` scheiterte erwartungsgemäss bei genau zwei neuen Erwartungen gegen das alte Verhalten: Delivery ohne Zusatzfelder und Delivery-TXT mit leeren Zusatzwerten ([Lauf 39555872263](https://github.com/samkruw/ContainerTrack/actions/runs/39555872263)). 13 Unit-Tests, davon zwei fehlgeschlagen. Anschliessend wurde die Implementierung geändert.

Erweiterte Suite: 13 Unit-Tests pro Buildvariante und 28 Android-Tests: bisherige 20 plus Return-Pflichtfelder, vier Freigabe-/Provider-/Timerprüfungen, zwei Exportansichten und ein Aufruf der echten Android-Auswahl über die Oberfläche (vom Test abgefangen, kein E-Mail-Versand).

Lokale Quellcode-Berechtigungsprüfung und fünf Python-QA-Tests bestanden. Android-Build, Lint, APK-Berechtigungen und Emulator laufen über den PR-Check. Dessen neuester Commit ist massgeblich; ein alter erfolgreicher Lauf belegt dieses Update nicht.

Die tatsächliche Anhangübernahme mit dem bevorzugten E-Mail-Programm sowie die visuelle Abnahme auf Hardware bleiben manuell zu prüfen. Bestehende offene Freigabeprüfungen stehen in QA_STATUS.md.
