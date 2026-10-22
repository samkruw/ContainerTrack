# Native Android – Referenzabgleich

final result: pending

## Ziel

Die acht am 22.10.2026 vom Benutzer ausgewählten Bildvorlagen1000036738–1000036745 werden in der bestehenden Kotlin/Compose-App umgesetzt. Kein Browser-/Web-Prototyp; die echte Android-Darstellung ist das Vergleichsobjekt. Quellbilder lokal unter /tmp/codex-remote-attachments; nicht als Produktbilder veröffentlicht.

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

Referenzen592×1280; Android-Capture1080×2340 mit nahezu identischem Seitenverhältnis. App-eigene Flächen werden verglichen; Systemleisten bleiben Androids Verantwortung. Grundfläche#151A1D, Lime#BEFF42, Weiß#F8FAF9, Linien#3B4348, Inter mit OFL-Lizenz. Markenleiste60dp, Navigation72dp, Außenabstand16dp, primäre Trefferfläche50dp, Radien5–10dp. Haupttitel44sp, Kennungen56sp, Eingaben52sp. Keine Standard-Material-Pills als Hauptnavigation, kein Glow.

## Zulässige fachliche Unterschiede

Sechs Delivery-Pflichtfotos bleiben verbindlich; Beispielzahlen4/5 werden nicht übernommen. Namen, drei Benutzer, Administratorrolle und gesendete Dateien werden nicht erfunden. Ohne Firmenkonfiguration bleibt Login deaktiviert und Offline-Nutzung verfügbar. Team-Galerie bleibt als lokale Galerie beschriftet, solange keine zentralen Fremdbilder tatsächlich abrufbar sind. Pflichtangaben stehen im Formular weiterhin zur Verfügung.

## Verifikation

39 Unit-Tests je Variante, sieben Python-QA-Tests und lokale vollständige Gradle-Builds/Lint sind erfolgreich. Neue Erstcontainer-/Auswahl-/SAF-Share-Tests kompiliert. Kein Beweis für reale Firmenübertragung. CI39815246723 führte56 Android-Tests aus;53 bestanden, drei scheiterten. Ursachen: zwei Tests suchten noch nicht komponierte LazyColumn-Aktionen ohne vorheriges Scrollen; eine Screenshot-Vorbereitung änderte die Benachrichtigungsfreigabe. Scroll-Suche korrigiert, Screenshot-Fixtures bestätigen historische Reminder ohne Berechtigungsänderung.

Die tatsächliche Übersicht aus dem CI-Protokoll wurde geöffnet (/tmp/design-reference-preview.jpg). Der Vergleich zeigte: fehlende variable Schriftgewichte, Material-Pill-Segmente und heller Systemleisten-Scrim. Explizite Inter-weight-Achsen, rechteckige Lime-Segmente und dunkle Android-Systemleisten wurden korrigiert. Die Aufnahme des neuen Formulars scrollt vor dem Capture zum Seitentitel zurück. Der anschließende vollständige Screenshot-/Gerätelauf steht noch aus; kein Design-Passed-Claim bis zu dessen Prüfung.

Zwischenlauf39834162881: alle acht Referenzaufnahmen erzeugt und geöffnet; 55/56 Gerätetests bestanden. Der verbleibende ZIP-Chooser-Test suchte den vollständigen Auftrag ohne Suche in einer inzwischen längeren LazyColumn. Test navigiert nun über eindeutige Referenzsuche und Scroll-Semantik; die reale Chooser-Prüfung bleibt unverändert. Im visuellen Abgleich Suchsymbol aus der Titelzeile verschoben, damit „Meine Aufträge“ die volle Breite erhält; doppelten Titel der Rechteansicht entfernt. Erneute vollständige Prüfung erforderlich.

Zwischenlauf39835785194: erneut55/56 Tests, alle25 Screenshots. Die acht gerenderten Ansichten wurden geöffnet; Auftragsüberschrift jetzt einzeilig, Rechteansicht ohne Doppeltitel. Verbleibender Fehler: der neu ergänzte Test rief performImeAction auf einem Feld mit ImeAction.Default auf. Der Test schließt die Tastatur nun direkt über InputMethodManager wie die übrigen Screenshot-Tests; Produktverhalten und echte Chooser-Assertion bleiben unverändert.
