# Native Android – Referenzabgleich

final result: passed (manueller visueller Abgleich)

## Ziel

Die acht am 22.10.2026 vom Benutzer ausgewählten Bildvorlagen 1000036738–1000036745 werden in der bestehenden Kotlin/Compose-App umgesetzt. Kein Browser-/Web-Prototyp; die echte Android-Darstellung ist das Vergleichsobjekt. Quellbilder lokal unter /tmp/codex-remote-attachments; nicht als Produktbilder veröffentlicht.

## Zuordnung

| Vorlage | Native Ansicht | Capture |
| --- | --- | --- |
| 1000036738 Meine Aufträge | OrderListScreen mit aktivem Auftrag, großen Kennungen, kompakten letzten Aufträgen | 20-reference-orders |
| 1000036739 Neuer Auftrag | SetupScreen mit Segmenten, KT, optionalem Erstcontainer/OCR, Pflichtdetails, Fotodokumentation | 21-reference-new-order |
| 1000036740 Fotos prüfen | PhotoReviewScreen mit echten Bildern, sechs Delivery-Plätzen und Status | 22-reference-photo-review |
| 1000036742 Galerie | Galerie-Containerliste, Filter, echte Thumbnail-/Upload-Daten | 23-reference-gallery |
| 1000036745 Fotoauswahl | Auftragsraster mit Auswahl, Original-JPG-Speichern/Teilen | 24-reference-photo-selection |
| 1000036744 Anmeldung | ProfileScreen mit Logo, Firmenanmeldung, Offline-Zugang | 25-reference-sign-in |
| 1000036741 Teams-Übertragung | UploadScreen mit echtem Kanal-/Kontostatus und per-Datei-Bestätigung | 26-reference-upload |
| 1000036743 Benutzer & Rechte | UserAccessScreen nur mit nachgewiesener Identität/Berechtigung | 27-reference-access |

## Visuelle Messwerte

Referenzen 592×1280; Android-Capture 1080×2340 mit nahezu identischem Seitenverhältnis. App-eigene Flächen werden verglichen; Systemleisten bleiben Androids Verantwortung. Grundfläche#151A1D, Lime#BEFF42, Weiß#F8FAF9, Linien#3B4348, Inter mit OFL-Lizenz. Markenleiste60dp, Navigation72dp, Außenabstand16dp, primäre Trefferfläche50dp, Radien5–10dp. Haupttitel44sp, Kennungen56sp, Eingaben52sp. Keine Standard-Material-Pills als Hauptnavigation, kein Glow.

## Zulässige fachliche Unterschiede

Sechs Delivery-Pflichtfotos bleiben verbindlich; Beispielzahlen4/5 werden nicht übernommen. Namen, drei Benutzer, Administratorrolle und gesendete Dateien werden nicht erfunden. Ohne Firmenkonfiguration bleibt Login deaktiviert und Offline-Nutzung verfügbar. Team-Galerie bleibt als lokale Galerie beschriftet, solange keine zentralen Fremdbilder tatsächlich abrufbar sind. Pflichtangaben stehen im Formular weiterhin zur Verfügung.

## Verifikation

GitHub-Lauf [39839563123](https://github.com/samkruw/ContainerTrack/actions/runs/39839563123), geprüfter Commit `42114afb1a0674e83356c46ad4825abcf79a176d`: Build und Gerätetests erfolgreich. 39 Unit-Tests je Variante, 56 Android-Tests ohne Fehler/übersprungene Tests, neun Python-QA-Tests, Lint und APK-Prüfung bestanden. 25 Screenshot-PNGs erzeugt, darunter alle acht vollständigen Referenzansichten.

Die finale Übersicht wurde tatsächlich geöffnet und mit den gelieferten Referenzen verglichen. [Bildschirmnachweis mit synthetischen Testdaten](design/verification/reference-ui-v1.3.1.jpg). Kontrolliert: Graphit/Lime, Inter-Gewichte, große Kennungen, einzeiliger Auftragstitel bei Standardschrift, Segmente, echte Foto-Raster und Auswahlmarken, eigenständige Anmeldung, Markenleiste und Navigation mit Unterstrich. Kein automatischer Pixelvergleich: dafür sind keine freigegebenen Goldens vorhanden.

Die fachlichen Unterschiede oben bleiben ausdrücklich bestehen. Galerie-/Upload-/Rechteansichten zeigen reale lokale oder bestätigte Daten; ohne Firmenkonfiguration keine behaupteten Team-Rechte oder Erfolge. Reale Microsoft-/SAF-/Hardware-Abnahme siehe [QA_STATUS.md](docs/QA_STATUS.md).
