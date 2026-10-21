# Container Track Android — Acceptance Matrix

## Delivery
- [ ] Order Number Pflicht
- [ ] ZRH Number Pflicht
- [ ] Order / Container-Typ Pflicht
- [ ] Quantity 1..50
- [ ] Quick nicht erforderlich und im Delivery-Formular ausgeblendet
- [ ] additional Work nicht erforderlich und im Delivery-Formular ausgeblendet
- [ ] OCR exakt 4-stellig
- [ ] führende Null bleibt
- [ ] erster gültiger Treffer wird übernommen
- [ ] Duplicate blockiert
- [ ] Manual Entry
- [ ] Torch wenn Hardware unterstützt
- [ ] Foto 1 leerer Innenraum
- [ ] Foto 2 Front
- [ ] Foto 3 Rückseite/Türen
- [ ] Foto 4 links
- [ ] Foto 5 rechts
- [ ] Foto 6 befüllter Innenraum
- [ ] kein Skip
- [ ] Dateinamen 01..06
- [ ] mehrere Container
- [ ] ZIP Root = Order Number
- [ ] Container-Unterordner
- [ ] TXT Quick und additional Work leer, auch bei historischen Werten
- [ ] TXT OUT
- [ ] TXT KT = ZRH Number
- [ ] TXT Order = Container-Typ
- [ ] Quantity korrekt
- [ ] alle Container im TXT
- [ ] Export nur bei Vollständigkeit
- [ ] 30-Minuten-Reminder nach Export
- [ ] Re-Export erzeugt keinen zweiten/reset Timer
- [ ] Countdown sichtbar
- [ ] Standby Notification via AlarmManager
- [ ] Logger gestartet muss bestätigt werden

## Return
- [ ] Formular zuerst gesperrt
- [ ] Logger gestoppt zuerst bestätigen
- [ ] loggerStoppedAt exakt bei Bestätigung
- [ ] Return Number Pflicht
- [ ] Order / Container-Typ Pflicht
- [ ] Quantity 1..50
- [ ] Quick Pflicht
- [ ] additional Work Pflicht
- [ ] `Nothing` und `Empty` in beiden Feldern akzeptiert
- [ ] OCR 4-stellig
- [ ] Duplicate Block
- [ ] exakt 1 Inhaltsfoto pro Container
- [ ] Dateiname 01
- [ ] ZIP Root = Return Number
- [ ] Container-Unterordner
- [ ] TXT IN
- [ ] TXT KT = Return Number
- [ ] TXT Order = Container-Typ
- [ ] alle Container im TXT

## Offline / Privacy
- [ ] Prozess-Tod verliert keinen Job
- [ ] Neustart stellt Job wieder her
- [ ] Bilder intern
- [ ] OCR im Flugmodus
- [ ] Export im Flugmodus
- [ ] Logger im Flugmodus
- [ ] kein INTERNET im merged manifest
- [ ] keine Analytics-/Cloud-Datenhaltungs-SDKs; technische ML-Kit-Hilfsartefakte dokumentiert

## UI / Anti-Slop
- [ ] Referenzbild verwendet
- [ ] Dark Navy / Electric Blue
- [ ] keine fremden Logos
- [ ] keine fremden Firmennamen
- [ ] keine Placeholder Cards
- [ ] keine nutzlosen Buttons
- [ ] keine Fake-Daten
- [ ] keine Marketingtexte
- [ ] Touch Targets >=48dp
- [ ] Fehler am Feld
- [ ] Permission-Denial sauber
- [ ] Start, Aufträge, Scannen, Export, Mehr funktionieren
- [ ] Screenshot-Regressionstests für Kern-Screens

## Update 1.2 / Teilen und Kompatibilität
- [ ] ZIP teilen ist Primäraktion, ZIP speichern bleibt unabhängig verfügbar
- [ ] ACTION_SEND, application/zip, EXTRA_STREAM und ClipData enthalten lesbare FileProvider-URI
- [ ] Nur temporärer Lesezugriff, keine internen Foto-/Datenbankdateien freigegeben
- [ ] Android Chooser öffnet; Abbruch gilt nicht als Versandbestätigung
- [ ] Re-Share/Re-Export verändert weder Reminder-ID noch Frist
- [ ] bestehende Room-Daten, Fotos und Reminder nach Update mit identischem Signaturschlüssel erhalten
- [ ] Glasflächen auf Formularen, Export, Statuskarten und Navigation konsistent

Checkboxen sind die vollständige Abnahmeliste, keine automatischen Testresultate. Aktuelle Nachweise und verbleibende Geräteprüfungen: [QA_STATUS.md](QA_STATUS.md).
