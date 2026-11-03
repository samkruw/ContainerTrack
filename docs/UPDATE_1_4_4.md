# ContainerTrack 1.4.4 — KT vollständig aus der Nutzung entfernt

Die App erfasst keine KT-Nummer mehr. Das erste Feld für Lieferungen ist die Auftragsnummer („Order Number“); Returns verwenden weiterhin „Return Number“. Es gibt kein zusätzliches KT-Pflichtfeld. Übersicht, Details, Fotoprüfung, Galerie, Suche, Teilen und Export verwenden die vorhandene Auftragsreferenz. Die Suche findet Auftrags- und Containernummern, nicht alte KT-Werte. Lange Auftragsreferenzen werden in den großen Überschriften platzsparend dargestellt.

Die Exportvorschau und die TXT-Kopfzeile verwenden „Auftragsnummer“ statt „KT“. Neue META.json-Dateien enthalten kein kt-Feld; ihr Export-schemaVersion-Wert ist deshalb 2. Das Room-Datenbankschema bleibt unverändert bei Version 2. Alte Aufträge, Fotos, Eigentümerkennungen, Erinnerungen und Fristen bleiben erhalten. Die bestehende KT-Spalte wird ausschließlich für die Kompatibilität alter Datenbanken beibehalten; alte Werte werden weder angezeigt noch durchsucht oder exportiert. Neue Aufträge speichern dort einen leeren Wert. Bereits vorhandene Archivdateien werden nicht umgeschrieben.

Fotoanhänge bleiben nummerierte Original-JPGs wie 0708_01.jpg. Teams-Ziele, mögliche Textunterdrückung und die ungeklärte „Shared Photos“-Anzeige hängen weiterhin von Teams ab. Kopieren der Auftragsinfos und der manuelle Kanalablauf bleiben verfügbar. Keine Benutzerverwaltung, PINs, Rollen, eigene Firmenanmeldung oder zusätzlichen Online-Dienste. Automatische Graph-Uploads bleiben deaktiviert.

Nur Debug-APK: Version 1.4.4 / versionCode 11. Ein Update einer bestehenden Installation setzt dasselbe Signaturzertifikat voraus; nicht zur Aktualisierung deinstallieren. Prüfnachweise: [QA_STATUS.md](QA_STATUS.md).
