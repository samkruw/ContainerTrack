# Container Track V18 — Dark Navy UI

ContainerTrack ist eine installierbare **Local-first PWA** für die geführte Dokumentation von **Lieferungen** und **Returns** mit mehreren Containern.

Die App verwaltet die komplette Aufnahme lokal auf dem Smartphone, erzwingt den richtigen Ablauf und erzeugt am Ende ein strukturiertes ZIP inklusive Excel-kompatibler TXT-Datei.

> **Local-only:** Order-/Return-Nummern, Container-Nummern, Fotos, Excel-Daten, Verläufe und Logger-Timer werden nicht an GitHub oder einen Cloud-Server übertragen. GitHub Pages hostet ausschliesslich die statischen App-Dateien.

---

## 1. Lieferung

### Eingaben

Vor dem Start müssen ausgefüllt werden:

- **Order Number** — Auftragsnummer und Name des obersten Export-Ordners
- **ZRH Number / KT** — wird in der TXT-Spalte `KT` verwendet
- **Order / Container-Typ** — wird manuell eingegeben und in der TXT-Spalte `Order` verwendet
- **Anzahl Container** — 1 bis 50
- **Quick** — freies Pflicht-Textfeld
- **additional Work** — freies Pflicht-Textfeld

`Quick` und `additional Work` dürfen nicht leer bleiben. Falls nichts anfällt, kann z. B. `Nothing` oder `Empty` eingetragen werden.

### Container-Ablauf

Für jeden Container:

1. 4-stellige Container-Nummer scannen oder manuell erfassen
2. **Leerer Innenraum**
3. **Aussenseite Front**
4. **Aussenseite Rückseite / Türen**
5. **Aussenseite links**
6. **Aussenseite rechts**
7. **Befüllter Innenraum**
8. Nächster Container

Die Foto-Reihenfolge ist zwingend. Schritte können nicht übersprungen werden.

### Delivery TXT

Spalten:

```text
Datum | In/Out | KT | Order | Quantity | additional Work | Container Number | Quick
```

Mapping:

```text
Datum             = aktuelles Datum
In/Out            = OUT
KT                = ZRH Number
Order             = manuell eingegebener Container-Typ
Quantity          = ausgewählte Container-Anzahl
additional Work   = Pflicht-Textfeld
Container Number  = alle Container-Nummern des Batches
Quick             = Pflicht-Textfeld
```

### Export-Struktur

Beispiel `ORDER-7845`:

```text
ORDER-7845/
├── 0708/
│   ├── 0708_01.jpg
│   ├── 0708_02.jpg
│   ├── 0708_03.jpg
│   ├── 0708_04.jpg
│   ├── 0708_05.jpg
│   └── 0708_06.jpg
├── 0710/
│   └── ...
├── ORDER-7845_EXCEL.txt
└── ORDER-7845_META.json
```

---

## 2. Logger nach Lieferung

Nach dem Delivery-Export startet ContainerTrack automatisch einen **30-Minuten-Timer**.

### Während die App offen ist

Oben in der App wird dauerhaft angezeigt:

- Delivery-/Order-Referenz
- Countdown `MM:SS`
- Fortschrittsbalken
- Hinweis bei mehreren offenen Timern

Nach Ablauf wechselt der Timer auf **JETZT** und wird deutlich hervorgehoben.

### Nach 30 Minuten

ContainerTrack fordert zum Starten des Loggers auf.

Die Meldung in der App kann nicht einfach weggeklickt werden und muss mit:

**`Logger gestartet – bestätigen`**

abgeschlossen werden.

Wenn Browser und Android es zulassen, versucht die PWA zusätzlich eine System-Notification anzuzeigen.

### Standby-Verhalten

ContainerTrack bleibt bewusst **Local-only** und verwendet keinen Push-Server.

Android kann eine PWA im Standby pausieren. Deshalb wird nicht nur ein laufender JavaScript-Zähler gespeichert, sondern der **absolute Fälligkeitszeitpunkt**.

Dadurch gilt:

- der Zeitpunkt geht im Standby nicht verloren
- beim Entsperren / erneuten Fokussieren wird sofort neu berechnet
- sind 30 Minuten bereits vorbei, erscheint direkt die Logger-Meldung
- eine exakte Notification bei komplett suspendierter PWA kann Android ohne Push-Server nicht garantiert werden

### Schutz vor doppeltem Timer

Ein erneuter Download desselben Delivery-Vorgangs:

- startet **keinen zweiten Timer**
- setzt den laufenden Timer **nicht zurück**

---

## 3. Return

Beim Return ist der Logger-Schritt bewusst umgedreht.

### Schritt 1 — Logger stoppen

Nach Auswahl von **Return** sind die übrigen Eingaben zunächst gesperrt.

Zuerst muss bestätigt werden:

**`Logger gestoppt – Eingaben freigeben`**

Der genaue Zeitpunkt dieser Bestätigung wird lokal im Return-Vorgang gespeichert.

### Return-Eingaben

Danach müssen ausgefüllt werden:

- **Return Number** — Name des Export-Ordners und Wert für `KT`
- **Order / Container-Typ** — manuell
- **Anzahl Container** — 1 bis 50
- **Quick** — freies Pflicht-Textfeld
- **additional Work** — freies Pflicht-Textfeld

### Return-Ablauf

Für jeden Container:

1. Container-Nummer scannen oder manuell erfassen
2. **ein Foto des Inhalts** aufnehmen
3. nächsten Container erfassen

### Return TXT

```text
Datum             = aktuelles Datum
In/Out            = IN
KT                = Return Number
Order             = manuell eingegebener Container-Typ
Quantity          = ausgewählte Container-Anzahl
additional Work   = Pflicht-Textfeld
Container Number  = alle Container-Nummern
Quick             = Pflicht-Textfeld
```

### Export-Struktur

```text
RETURN-194/
├── 0708/
│   └── 0708_01.jpg
├── 0710/
│   └── 0710_01.jpg
├── RETURN-194_EXCEL.txt
└── RETURN-194_META.json
```

---

## 4. Container-Scan

ContainerTrack erwartet eine **4-stellige Container-Nummer**, z. B.:

```text
0708
1483
0236
```

Führende Nullen bleiben erhalten.

### Speed-Modus

Der erste gültige 4-stellige OCR-Treffer wird sofort übernommen. Es gibt keine 2×- oder 3×-Kontrolle mehr.

### OCR-Aufbereitung

Für den LiveScan werden mehrere lokale Varianten verwendet:

- Graustufen
- Kontrastverstärkung
- Schwarz/Weiss
- invertiertes Bild
- invertiertes Schwarz/Weiss
- Ziffern-Whitelist `0–9`

Dies ist speziell für weisse Nummern auf blauem Schild optimiert.

### Foto-OCR Fallback

Wenn LiveScan nicht funktioniert, kann weiterhin ein Foto des Schildes aufgenommen und lokal per OCR ausgewertet werden.

> Tesseract.js läuft im Browser. Die Fotos werden nicht an einen OCR-Server geschickt. Die Bibliothek wird aktuell über ein externes CDN geladen; beim ersten OCR-Start kann deshalb eine Internetverbindung notwendig sein.

---

## 5. Batch-Sicherheit

ContainerTrack verhindert typische Fehler im Ablauf:

- Container-Anzahl wird vorab festgelegt
- jeder Container erhält einen eigenen Unterordner
- doppelte Container-Nummern im selben Batch werden blockiert
- Delivery-Fotos müssen strikt `1 → 6` erfolgen
- Return benötigt exakt ein Inhaltsfoto je Container
- Export wird erst freigegeben, wenn alle Container vollständig sind
- nur das zuletzt aufgenommene Foto kann direkt zurückgesetzt werden
- angefangene Vorgänge können lokal wieder geöffnet werden

---

## 6. Lokale Speicherung

### IndexedDB

Gespeichert werden lokal:

- Delivery-/Return-Vorgänge
- Order-/Return-Referenzen
- Container-Nummern
- Fotos
- Bearbeitungsstand
- TXT-Felder
- Logger-Stopp-Zeitpunkt beim Return

### localStorage

Der Delivery-Logger-Reminder speichert lokal:

- Timer-ID
- Delivery-Referenz
- Startzeit
- Fälligkeitszeit
- Bestätigungsstatus

### GitHub Pages

GitHub enthält nur:

```text
index.html
manifest.webmanifest
sw.js
icon-192.png
icon-512.png
README.md
```

Keine operativen Containerdaten werden ins Repository geschrieben.

---

## 7. PWA

ContainerTrack kann über GitHub Pages als PWA installiert werden.

Auf Android/Chrome:

1. GitHub-Pages-Seite öffnen
2. **Installieren** wählen
3. Kamera-Berechtigung erlauben
4. optional Notifications erlauben

Die App startet danach in einem eigenen App-Fenster.

---

## 8. Technischer Stack

```text
Vanilla HTML
Vanilla CSS
Vanilla JavaScript
IndexedDB
localStorage
Tesseract.js
MediaDevices / getUserMedia
Canvas API
Notifications API
Service Worker
Web App Manifest
GitHub Pages
```

Keine Frameworks. Kein Build-Prozess. Kein Backend. Keine Cloud-Datenbank. Keine kostenpflichtige API.

---

## 9. QA / Testloop V17

Der finale Build wurde in mehreren Testschleifen geprüft.

### Automatisch / browser-simuliert geprüft

- JavaScript-Syntax `index.html`
- JavaScript-Syntax `sw.js`
- gültiges `manifest.webmanifest`
- keine doppelten HTML-IDs
- alle JavaScript-DOM-Referenzen vorhanden
- alle Label-Ziele vorhanden
- keine Optimo-/Firmenlogos oder Firmennamen
- Delivery-Pflichtfeld-Validierung
- `Quick` als freies Pflicht-Textfeld
- `additional Work` als Pflicht-Textfeld
- Delivery-Batch mit mehreren Containern
- 6 Fotos in strikter Reihenfolge
- Duplicate-Container-Schutz
- Delivery-ZIP-Struktur
- Delivery-TXT-Inhalt
- Delivery-Mapping `OUT / KT=ZRH / Order=Container-Typ`
- Return-Logger-Gate
- exakter Logger-Stopp-Zeitpunkt
- Return-Pflichtfeld-Validierung
- Return-Batch mit mehreren Containern
- ein Inhaltsfoto pro Return-Container
- Return-ZIP-Struktur
- Return-TXT-Inhalt
- Return-Mapping `IN / KT=Return Number / Order=Container-Typ`
- lokaler Verlauf öffnen / wieder aufnehmen
- 30-Minuten-Timer erzeugen
- sichtbarer Countdown
- Fälligkeitsstatus `JETZT`
- blockierende Logger-Bestätigung
- Bestätigung entfernt offenen Reminder
- Re-Export erzeugt keinen zweiten Timer
- ZIP-Datei auf Beschädigung geprüft
- PWA-Icons auf 192×192 und 512×512 geprüft

### Hardwareabhängig

Folgende Punkte sind technisch geprüft, benötigen für einen vollständigen Endtest aber weiterhin ein reales Android-Gerät:

- reale Kamera / `getUserMedia`
- Fokusqualität der Kamera
- Taschenlampen-/Torch-Unterstützung
- reale OCR-Erkennungsrate unter Lagerbedingungen
- Installation über Chrome als PWA
- Android-System-Notification im Standby

Diese Funktionen hängen von Kamera, Chrome-Version und Android-Energiesparverhalten ab und können in einem Headless-Testbrowser nicht vollständig simuliert werden.

---

## 10. Versionsübersicht

### V17 — Final QA

- Quick auf freies Pflicht-Textfeld geändert
- Validierung vor Erzeugung eines Vorgangs
- exakter Return-Logger-Stopp-Zeitpunkt
- Schutz vor doppeltem Delivery-Timer beim Re-Export
- README vollständig bereinigt
- kompletter Delivery-/Return-/Timer-Testloop

### V16

- sichtbarer Logger-Countdown in der App
- Fortschrittsbalken
- `JETZT`-Status nach Ablauf

### V15

- Delivery: 30-Minuten-Logger-Reminder
- Return: Logger stoppen als erster Schritt
- Quick und additional Work zu Pflichtfeldern gemacht

### V14

- Order Number und Excel-Spalte Order bei Delivery getrennt
- Order = Container-Typ

### V13

- Excel-TXT auch für Delivery
- ZRH Number → KT bei Delivery
- Return Number → KT bei Return

### V12

- Delivery- und Return-Modus
- Batch mit frei wählbarer Container-Anzahl
- Container-Unterordner
- Return mit nur einem Inhaltsfoto

### V10

- schneller Container-LiveScan
- erster gültiger Treffer wird direkt übernommen
- 6-Foto-Delivery-Ablauf
- Local-only PWA-Grundlage


## V18 UI Update

Die komplette Oberfläche wurde auf das neue App-/UI-Designsystem umgestellt.

### Design

- Dark Navy Hintergrund
- Graphite/Dark Cards
- Electric Blue als Primäraktion
- Light Blue als Akzent
- Grün für abgeschlossene Schritte
- Amber für Logger-/Warnstatus
- Rot für Fehler und abgelaufene Logger-Reminder
- Mobile-First Layout
- neue feste Bottom-Navigation
- Scanner mit blauem Fokusrahmen
- Formulare, Statuskarten, Workflow-Schritte und Modals im gleichen Designsystem
- neues neutrales Container-Track-App-Icon

### Navigation

Die mobile Navigation enthält:

- Start
- Aufträge
- Scannen
- Export
- Mehr

Alle bisherigen V17-Funktionen bleiben erhalten:

- Delivery / Return
- Batch mit mehreren Containern
- Live-OCR
- Pflichtfoto-Reihenfolge
- Return nur 1 Inhaltsfoto
- Excel-kompatible TXT-Datei
- Local-only IndexedDB
- sichtbarer 30-Minuten-Logger-Timer
- Return-Logger-Stopp als Schritt 1
- PWA Installation
- lokaler ZIP-Export

Keine Firmenlogos oder Firmennamen wurden übernommen. Es wurde ausschließlich das visuelle Designsystem aus der Referenz adaptiert.
