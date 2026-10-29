# ContainerTrack 1.5.0 — Return-Abschluss und E-Mail

## Return dokumentieren

Jeder Container erhält zuerst ein Inhalts-Pflichtfoto. Zusätzlich sind Schadenfotos und sonstige Anfangsfotos möglich. Für den Abschluss sind pro Container genau so viele Leer-/Sauber-Fotos erforderlich wie Anfangsfotos einschließlich aller Zusatzfotos: 1 Inhalt + 2 Schäden bedeutet 3 Abschlussfotos. Erst wenn alle Container anfangs dokumentiert sind, können Abschlussfotos aufgenommen werden. Zusatzfotos, Neuaufnahmen und einzelne Löschungen sind bis zum Abschluss möglich; die angezeigte Zielanzahl passt sich an. Es werden keine anderen Fotos stillschweigend gelöscht.

## Erinnerung und Abschluss

Beim Anlegen eines neuen Returns wird der Termin `createdAt + 24 Stunden` dauerhaft gespeichert. Bearbeitung, E-Mail-Vorbereitung, App-Neustart und ZIP-Export starten die Frist nicht neu. Die Erinnerung bleibt offen, bis der Nutzer in der App ausdrücklich „E-Mail gesendet“ bestätigt. Das erfordert vollständige Fotodokumentation und eine E-Mail-Vorbereitung für den aktuellen Fotosatz. Jede Änderung an Fotos oder Containernummer macht eine vorherige Vorbereitung ungültig. Nach Versandbestätigung ist die Dokumentation schreibgeschützt.

Android muss Benachrichtigungen und für genaue Terminplanung genaue Alarme erlauben. Gerät ausgeschaltet, verweigerte Rechte oder Betriebssystembeschränkungen können die tatsächliche Anzeige verzögern/verhindern; eine plattformunabhängige garantierte Anzeige innerhalb 24h ist nicht möglich. Offene/fällige Returns bleiben zusätzlich in der App sichtbar. Benachrichtigungen öffnen den richtigen Auftrag. Die 30-Minuten-Logger-Erinnerung bei Lieferungen bleibt unabhängig bestehen.

## E-Mail statt Teams

Die aktiven Teams-Aktionen sind entfernt. Die App bereitet eine E-Mail in einem installierten Smartphone-Mailprogramm vor, mit Order Number unverändert als Betreff und sämtlichen Fotos sämtlicher Container dieses Auftrags. Empfänger wird im Mailprogramm gewählt; es gibt keinen erfundenen Standardempfänger und keinen eigenen Versanddienst. In der Galerie muss für E-Mail genau ein Auftrag gewählt werden; die E-Mail enthält dessen vollständige Fotodokumentation, nicht nur eine beliebige Teilmenge. Downloads und Kopieren bleiben möglich.

Die E-Mail-App wird über mailto-Handler ermittelt, Anhänge bleiben `image/jpeg` und erhalten nur temporäre Leserechte. Ohne geeignete Mail-App erscheint ein verständlicher Fehler statt eines Teams-/allgemeinen Share-Fallbacks. Die App kann weder den tatsächlichen Versand noch die Zustellung erkennen; Versandbestätigung ist eine bewusste Nutzerangabe. Größe/Anzahl der Anhänge werden angezeigt, über 20 MiB wird auf mögliche Mailanbieter-Limits hingewiesen. Es werden keine Anhänge automatisch weggelassen oder als gesendet markiert.

Lieferungsfotos und alte abgeschlossene Returns behalten Namen wie `0708_01.jpg`. Neue Returns verwenden z. B. `0708_vorher_01_inhalt.jpg`, `0708_vorher_02_schaden.jpg`, `0708_vorher_03_zusatz.jpg` und `0708_nachher_01_leer_sauber.jpg`. Nummerierung erfolgt pro Container und Phase. Die Originaldateien bleiben unverändert; Anhänge sind nummerierte Cachekopien.

## Bestandsdaten und Prüfung

Room migriert ohne Datenlöschung von 2 auf 3 (bzw. 1→2→3). Bestehende abgeschlossene/exportierte Returns bleiben im bisherigen Ablauf lesbar. Noch offene alte Returns erhalten den neuen Ablauf mit Frist aus ihrer ursprünglichen Erstellung, gegebenenfalls sofort fällig. Fotos, Auftragsreferenzen, alte private KT-Spalte und Historie bleiben erhalten. Neue Exportmetadaten haben schemaVersion 3 mit Foto-Phase/Kategorie.

Keine Benutzerverwaltung/PINs/Rollen, kein KT im aktiven Ablauf und kein zusätzlicher Online-Dienst. Nur Debug-APK 1.5.0 / versionCode 12. Update einer bestehenden Installation setzt dasselbe Signaturzertifikat voraus; nicht zur Aktualisierung deinstallieren. Tatsächliche Prüfstände und Grenzen stehen in [QA_STATUS.md](QA_STATUS.md).
