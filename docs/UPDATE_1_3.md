# ContainerTrack 1.3.0 (versionCode 4)

Native Android-App mit unveränderter App-ID `ch.zerogen.containertrack`. Nur Debug-APK, keine Release-Veröffentlichung.

Delivery dokumentiert zunächst je Container Fotos 01–05: leerer Innenraum, Front, Rückseite/Türen, links, rechts. Weitere Container können sofort folgen. Foto 06 (befüllt) darf später aufgenommen werden. Erst alle sechs Fotos für alle Container schließen den Auftrag ab. Return bleibt bei einem Foto sowie Logger-Stopp, Quick und additional Work.

Der erste vollständige Abschluss legt atomar den Completion-Marker, die Foto-Warteschlange und bei Delivery genau eine Logger-Erinnerung mit 30 Minuten Frist an. Export, Teilen, erneute Aufnahme, Wiederanmeldung und Upload-Retry setzen diese Frist nicht zurück. Bereits abgeschlossene Altdaten erzeugen keinen automatischen Massen-Upload. Einer abgeschlossenen alten Delivery ohne Erinnerung wird bei der Wiederherstellung einmalig eine Erinnerung ab diesem Zeitpunkt zugeordnet.

Room migriert explizit von Schema 1 auf 2. Aufträge, Foto-Pfade und vorhandene Erinnerungen bleiben erhalten; es gibt keine destruktive Migration. Neue Queue-Felder enthalten IDs, Status, Zeitpunkte und bestätigte Uploader, keine Zugangstoken.

Aufträge bieten Suche und Filter Aktiv, Befüllt-Foto fehlt, Upload-Fehler und Alle. Containerkarten lassen sich zu nummerierten Fotoplätzen aufklappen. Galerie zeigt vorhandene lokale Fotos mit KT-/Order-/Container- und bestätigtem Uploader-Filter. Profil zeigt den tatsächlichen Microsoft-Status und Geräteeinstellungen. Der Plus-Button legt Lieferung oder Return an.

Optionaler Microsoft-Upload verwendet MSAL Public Client mit einem Firmenkonto und Microsoft Graph. Zielkanal ist `Op530-Lal`. Ausschließlich nummerierte JPEGs werden übertragen; ZIP, TXT und META bleiben beim separaten lokalen Export. WorkManager verlangt Netzwerk, wiederholt vorübergehende Fehler mit Backoff und speichert Bestätigungen je Foto. Ordner und Dateipfade sind deterministisch. Wiederholungen überschreiben dieselbe Datei, statt Duplikate anzulegen. Bei geänderten Fotos/Containernummern werden alte Bestätigungen ungültig. Lokale Fotos werden nach Upload nicht gelöscht. Kontoabweichung pausiert die Queue; Abmeldung beendet anstehende Arbeit.

Ohne öffentliche Microsoft-Konfiguration bleibt die App nutzbar und zeigt „Nicht verbunden“. Es werden weder erfolgreicher Login noch Upload simuliert. Einrichtung: [MICROSOFT_SETUP.md](MICROSOFT_SETUP.md).

Die neuen Netzwerkberechtigungen dienen ausschließlich Microsoft-Anmeldung und Fotoübertragung. OCR, Aufnahme, lokale Daten und Logger funktionieren offline. Keine Standortberechtigung, kein WebView, keine Analytics. Beim manuellen Android-/Teams-Teilen entscheidet die Ziel-App über den tatsächlichen Versand; der App-Uploadstatus wird dadurch nicht auf erfolgreich gesetzt.

Ein Debug-Update benötigt denselben Signaturschlüssel wie die installierte APK. Ein frischer GitHub-Runner erzeugt standardmäßig einen anderen Debug-Schlüssel. Bei Signaturabweichung ist kein Update über die bestehende Installation möglich; eine Deinstallation löscht interne Daten. Es werden keine Schlüssel oder Passwörter im Repository benötigt.
