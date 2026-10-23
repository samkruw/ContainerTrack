# Native Android — Referenzabgleich 1.4.1

final result: passed (manueller visueller Abgleich)

Die ausgewählten Bildvorlagen werden in der bestehenden Kotlin/Compose-App umgesetzt. Nach der letzten Nutzeranweisung entfallen Anmeldung, PINs und Benutzer-/Rollenverwaltung vollständig. Es gibt keine Team-Galerie; die Galerie zeigt lokale Daten.

## Zuordnung

| Vorlage | Native Ansicht | Capture |
| --- | --- | --- |
| 1000036738 Meine Aufträge | OrderListScreen mit aktivem Auftrag, großen Kennungen, kompakten letzten Aufträgen | 20-reference-orders |
| 1000036739 Neuer Auftrag | SetupScreen mit Segmenten, KT, optionalem Erstcontainer/OCR, Pflichtdetails, Fotodokumentation | 21-reference-new-order |
| 1000036740 Fotos prüfen | PhotoReviewScreen mit echten Bildern, sechs Delivery-Plätzen und Status | 22-reference-photo-review |
| 1000036742 Galerie | Galerie-Containerliste, Filter, echte Thumbnail-/Upload-Daten | 23-reference-gallery |
| 1000036745 Fotoauswahl | Auftragsraster mit Auswahl, Original-JPG-Speichern/Teilen | 24-reference-photo-selection |
| 1000036744 Anmeldung | Entfällt auf ausdrücklichen Nutzerwunsch; stattdessen Geräteeinstellungen | 25-reference-settings |
| 1000036741 Teams-Übertragung | UploadScreen mit lokal gespeicherten JPGs und bewusster Übergabe an Teams | 26-reference-upload |
| 1000036743 Benutzer & Rechte | Entfällt auf ausdrücklichen Nutzerwunsch | kein Capture |

## Visuelle Messwerte

Referenzen 592×1280; Android-Capture 1080×2340 mit nahezu identischem Seitenverhältnis. App-eigene Flächen werden verglichen; Systemleisten bleiben Androids Verantwortung. Grundfläche#151A1D, Lime#BEFF42, Weiß#F8FAF9, Linien#3B4348, Inter mit OFL-Lizenz. Markenleiste60dp, Navigation72dp, Außenabstand16dp, primäre Trefferfläche50dp, Radien5–10dp. Haupttitel44sp, Kennungen56sp, Eingaben52sp. Keine Standard-Material-Pills als Hauptnavigation, kein Glow.

## Fachliche Anpassungen

Sechs Delivery-Pflichtfotos bleiben verbindlich. Die Beispiele mit vier oder fünf Fotos ersetzen diese Vorgabe nicht. Pflichtangaben bleiben im Formular erreichbar. Alle lokalen Aufträge und Fotos sind unabhängig von früheren Benutzerzuordnungen zugänglich. Geräteeinstellungen ersetzen das Profil; Benutzerverwaltung und Anmeldung erhalten keine Ersatzattrappen. Teams öffnet seine eigene Zielauswahl; Teilen bestätigt keinen Upload und startet keinen Timer erneut.

## Verifikation

[GitHub-Lauf 39909135079](https://github.com/samkruw/ContainerTrack/actions/runs/39909135079), geprüfter App-/Test-Commit `8d82240fdbabe7c583e48eb6cce67017ef915a85`: 39 Unit-Tests je Variante, 60 Android-Tests, neun Python-QA-Tests, Lint und APK-Berechtigungsprüfung erfolgreich. 24 Screenshot-PNGs, darunter sieben aktuelle Referenzansichten.

Die [finale Übersicht mit synthetischen Testdaten](design/verification/reference-ui-v1.4.1.jpg) wurde tatsächlich geöffnet und visuell geprüft: Graphit/Lime, Inter, große Kennungen, Segmente, Foto-Raster, Auswahlmarken, Markenleiste, Navigation mit Unterstrich sowie Einstellungen ohne Login/Rollen. Kein automatischer Pixelvergleich; keine freigegebenen Goldens vorhanden. Reale Teams-/SAF-/Hardware-Prüfung siehe [QA_STATUS.md](docs/QA_STATUS.md).
