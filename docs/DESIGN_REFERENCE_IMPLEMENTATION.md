# Bildvorlagen und aktueller Produktumfang — 1.5.0

Stand: 28.10.2026. Die acht Benutzerbilder 1000036738–1000036745 vom 22.10.2026 bleiben die visuelle Referenz. Die Umsetzung erfolgt in der bestehenden nativen Compose-/Room-/CameraX-App. Die nachfolgenden fachlichen Regeln ersetzen frühere Vorgaben zu KT, Benutzern, Teams und dem einzelnen Return-Foto.

## Oberfläche

Visuelles Ziel bleiben die Graphit-Grundfläche #151A1D, nahezu weiße Typografie, Lime #BEFF42, flache Abschnitte mit feinen grauen Trennlinien, kleine rechteckige Radien und gut lesbare Auftrags- und Containernummern. Die Markenleiste trägt Container-Logo und Einstellungs-Zahnrad. Die Navigation Aufträge–Kamera–Galerie nutzt den Lime-Unterstrich. Der aktive Auftrag wird hervorgehoben, weitere Aufträge erscheinen als kompakte Zeilen; die Galerie zeigt tatsächliche lokale Fotos mit sichtbarer Auswahl und Fertigmarken.

Die App startet direkt ohne Anmeldung, Benutzerverwaltung, Rollen oder PINs. Das Zahnrad öffnet Geräteeinstellungen. KT ist kein Eingabe-, Such-, Anzeige- oder Exportfeld; ein historischer Datenbankwert bleibt nur zur Datenverträglichkeit erhalten. Personen, Initialen, Rollen und Beispieldaten aus den Bildvorlagen werden nicht als Produktdaten übernommen. Aktive Teams-Aktionen und Upload-Erfolgsmeldungen gehören nicht zum aktuellen Ablauf.

## Fotodokumentation und Abschluss

Lieferungen behalten sechs Pflichtfotos je Container: leerer Innenraum, Front, Rückseite/Türen, links, rechts und später befüllter Innenraum. Die separate 30-Minuten-Erinnerung zum Loggerstart bleibt erhalten.

Neue und beim Update noch offene Returns benötigen je Container genau ein Inhalts-Pflichtfoto. Zusätzliche Anfangsfotos für Schäden oder Sonstiges sind optional. Die Oberfläche zeigt beide Ergänzungsaktionen und die daraus entstehende Zielanzahl der Leer-/Sauber-Fotos. Jeder Container benötigt am Ende exakt so viele Leer-/Sauber-Fotos wie Anfangsfotos insgesamt: 1 Inhalt + 2 Schadenfotos ergibt 3 Leer-/Sauber-Fotos. Alle vorgesehenen Container müssen dokumentiert sein; eine fehlende oder überzählige Aufnahme darf keinen vollständigen Auftrag ergeben. Einzelne Fotos bleiben bis zur Versandbestätigung ansehbar, ersetzbar und löschbar.

Ein Return erhält bei Erstellung eine feste Frist von 24 Stunden. Bearbeitung, Export, Öffnen oder Abbrechen des Mailprogramms starten diese Frist nicht neu und bestätigen keinen Versand. Vollständige Fotos führen zunächst zum Status „E-Mail ausstehend“. Erst die ausdrückliche lokale Bestätigung „E-Mail gesendet“ für die aktuell vorbereitete Dokumentation schließt den Return und seine Erinnerung. Nach einer Änderung ist die Mail erneut vorzubereiten; nach der Versandbestätigung ist die Dokumentation gesperrt. Offene und überfällige Returns bleiben in der App sichtbar und bearbeitbar. Eine Return-Erinnerung darf die Fotobearbeitung nicht durch einen unbestätigbaren Logger-Dialog blockieren.

## E-Mail und Dateinamen

„Auftrag per E-Mail“ übergibt alle Original-JPGs aller Container eines vollständigen Auftrags an eine geeignete Smartphone-Mail-App. Das gilt auch beim Einstieg über die Galerie; eine Fotoauswahl verkleinert den Auftragsanhang nicht. Mehrere Aufträge werden getrennt vorbereitet. Betreff ist exakt die Auftragsnummer, der Begleittext enthält die Auftragsinformationen. Den Empfänger wählt der Benutzer im Mailprogramm. Die App sendet keine Nachricht selbst und bestätigt keine Zustellung. Anhänge werden nicht stillschweigend gekürzt; Größenlimits und tatsächlicher Versand hängen von Mail-App und Maildienst ab.

Lieferungen und bereits abgeschlossene alte Returns behalten Namen wie `0708_01.jpg`. Der neue Return-Ablauf verwendet `0708_vorher_01_inhalt.jpg`, `0708_vorher_02_schaden.jpg`, `0708_vorher_03_zusatz.jpg` und `0708_nachher_01_leer_sauber.jpg` usw. Die Nummerierung zählt je Phase; bestehende interne Fotoschritte werden nicht neu angeordnet. Geteilt werden benannte Cachekopien mit temporären Leserechten. Originaldateien bleiben erhalten. Room-Migration 2→3 bewahrt vorhandene Aufträge, Fotos und Erinnerungen; bereits abgeschlossene alte Returns behalten ihren bisherigen Ein-Foto-Ablauf.

## Nachweis und offene Abnahme

Der aktuelle Ausführungsstand steht in [QA_STATUS.md](QA_STATUS.md). Für 1.5.0 sind 50 Unit-Tests je Variante, neun Python-QA-Tests und zwölf Android-Kernprüfungen bestätigt. Nach dem ersten Gesamtlauf mit 68/76 bestandenen Tests bestand ein gezielter Nachlauf 26/30. Der abschließende Nachlauf wurde nach zwei bestandenen von sechs Prüfungen durch den Cloud-Neustart unterbrochen. Vier UI-Prüfungen bleiben offen; keine vollständige Gerätefreigabe.

Neun aktuelle Captures wurden tatsächlich einzeln gesichtet: sechs native Ansichten und drei Return-Ausschnitte. [Übersicht und Einzelheiten](../design-qa.md). Der Fotoauswahl-Capture fehlt weiterhin. Echte Mail-App-Übergabe, Anhangsgrenzen, Versand, Kamera/OEM und Benachrichtigungen auf dem Nutzergerät bleiben gesondert zu prüfen. Synthetische Containerbilder werden ausschließlich als Testdaten verwendet.
