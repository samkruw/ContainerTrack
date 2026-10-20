# Container Track Android — Acceptance Matrix

## Delivery
- [ ] Order Number Pflicht
- [ ] ZRH Number Pflicht
- [ ] Order / Container-Typ Pflicht
- [ ] Quantity 1..50
- [ ] Quick Pflicht, freies Textfeld
- [ ] additional Work Pflicht, freies Textfeld
- [ ] `Nothing` akzeptiert
- [ ] `Empty` akzeptiert
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
- [ ] kein Firebase/Analytics/Cloud SDK

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
