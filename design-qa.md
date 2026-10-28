# Native Android — Referenzabgleich 1.4.4

final result: funktionale Prüfungen bestanden; manueller visueller Abgleich für 1.4.4 offen

Die ausgewählten Bildvorlagen werden in der bestehenden Kotlin/Compose-App umgesetzt. Auf Nutzerwunsch entfallen Anmeldung, PINs, Rollen und jetzt auch KT vollständig aus der App-Nutzung. Die Galerie zeigt lokale Daten.

## Zuordnung

| Vorlage | Native Ansicht | Capture |
| --- | --- | --- |
| 1000036738 Meine Aufträge | OrderListScreen mit aktivem Auftrag, großen Kennungen, kompakten letzten Aufträgen | 20-reference-orders |
| 1000036739 Neuer Auftrag | SetupScreen mit Segmenten, Auftragsnummer, optionalem Erstcontainer/OCR, Pflichtdetails, Fotodokumentation | 21-reference-new-order |
| 1000036740 Fotos prüfen | PhotoReviewScreen mit echten Bildern, sechs Delivery-Plätzen und Status | 22-reference-photo-review |
| 1000036742 Galerie | Galerie-Containerliste, Filter, echte Thumbnail-/Upload-Daten | 23-reference-gallery |
| 1000036745 Fotoauswahl | Auftragsraster mit Auswahl, Original-JPG-Speichern/Teilen | 24-reference-photo-selection |
| 1000036744 Anmeldung | Entfällt auf ausdrücklichen Nutzerwunsch; stattdessen Geräteeinstellungen | 25-reference-settings |
| 1000036741 Teams-Übertragung | UploadScreen mit lokal gespeicherten JPGs und bewusster Übergabe an Teams | 26-reference-upload |
| 1000036743 Benutzer & Rechte | Entfällt auf ausdrücklichen Nutzerwunsch | kein Capture |

## Visuelle Messwerte

Referenzen 592×1280; Android-Capture 1080×2340 mit nahezu identischem Seitenverhältnis. App-eigene Flächen werden verglichen; Systemleisten bleiben Androids Verantwortung. Grundfläche#151A1D, Lime#BEFF42, Weiß#F8FAF9, Linien#3B4348, Inter mit OFL-Lizenz. Markenleiste60dp, Navigation72dp, Außenabstand16dp, primäre Trefferfläche50dp, Radien5–10dp. Haupttitel44sp, kurze Kennungen56sp, längere Auftragsreferenzen32sp mit begrenzten Zeilen, Eingaben52sp. Keine Standard-Material-Pills als Hauptnavigation, kein Glow.

## Fachliche Anpassungen

Auftragsreferenz statt KT in Erfassung, Pflichtprüfung, Suche, Übersicht, Details, Fotoprüfung, Galerie und Export. Lieferungen benötigen nur Auftragsnummer, Containertyp und Menge; der optionale Erstcontainer/OCR bleibt erhalten. Alte KT-Werte verbleiben ausschließlich ungenutzt in der bestehenden Room-Spalte zur Datenbankkompatibilität. Sechs Delivery-Pflichtfotos, Return-Logger-Gate, Kopieren und manueller Teams-/Android-Share bleiben verbindlich. Kein bestätigter Upload durch bloßes Teilen.

## Verifikation

[GitHub-Lauf 40330673261](https://github.com/samkruw/ContainerTrack/actions/runs/40330673261), App-/Test-Commit `f45788e8c1afaef03189302705f8d582a7696e1e`: 41 Unit-Tests je Variante, 60 Android-Tests, neun Python-QA-Tests, Lint und APK-Berechtigungsprüfung erfolgreich. 24 Screenshot-PNGs mit sieben Referenzansichten wurden erzeugt: [Screenshot-Artefakt](https://github.com/samkruw/ContainerTrack/actions/runs/40330673261/artifacts/12654148709).

Die aktuelle Übersicht wurde noch nicht manuell visuell geprüft. Nach der Wiederaufnahme sind die temporären Dateien nicht mehr vorhanden; der erneute GitHub-API-Abruf ist durch die Entwicklungs-Netzwerkregel blockiert. Die [visuell geprüfte Übersicht 1.4.3](design/verification/reference-ui-v1.4.3.jpg) ist ein historischer Nachweis und zeigt noch KT außerhalb der damaligen Fotoinfos. Sie wird nicht als Nachweis für die vollständige KT-Entfernung verwendet. Keine freigegebenen Goldens, kein behaupteter Pixelvergleich.

Unabhängiger statischer Review bestätigt keine aktiven KT-Verwendungen außerhalb der ignorierten Altspalte und keine wichtigen Restbefunde. Lange Referenzen und große Systemschriften sowie reale Teams-/SAF-/Hardware-Darstellung bleiben Geräte-Abnahme. Details: [QA_STATUS.md](docs/QA_STATUS.md).
