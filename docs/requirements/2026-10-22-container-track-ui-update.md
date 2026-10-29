# Container Track – UI-Redesign „Terminal Dark“

**Stand:** 22.10.2026  
**Status:** Fachliche UI-Entscheidungen abgeschlossen; Umsetzung noch nicht gestartet  
**Zielsystem:** Bestehende Container-Track-Web-App/PWA aus dem öffentlichen GitHub-Projekt  
**Umfang:** Oberfläche, Informationsdarstellung, Navigation und Bedienung modernisieren – **keine neue native Android-App**, kein Austausch der vorhandenen Datenspeicherung und keine zusätzliche kostenpflichtige Infrastruktur.

## 1. Ziel und Grenzen

Die Mitarbeiter sollen mehrere KT-Aufträge und Container bei der Fotoaufnahme schnell erfassen, den Fotofortschritt erkennen, den Zustand der Teams-Übertragung verstehen und benötigte Fotos über die Galerie wiederfinden können. Die App bleibt auf Android und Desktop nutzbar. Bestehende fachliche Funktionen, Pflichtfelder, OCR-/Kamerafunktionen, Exportmöglichkeiten, Timer-Logik und Offline-Speicherung sollen durch die UI-Überarbeitung nicht beschädigt werden.

**Wichtig:** Diese Spezifikation beschreibt die *gewünschte Darstellung*. Sie ist keine Bestätigung, dass eine Microsoft-365-Anmeldung, eine geräteübergreifende Galerie, die Rollenprüfung oder ein automatischer Teams-Upload bereits technisch implementiert ist. Fehlende Funktionen dürfen nicht durch funktionslose Schaltflächen oder erfundene Statusmeldungen vorgetäuscht werden; sie benötigen gegebenenfalls einen separat freizugebenden Funktionsumfang.

## 2. Verbindliche UI-Entscheidungen

| Bereich | Gewähltes UI-Verhalten |
|---|---|
| Design | Terminal Dark: dunkle Graphitflächen, zurückhaltende limettengrüne Aktionen, hohe Lesbarkeit |
| Startseite | Kompakte KT-Liste mit KT-Nummer, Lieferung/Return, Containeranzahl, Foto-Fortschritt, Status |
| Suche | Suchfeld nach KT-Nummer; Filterchips **Aktiv**, **Befüllt-Foto fehlt**, **Upload-Fehler**, **Alle**; Start auf **Aktiv** |
| KT antippen | Bottom-Sheet-Schnellmenü **Fotografieren**, **Details**, **Teilen** |
| Fotostatus | Fünf eindeutig benannte Fotoplätze pro Container, fehlende Plätze antippbar; Befüllt-Foto später ergänzbar |
| Mehrere Container | Vertikal angeordnete, aufklappbare Karten mit Nummer, `x/5` und Zustand |
| Uploadstatus | Direkt im KT-Eintrag: **Wartend**, **Wird hochgeladen**, **Erfolgreich**, **Fehler**, ggf. Fortschritt |
| Fehler | Roter Fehlerstatus mit Kurzursache und **Erneut versuchen** (wenn technisch verfügbar) |
| Offline | Schmaler Hinweis oben mit Anzahl wartender Fotos, ohne Fotoaufnahme zu blockieren |
| Galerie | Eigener Tab, Bilderraster, Filter nach KT, Container und Mitarbeiter, Vollbildansicht |
| Teilen | Auswahl **Teams** / **Android-Teilen**; nur nummerierte JPG-Fotos, kein ZIP, Excel, TXT im Team-Share |
| Hauptnavigation | **Aufträge – Galerie – Profil**, zusätzlicher hervorgehobener `+`-Button für neuen KT |
| Neuer KT | Einseitiges kompaktes Formular für Lieferung/Return, KT-Nummer und mehrere Container; existierende Pflichtfelder gelten weiter |
| 30-Minuten-Timer | Sichtbar sowohl in KT-Liste als auch in Details; bestehende Timer-Logik unverändert |
| Profil | Name, Microsoft-365-Konto (sofern integriert), Rolle, Verbindungsstatus; Admin erhält zusätzlichen Bereich für Gesamtgalerie und Benutzerübersicht |

## 3. Navigationsmodell

Die Bottom-Navigation bleibt in allen regulären Hauptansichten sichtbar:

- **Aufträge:** zentrale Arbeitsliste, Suche, Filter und neue KT-Aufträge.
- **Galerie:** nach Fotos suchen, Vorschaubilder ansehen, Details und Teilen öffnen.
- **Profil:** eigene Kontoinformation, Verbindung, gegebenenfalls Admin-Abschnitt und Abmeldung.

Der grüne `+`-Button öffnet **Neuer KT**. Er darf weder die Aufnahmefläche noch wichtige Aktionen verdecken. Kamera- und Detailscreens zeigen eine klare Zurücknavigation zum jeweiligen KT. Das Bottom Sheet zum KT lässt sich durch Zurück, Schliessen oder Tippen ausserhalb verlassen.

## 4. Die neun UI-Bereiche

### 4.1 Aufträge / Startseite

**Kopf:** App-Titel, Microsoft-/Offline-Verbindungsstatus und gegebenenfalls Nutzernamen-Kürzel. Darunter nur bei Offline-Zustand ein kompakter Hinweis, z. B. `Offline · 15 Fotos warten`.

**Filter:** Suche `KT-Nummer suchen …`, horizontale Chips `Aktiv`, `Befüllt-Foto fehlt`, `Upload-Fehler`, `Alle`; Startfilter `Aktiv`. Filterwahl und Suchbegriff werden beim Öffnen eines Auftrags sinnvoll beibehalten.

**Kompakte KT-Zeile:**

1. KT-Nummer und Typ (`Lieferung`/`Return`).
2. Anzahl Container und gesamter Fotofortschritt, z. B. `3 Container · 12/15 Fotos`.
3. Klarer Statuschip, z. B. `In Bearbeitung`, `Wartend`, `Upload läuft`, `Übertragen`, `Fehler`.
4. Bestehender Countdown, sofern aktiv (z. B. `18:42`).
5. Optional ein schmaler Upload-Fortschrittsbalken statt zusätzlicher Detailzeilen.

Keine aufgeblähten Dashboard-KPIs oder grossen Karten pro KT. Tap auf die Zeile öffnet das Schnellmenü. Abgeschlossene Einträge bleiben über `Alle` auffindbar. Der `+`-Button ist stets schnell erreichbar.

### 4.2 Neuer KT-Auftrag

Ein Screen mit Segmentauswahl `Lieferung` / `Return`, KT-Nummer, bestehenden Pflichtfeldern und mehreren Containern. Für jeden Container gibt es eine kurze Eingabezeile für die Nummer plus **Weiteren Container hinzufügen**. Speichern/Starten ist als einzige dominante Aktion markiert.

Bestehende Validierung bleibt erhalten. Für **Lieferung** werden `Quick` und `Additional Work` nicht neu zu Pflichtfeldern gemacht. Keine zusätzlichen Informationen nur für das neue Design verlangen. Lange Formulare erhalten saubere Abschnittsüberschriften und eine bedienbare Bildschirmtastatur-Navigation.

### 4.3 KT-Schnellmenü

Das Bottom Sheet zeigt drei grosse Aktionen:

- **Fotos aufnehmen** – öffnet die passende Fotoübersicht des KT.
- **Auftragsdetails** – öffnet die Containerliste mit Fortschritt und Timer.
- **Teilen** – öffnet die festgelegte Share-Auswahl.

Bei fehlenden oder noch nicht verfügbaren Fotos erklärt die UI, was sich gerade teilen lässt. Das Menü darf keinen Upload als erfolgreich anzeigen, solange keine Bestätigung vorhanden ist.

### 4.4 KT-Details / mehrere Container

Vertikale, aufklappbare Containerkarten. Geschlossen: Containernummer, `x/5 Fotos`, Status (`Befüllt-Foto fehlt`, `Vollständig` etc.) sowie klarer Pfeil. Geöffnet: die fünf Fotoplätze und Aktionen zum Fotografieren/Ansehen/Ersetzen. Beim Wechsel zu einem anderen Container kann die vorherige Karte geschlossen werden, damit die Liste kompakt bleibt.

Der KT-Kopf zeigt Gesamtstatus, Gesamtfortschritt, gegebenenfalls Mitarbeiter und den unveränderten 30-Minuten-Timer. Bei `n` Containern beträgt das erwartete Fotosoll `5 × n`, sofern das bestehende Fachmodell alle fünf Fotos erfordert.

### 4.5 Fotoübersicht und Kamera

Fünf Slots pro Container: **Vorne, Hinten, Links, Rechts, Befüllt**. Ein Slot zeigt entweder das echte Vorschaubild mit Häkchen oder ein gut erkennbares Kamera-/Plus-Symbol. Slot-Tap startet die vorhandene Aufnahme beziehungsweise öffnet ein bestehendes Bild zur Kontrolle.

Der **Befüllt**-Slot bleibt offen, wenn zunächst nur leere Container fotografiert werden. Es muss möglich sein, zuerst bei allen Containern die vier Aussenfotos aufzunehmen und erst später die Befüllt-Fotos zu ergänzen. Echte Dateinamen und OCR-Zuordnungen werden durch das neue UI nicht verändert.

### 4.6 Teams-Übertragung und Teilen

**Automatischer Uploadstatus innerhalb des KT**, kein obligatorischer eigener Haupttab. Fachlich bleibt vorgesehen, dass die Übertragung erst nach Vollständigkeit aller Container eines KT angestossen wird. Der bestehende reale Uploadstatus hat Vorrang vor jeder UI-Annahme.

Statusabbildung:

- `Wartend`: Upload noch nicht begonnen, z. B. fehlende Verbindung.
- `Wird hochgeladen`: Zähler und Balken nur für tatsächlich bestätigte Fortschrittsdaten.
- `Erfolgreich`: erst nach bestätigter Übertragung aller betroffenen Bilder.
- `Fehler`: rotes Kennzeichen, kurze Ursache, echte Wiederholungsaktion, soweit vorhanden.

Manuelles **Teilen** öffnet ein Bottom Sheet mit `Teams` und `Android-Teilen`. Nur die vorgesehenen, nummerierten **JPG-Dateien** werden übergeben – **keine ZIP-, Excel-, TXT- oder Sidecar-Dateien**. Falls die direkte Teams-Schnittstelle im aktuellen Produkt noch nicht vorhanden oder für das Firmenkonto gesperrt ist, darf die UI nicht behaupten, sie hätte gesendet; die vorhandene Android-Teilen-Funktion ist der ehrliche Fallback.

### 4.7 Galerie und Foto-Vollbild

Eigener Tab mit **klassischem Vorschaubild-Raster** (nicht automatisch nach KT gruppierte Karten). Oben Suche/Filter: KT-Nummer, Containernummer, Mitarbeiter. Treffer zeigen echte Vorschaubilder. Beim Öffnen: Grossansicht mit eindeutiger Zuordnung, Zurück, Download/Teilen, sofern diese Aktionen schon angeboten werden können.

Die Admin-Ansicht soll alle **zentral hochgeladenen und tatsächlich verfügbaren** Fotos über Mitarbeiter hinweg auffindbar machen. Die Galerie kann nicht automatisch auf rein lokal gespeicherte, noch nicht synchronisierte Fotos eines fremden Smartphones zugreifen.

### 4.8 Profil

Gemeinsames Layout mit Nutzername, Microsoft-365-Identität soweit vorhanden, Rolle (`Mitarbeiter` / `Admin`) und Verbindungsstatus. Darunter vorhandene Einstellungen und Abmelden. Keine Kennwörter im UI speichern oder neue eigene Passwortanmeldung erfinden.

### 4.9 Admin-Abschnitt

Im Profil bei berechtigten Admins zusätzliche Menüpunkte: **Gesamte Team-Galerie** und **Benutzerübersicht**. Der Admin-Bereich nutzt dasselbe Terminal-Dark-Design; er ist keine zweite App.

**Berechtigungshinweis:** Das visuelle Verstecken von Admin-Menüpunkten schützt keine Daten. Zugriff auf Firmenfotos und Benutzer muss durch die tatsächlich vorhandene Microsoft-365-/Serverberechtigung abgesichert sein. Eine öffentliche GitHub-PWA darf keine geheimen Microsoft-Zugangsdaten, Tokens oder Client-Secrets enthalten. Wenn die erforderliche Berechtigung/Anbindung fehlt, ist die Aktion gesperrt und erklärt den Grund.

## 5. Gestaltungsrichtlinien

- **Grundfläche:** tiefes Graphit/Schwarz, klare Ebenen statt starker Glassmorphism-Effekte.
- **Primäraktion:** gedecktes Neon-Lime nur für wesentliche Buttons, ausgewählte Tabs, Foto-Fertigstand und Fortschritt.
- **Statusfarben:** Grün für bestätigt/erfolgreich, Gelb/Amber für unvollständig oder wartend, Rot für Fehler. Status zusätzlich immer als Text oder Icon zeigen.
- **Lesbarkeit:** helle Texte auf dunklen Flächen, ausreichend hoher Kontrast auch im hellen Lagerumfeld; keine dunkelgraue Schrift auf schwarzem Grund.
- **Bedienflächen:** gross genug für schnelle Bedienung mit einer Hand, keine winzigen Foto-Slots ohne klare Trefferzone.
- **Typografie:** nüchtern, Zahlen und Fotofortschritt gut scanbar, Timer mit tabellarischen Ziffern.
- **Responsive:** mobile Ansicht priorisieren; am Desktop breiteres Listenlayout und mehr Galerie-Spalten, aber dieselben Funktionen.
- **Sprache:** bestehende deutsche Begriffe konsistent verwenden (`Befüllt`, `Lieferung`, `Return`, `KT`).
- **Animationen:** kurze Übergänge, keine verlangsamenden Schmuckanimationen.

## 6. Zustands- und Informationsregeln

| Ereignis | Sichtbare Reaktion |
|---|---|
| Vier Fotos vorhanden, Befüllt fehlt | `4/5`, amber Status `Befüllt-Foto fehlt`, fünfter Slot antippbar |
| Alle Container vollständig | `5/5` je Container, gesamter KT vollständig; tatsächlichen Uploadstatus anzeigen |
| Upload wartet wegen Offline | Offline-Leiste und `Wartend` beim betreffenden KT; Fotoaufnahme bleibt offen |
| Upload läuft | Echter Fortschritt pro KT; andere Aufträge weiter bedienbar |
| Upload bestätigt erfolgreich | `Erfolgreich` in der Liste und optional kurze Bestätigung |
| Upload fehlgeschlagen | Roter KT-Status, Kurzursache, verfügbare Aktion `Erneut versuchen` |
| Timer aktiv | Derselbe aktuelle Countdown in Liste und Detail, ohne Änderung am Start-/Endereignis |
| Keine Galerie-Treffer | Leerer Zustand mit Hinweis, Filter zu ändern; keine erfundenen Fotos |
| Mitarbeiter ohne Adminrecht | Normales Profil; keine irreführenden Admin-Aktionen; Berechtigung serverseitig wirksam |

## 7. Nicht Bestandteil dieses UI-Updates

- Neue native Android-App mit Kotlin/Jetpack Compose.
- Neue Datenbank oder Umzug bestehender Fotos/Datensätze.
- Neues Backend, neue Cloud oder kostenpflichtige API-Dienste.
- Neue Login- oder Microsoft-Graph-Integration, falls technisch noch nicht vorhanden.
- Neue serverseitige Rollen-/Rechteverwaltung oder Änderung der Teams-Kanalberechtigungen.
- Änderung des bestehenden 30-Minuten-Timers oder der Automatikregeln.
- Änderung vorhandener Excel-/TXT-Exportfunktionen **ausserhalb** des festgelegten Foto-Share-Flows.

## 8. Umsetzungs- und Prüfreihenfolge

1. Bestehende PWA-Dateien und aktuelle Daten-/Statusfelder prüfen. Die UI ausschliesslich an tatsächlich verfügbare Daten binden.
2. Terminal-Dark-Farben, Typografie, Header, Bottom-Navigation und kompakte KT-Liste vereinheitlichen.
3. KT-Schnellmenü, kompaktes Neuer-KT-Formular, aufklappbare Containerkarten und fünf Foto-Slots gestalten.
4. Galerie-Raster mit Filtern und Vollbild sowie Profil/Admin-Darstellung aktualisieren.
5. Anzeige für Upload/Offline/Fehler/Timer an bestehende Statusquellen anschliessen; Teams-/Android-Share-Optionen nur mit real verfügbaren Aktionen verbinden.
6. Regressionsprüfung: vorhandene Aufträge und Fotos, Lieferung/Return, mehrere Container, Befüllt-Foto später, OCR, Offline, Timer, Share und bestehende Exporte.

### Abnahmekriterien

- Auf Android sind mindestens mehrere KT-Zeilen ohne Scrollen sichtbar; ihre Kerninformationen sind lesbar.
- Suchfeld, alle vier Filterchips, Bottom Sheet und Navigation sind per Touch bedienbar.
- Alle fünf Fotoplätze und `x/5` sind je Container eindeutig erkennbar; mehrere Container lassen sich aufklappen.
- Ein späteres Befüllt-Foto wird korrekt im Fortschritt berücksichtigt.
- Timer in Liste und Details zeigt denselben realen Wert.
- Online/Offline und Uploadzustände spiegeln den tatsächlichen Systemzustand wider.
- Bei Uploadfehlern werden lokale Fotos nicht gelöscht; Wiederholen ist nur aktiv, wenn es eine echte Aktion gibt.
- Teams-/Android-Teilen übergibt ausschliesslich die ausgewählten nummerierten JPGs.
- Galerie zeigt reale Bilder und Filterergebnisse; Adminansicht verspricht keinen Zugriff auf nicht synchronisierte Fremdgeräte.
- Kein bestehender Prozess, Export oder Pflichtfeld ist durch die neue Oberfläche verloren gegangen.
- Keine Secrets oder Firmenzugänge liegen im öffentlichen GitHub-Quelltext.

## 9. Übergabe und offene technische Prüfung

Diese UI-Vorgabe ist fachlich anhand der bisherigen Entscheidungen abgeschlossen. Für eine sichere Umsetzung wird **zuerst das bestehende GitHub-Repository beziehungsweise die aktuelle App-Version benötigt**, damit Farben, Komponenten, Datenfelder und tatsächliche Funktionen erhalten bleiben. Insbesondere muss geprüft werden, welche Microsoft-365-/Teams-Anbindung und welche Adminrechte heute bereits funktionieren. Nicht vorhandene Schnittstellen sind **kein** stillschweigender Teil eines blossen UI-Updates.

Die zuvor erstellte **Kotlin-/Native-Android-Neuentwicklungsspezifikation** ist für diesen Auftrag ausdrücklich **nicht massgeblich**.
