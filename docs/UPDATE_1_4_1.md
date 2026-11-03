# ContainerTrack 1.4.1 — direkt lokal arbeiten

Die App startet direkt mit den Aufträgen. Lokale Anmeldung, PINs, Admins, Mitarbeiterrollen und Benutzerverwaltung entfallen auf Wunsch. Im Zahnrad liegen nur noch die Geräteeinstellungen. Es gibt keine zentrale Anmeldung, Team-Galerie oder Synchronisierung zwischen Geräten.

Vorhandene Aufträge und Fotos sind unabhängig von früheren Eigentümerkennungen sichtbar und nutzbar. Diese Kennungen werden nicht umgeschrieben. Room bleibt bei Schema 2; Datenbank, Fotos, Erinnerungen und Fristen werden nicht gelöscht. Frühere private Profileinstellungen werden nicht mehr geladen oder benötigt.

Das Graphit-/Limetten-Design und die vorhandenen Fotoabläufe bleiben erhalten. Nach vollständiger Dokumentation öffnet „Abschließen & in Teams teilen“ die Teams-App beziehungsweise Android-Teilen. Empfänger oder Kanal und Senden bestätigt der Nutzer dort selbst. Die App benötigt keine eigene Firmenkonfiguration oder OAuth-Anmeldung. Teilen bestätigt keinen erfolgreichen Upload und startet keine Erinnerung erneut.

Der manuelle Teilen-Modus wird unabhängig von Benutzern festgelegt. Neue Aufträge erzeugen keine automatische Graph-Upload-Queue. Alte Hintergrund-Worker bleiben deaktiviert; historische Übertragungsdaten bleiben erhalten.

Nur Debug-APK: Version 1.4.1 / versionCode 8, unveränderte App-ID. Ein Update einer vorhandenen Installation setzt dasselbe Signaturzertifikat voraus. Eine Deinstallation löscht lokale Daten und ist kein Update-Verfahren.

Prüfnachweise: [QA_STATUS.md](QA_STATUS.md).
