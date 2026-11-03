# ContainerTrack 1.3.1 — konkrete Bildvorlagen

Die acht nachgereichten Graphit-/Lime-Bilder sind Grundlage der nativen Designkorrektur. VersionCode 5, App-ID unverändert, Room-Schema unverändert bei 2.

Die Oberfläche nutzt Inter mit expliziten variablen Schriftgewichten, große KT-/Container-Zahlen, ein flaches Container-Logo, kräftiges Lime, kleine rechteckige Radien und klare Trennlinien. Die Hauptnavigation entspricht Aufträge–Kamera–Galerie mit ausgewähltem Unterstrich. Das Zahnrad führt zum Firmenkonto und zu Einstellungen; ohne verbundenes Konto erscheint die eigenständige Anmeldung mit Offline-Zugang.

Die Auftragsübersicht hebt den aktuellen Vorgang hervor und zeigt letzte Vorgänge als kompakte Zeilen. Suche/Filter werden über das Suchsymbol geöffnet. Der neue Auftrag enthält KT-/Return-Nummer, einen optionalen ersten Container, vorhandenen OCR-Scan und ausklappbare Pflichtangaben. Ungültige Eingaben öffnen die Pflichtangaben; das Return-Logger-Gate bleibt erhalten. Job und optionaler Erstcontainer werden atomar gespeichert, führende Nullen bleiben erhalten. „Fotos starten“ führt bei eingegebenem Erstcontainer direkt zur Aufnahme.

Eine eigene Fotoprüfung zeigt echte Originalbilder, Dateinamen und den realen Vollständigkeitsstand. Bilder lassen sich über die vorhandene Aufnahme ansehen/ersetzen. Der Timer startet weiterhin beim ersten vollständigen Abschluss; die neue Ansicht startet ihn nicht erneut. Delivery bleibt bei sechs Fotos, Return bei einem; die Beispielzahlen aus den Bildern ändern die Fachregeln nicht.

Die Galerie zeigt tatsächlich vorhandene lokale Fotos als Containerliste und als auswählbares Raster. Ausgewählte Original-JPGs lassen sich ohne ZIP/TXT/META teilen oder über Androids Ordnerauswahl speichern. Dateien verschiedener Aufträge werden in getrennten Ordnern abgelegt. Lokale Fotos, Completion-Marker und Upload-Bestätigungen bleiben durch Speichern/Teilen unverändert. Temporäre Share-Kopien werden nach sieben Tagen bereinigt und beim Löschen des betreffenden Auftrags entfernt.

Die Teams-Ansicht zeigt den echten Kontostatus, Zielkanal Op530-Lal und Dateibestätigungen. Ohne Microsoft-Konfiguration gibt es keine erfundene Anmeldung, Übertragung, Administratorrolle oder Team-Benutzerliste. Die lokale Galerie wird nicht als zentrale Team-Galerie ausgegeben. Einrichtung: [MICROSOFT_SETUP.md](MICROSOFT_SETUP.md).

Android-Tests erfassen alle acht vollständigen nativen Ansichten einschließlich Markenleiste/Navigation. Die Containerabbildung liegt ausschließlich in androidTest/assets und ist synthetisches Testmaterial; sie ist nicht in der ausgelieferten App enthalten. Visuelle Prüfnachweise: [design-qa.md](../design-qa.md), aktueller Lauf und Artefakt: [QA_STATUS.md](QA_STATUS.md).

Nur Debug-APK. Eine Installation über einer vorhandenen App setzt weiterhin dasselbe Debug-Zertifikat voraus.
