# Update 1.5.3 – Quick und Zusatzarbeiten entfernt

Lieferung und Return verlangen nur noch ihre übrigen Auftragsangaben. Quick und Zusatzarbeiten erscheinen weder im Formular noch in Auftragsdetails, Exportvorschau oder vorbereiteten E-Mail- und Share-Texten. Neue Aufträge speichern dafür leere Werte.

Neue ZIP-Exporte enthalten eine TXT-Datei mit sechs Spalten: `Datum`, `In/Out`, `Auftragsnummer`, `Order`, `Quantity`, `Container Number`. Die META-Datei hat `schemaVersion` 5 und enthält keine Quick-/Zusatzarbeiten-Schlüssel. Auch ältere Aufträge mit gespeicherten Werten geben diese Felder in neuen E-Mails und Exporten nicht mehr aus. Ein Empfänger, der bisher die acht TXT-Spalten verarbeitet hat, muss auf das neue Format angepasst werden.

Die alten Room-Spalten bleiben intern erhalten, damit vorhandene Aufträge ohne Datenbankmigration lesbar bleiben. App-ID und Room-Schema 4 ändern sich nicht. Return-Fotos, Überspringen bei bereits sauberen Containern, E-Mail-Bestätigung und 24h-Erinnerung bleiben bestehen.
