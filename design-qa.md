# Native Android — Referenzabgleich 1.5.0

Die native Kotlin/Compose-App behält Graphit/Lime, Inter, die Container-Markenleiste und die Navigation Aufträge–Kamera–Galerie der ausgewählten Bildvorlagen. Aktuelle fachliche Anpassungen: kein KT, keine Anmeldung oder Rollen, lokale Galerie und E-Mail statt Teams. Return ergänzt Anfangs-/Abschlussphasen und optionale Schaden-/Zusatzfotos.

## Zuordnung

| Vorlage | Native Ansicht | Capture |
| --- | --- | --- |
| 1000036738 Meine Aufträge | OrderListScreen mit aktivem Auftrag, großen Kennungen und kompakten letzten Aufträgen | 20-reference-orders |
| 1000036739 Neuer Auftrag | SetupScreen mit Lieferung/Rückgabe, Order Number, Erstcontainer/OCR und Pflichtdetails | 21-reference-new-order |
| 1000036740 Fotos prüfen | PhotoReviewScreen mit echten lokalen Bildern und Status | 22-reference-photo-review |
| 1000036742 Galerie | Lokale Containerliste mit echten Foto-Thumbnails und Suche | 23-reference-gallery |
| 1000036745 Fotoauswahl | Raster, Originalfotos speichern, vollständigen Auftrag per E-Mail vorbereiten | 24-reference-photo-selection |
| 1000036744 Anmeldung | Auf Nutzerwunsch ersetzt durch lokale Geräteeinstellungen ohne Konto | 25-reference-settings |
| 1000036741 Teams-Übertragung | Auf Nutzerwunsch ersetzt durch E-Mail-Vorbereitung mit Order Number und allen Anhängen | 26-reference-upload |
| 1000036743 Benutzer & Rechte | Auf ausdrücklichen Nutzerwunsch entfallen | kein Capture |
| Neue Return-Anforderung | Anfangs-/Sauber-Zähler, optionale Schaden-/Zusatzfotos, einzelne Fotoansicht | 28-reference-return-workflow, 28b/28c |

## Messwerte und Anpassungen

Referenzen 592×1280, Zielcapture 1080×2340 mit nahezu identischem Seitenverhältnis. Grundfläche #151A1D, Lime #BEFF42, Weiß #F8FAF9, Linien #3B4348, Inter mit OFL-Lizenz. Markenleiste 60dp, Navigation 72dp, Außenabstand 16dp, primäre Trefferfläche mindestens 50dp, Radien 5–10dp. Haupttitel 44sp; kurze Kennungen 56sp, längere Referenzen 32sp und begrenzte Zeilen. Kein Glow.

Fachliche Änderungen haben Vorrang vor den früheren Beispieldaten: sechs Delivery-Fotos; Return mindestens ein Inhaltsfoto und je Container genau gleich viele Abschlussfotos wie sämtliche Anfangsfotos. Die E-Mail-Ansicht meldet lokal vorhandene Anhänge und nur eine ausdrücklich vom Nutzer bestätigte Sendung. Kein behaupteter Server-Upload.

## Sichtprüfung und Grenzen

Neun aktuelle Captures wurden aus dem API-35-Emulator gesichert und einzeln visuell geprüft: Übersicht, Formular, Fotoprüfung, Galerie, Einstellungen, E-Mail und drei Return-Ausschnitte. Markenleiste, Inter, Graphit/Lime, Navigation, lesbare Auftrags-/Containernummern und die Return-Zähler sind sichtbar. Die neuen Return-Umrissschaltflächen wurden mit 5dp-Radien und neutralen Rahmen an die gemeinsame Gestaltung angeglichen und erneut aufgenommen.

[Übersicht 1.5.0](design/verification/reference-ui-v1.5.0.jpg). Die letzten drei Bilder zeigen bewusst den isoliert geprüften Return-Inhalt ohne App-Chrome. Die Referenz-Fotoauswahl `24-reference-photo-selection` fehlt; ihre abschließende UI-Prüfung wurde durch den Cloud-Neustart unterbrochen. Eine vollständige visuelle Freigabe wird daher nicht behauptet. Es gibt keine freigegebenen Goldens und keinen Pixelvergleich.

Aktuelle Testergebnisse und Grenzen: [QA_STATUS.md](docs/QA_STATUS.md). Reale Mailprogramme, OEM-Kamera, SAF-Provider, große Systemschrift und TalkBack benötigen zusätzliche Prüfung auf dem Nutzergerät.
