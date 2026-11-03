# Container Track – Codex Update Handoff V1.3

**Datum:** 22.10.2026  
**Ziel:** Bestehende native Android-App aktualisieren und redesignen – **kein Neuaufbau**.  
**Repository:** `samkruw/ContainerTrack`  
**Arbeitsbranch:** `codex/native-android-app`  
**App-ID:** `ch.zerogen.containertrack`

---

## 0. Verbindliche Arbeitsweise

Du arbeitest **UPDATE-ONLY** auf dem bestehenden nativen Android-Projekt.

### Nicht tun
- Keine neue PWA bauen.
- Keine zweite Android-App anlegen.
- Keine bestehende Room-/CameraX-/OCR-/Export-Logik unnötig ersetzen.
- Kein neues kostenpflichtiges Backend.
- Keine Passwörter, Tokens, Client-Secrets oder Firmenzugänge in GitHub einchecken.
- Keine Fake-Statuswerte oder funktionslosen Buttons einbauen.
- Keine Teams-Erfolgsmeldung zeigen, bevor der Upload tatsächlich bestätigt wurde.
- Keine ZIP-, TXT-, Excel- oder META-Dateien an Teams senden.

### Zuerst
1. Bestehenden Branch vollständig analysieren.
2. Bestehende Datenmodelle, Room-Schema, ViewModel-State, Timer, Export, CameraX, OCR, Share-Flow und Theme inventarisieren.
3. Bestehende Tests ausführen.
4. Danach minimal-invasive Änderungen umsetzen.
5. Rückwärtskompatibilität für vorhandene lokale Aufträge wahren.

---

# 1. Aktueller Produktstand, der erhalten bleiben muss

Die bestehende App ist eine native Android-App mit Kotlin / Jetpack Compose.

Vorhanden:
- Room
- CameraX
- Offline OCR mit gebündeltem ML Kit
- Offline-first Workflow
- Android Share
- SAF / ZIP-Export
- Logger-Erinnerung mit AlarmManager / Receiver
- Boot-Restore
- interne Fotoablage
- Delivery und Return
- mehrere Container
- bestehendes Dark Theme / GlassSurface-Komponenten

Bestehende fachliche Regeln dürfen nur dort geändert werden, wo dieses Dokument es ausdrücklich verlangt.

---

# 2. Wichtigste fachliche Änderung: Delivery in zwei Phasen

## 2.1 Delivery – sechs Pflichtfotos pro Container

Für **Delivery** sind weiterhin genau sechs Bilder pro Container erforderlich:

1. `01` – Leerer Innenraum
2. `02` – Aussenseite Front
3. `03` – Aussenseite Rückseite / Türen
4. `04` – Aussenseite links
5. `05` – Aussenseite rechts
6. `06` – Befüllter Innenraum

Dateinamen bleiben:

`<4-stellige-container-nummer>_01.jpg` bis `_06.jpg`

Beispiel:
- `0708_01.jpg`
- `0708_02.jpg`
- ...
- `0708_06.jpg`

Führende Null muss erhalten bleiben.

## 2.2 Spätes Befüllt-Foto

Der bisherige Ablauf darf den Mitarbeiter **nicht** zwingen, Foto 6 sofort aufzunehmen.

Bei mehreren Containern soll folgender Workflow möglich sein:

### Phase A – Leerdokumentation
Für alle Container werden zuerst Fotos 1–5 aufgenommen.

Beispiel:
- 0708 → 5/6
- 0710 → 5/6
- 0712 → 5/6

Danach Status:

`Bereit zum Befüllen`

### Phase B – Befüllung
Später wird pro Container nur noch Foto 6 ergänzt.

Beispiel:
- 0708 → 6/6
- 0710 → 5/6
- 0712 → 5/6

Der Auftrag bleibt unvollständig.

Erst wenn **jeder Container 6/6** erreicht hat:

`Vollständig`

## 2.3 Keine Doppelaktionen
Der Übergang auf `Vollständig` darf nur einmal die Abschlussautomatik auslösen.

Die Logik muss idempotent sein.

---

# 3. Automatische Abschlussautomatik

Wenn ein Delivery-Auftrag erstmals vollständig wird:

## A. Logger-Timer starten
- 30-Minuten-Timer lokal starten.
- Genau einmal pro Auftrag.
- Nicht vom Internet abhängig.
- Nicht vom Teams-Upload abhängig.
- Kein Reset bei Retry, Share oder erneutem Öffnen.
- Bestehende Alarm-/Boot-Restore-Logik weiterverwenden.

**Wichtig:** Der Timer startet künftig bei vollständigem Delivery-Auftrag – nicht erst durch ZIP-Export.

## B. Teams-Upload anstossen
Automatischen Upload starten bzw. in die lokale Sync-Warteschlange stellen.

Wenn offline:
- Auftrag bleibt vollständig.
- Timer läuft trotzdem.
- Uploadstatus = `Wartet auf Verbindung`.
- Sobald wieder online: Upload automatisch fortsetzen.

---

# 4. Teams-Ziel

Der feste Microsoft-Teams-Kanal heißt:

**`Op530-Lal`**

Die App darf keine geheimen Firmenzugänge enthalten.

Der Kanalname darf als Konfiguration im Code vorhanden sein. Für die tatsächliche Zielauflösung sollen nach Möglichkeit stabile Microsoft IDs gespeichert/verwendet werden, nachdem sie für die Firmenumgebung eindeutig ermittelt wurden.

---

# 5. Teams – ausschließlich nummerierte Fotos

Beim automatischen Teams-Upload werden **nur die JPG-Fotos** des abgeschlossenen Auftrags übertragen.

## Delivery
Pro Container:
- `_01.jpg`
- `_02.jpg`
- `_03.jpg`
- `_04.jpg`
- `_05.jpg`
- `_06.jpg`

## Return
Nur die fachlich bestehenden nummerierten Return-Fotos übertragen.

### Niemals an Teams senden
- ZIP
- TXT
- Excel
- CSV
- META.json
- Room-Datenbank
- Audit-/Debug-Dateien
- Logger-Daten
- interne IDs als separate Dateien

Der bestehende vollständige lokale Export darf separat erhalten bleiben.

---

# 6. Teams-Authentifizierung / mehrere Benutzer

Container Track ist ein öffentliches GitHub-Projekt. Deshalb:

## Pflicht
- Microsoft OAuth / MSAL als **Public Client** verwenden.
- Kein Client Secret in Android.
- Kein Benutzerpasswort speichern.
- Keine gemeinsam genutzten Teams-Zugangsdaten.
- Keine Tokens im Repository.
- Tokens nur über die dafür vorgesehene sichere MSAL-/Android-Tokenablage verwalten.
- Abmelden und Benutzerwechsel unterstützen.
- Firmenkonto / Tenant nach Möglichkeit auf die gewünschte Organisation einschränken.

## Benutzerprinzip
Jeder Mitarbeiter verwendet auf seinem Smartphone sein **eigenes Firmenkonto**.

Die App soll anzeigen:
- angemeldeter Name
- Firmenkonto
- Verbindungsstatus
- optional Rolle / Zugriff, soweit real verfügbar

Die App darf keinen eigenen E-Mail-/Passwort-Login erfinden.

---

# 7. Upload-Queue

Ein robuster Sync-Mechanismus wird benötigt.

Bevorzugt:
- WorkManager
- Netzwerkbedingung
- persistente lokale Queue / Room-State
- Retry mit Backoff
- App-Neustart überstehen
- keine doppelten Uploads

## Zustände
Mindestens:
- `LOCAL_INCOMPLETE`
- `READY_FOR_FILL`
- `COMPLETE_PENDING_UPLOAD`
- `UPLOADING`
- `SYNCED`
- `UPLOAD_ERROR`

UI-Texte auf Deutsch:
- In Bearbeitung
- Bereit zum Befüllen
- Wartet auf Upload
- Wird hochgeladen
- Übertragen
- Fehler

## Idempotenz
Pro Datei muss lokal festgehalten werden, ob sie erfolgreich hochgeladen wurde.

Bei Retry:
- bereits bestätigte Bilder nicht erneut hochladen
- nur fehlende/fehlgeschlagene Dateien wiederholen

Lokale Fotos bei Uploadfehler **niemals löschen**.

---

# 8. Manueller Fallback

Wenn der direkte Microsoft-/Teams-Upload nicht funktioniert:

`Teilen` → Bottom Sheet:

1. **Teams**
2. **Android-Teilen**

Für beide Wege werden ausschließlich die vorgesehenen nummerierten JPG-Dateien übergeben.

Kein ZIP.

Wenn kein direkter Teams-Upload technisch möglich ist, darf die App nur den echten Android-Share-Fallback anbieten und keinen Erfolg vortäuschen.

---

# 9. Redesign – Terminal Dark

Die mitgelieferte UI-Spezifikation ist die gestalterische Grundlage.

## Stil
- tiefes Graphit / Schwarz
- hohe Lesbarkeit
- gedecktes Lime für Primäraktionen
- Grün = bestätigt
- Amber = wartend / unvollständig
- Rot = Fehler
- keine übertriebenen Glow-/Glass-Effekte
- bestehende GlassSurface-Komponenten dürfen nur dezent weiterverwendet werden, sofern sie zum neuen Terminal-Dark-System passen
- klare Kontraste im hellen Lagerumfeld
- Touch-Flächen gross genug für Einhandbedienung

## Hauptnavigation
Bottom Navigation:
- **Aufträge**
- **Galerie**
- **Profil**

Zusätzlich hervorgehobener `+`-Button:
- Neuer Auftrag

---

# 10. Aufträge-Startseite

Kompakte Liste statt grosser Dashboardkarten.

Pro Auftrag anzeigen:
- KT / Order / Return Kennung entsprechend bestehendem Fachmodell
- Lieferung oder Return
- Anzahl Container
- Gesamt-Fotofortschritt
- aktueller Workflowstatus
- Teams-Uploadstatus
- Logger-Countdown, wenn aktiv

Suche:
- KT-/Auftragsnummer

Filter:
- Aktiv
- Befüllt-Foto fehlt
- Upload-Fehler
- Alle

Standard:
- `Aktiv`

Tap auf Auftrag:
Bottom Sheet mit:
- Fotos aufnehmen
- Auftragsdetails
- Teilen

---

# 11. Neuer Auftrag

Bestehende Fachfelder erhalten.

## Delivery
Quick und Additional Work **nicht** als neue Pflichtfelder einführen.

## Return
Bestehende Pflichtfelder unverändert beibehalten.

Mehrere Container direkt erfassbar.

Die App darf keine neuen Pflichtinformationen nur fürs Redesign verlangen.

---

# 12. Auftragsdetails / Multi-Container

Vertikale aufklappbare Containerkarten.

Bei Delivery geschlossen:
- Containernummer
- `x/6 Fotos`
- Status

Beispiele:
- `5/6 · Befüllt-Foto fehlt`
- `6/6 · Vollständig`

Geöffnet:
- sechs klar benannte Fotoslots
- Vorschau
- Fotografieren
- Ansehen
- Ersetzen

Foto 6:
**Befüllt**

Bei mehreren Containern soll besonders schnell sichtbar sein, wo das Befüllt-Foto noch fehlt.

---

# 13. Kamera / Fotoslots

Delivery-Slots:
1. Leer
2. Front
3. Rückseite / Türen
4. Links
5. Rechts
6. Befüllt

Vorhandene CameraX-/OCR-Funktionen weiterverwenden.

Kein unnötiger Kamera-Neubau.

OCR:
- 4-stellige Container-Nummer
- führende Null erhalten
- manueller Fallback
- bestehende Duplicate-Prüfung erhalten

---

# 14. Galerie

Eigener Haupttab.

Ziel:
reale lokal bzw. zentral tatsächlich verfügbare Bilder anzeigen.

## Filter
- Auftrag / KT
- Container
- Mitarbeiter, falls aus real synchronisierten Daten verfügbar

## Ansicht
- Thumbnail-Raster
- Vollbildansicht
- klare Zuordnung
- Teilen / Download nur wenn technisch real vorhanden

Keine Fake-Cloud-Galerie.

Wenn Bilder eines anderen Mitarbeiters nicht über die zentrale Microsoft-Ablage abrufbar sind, darf die App sie nicht vortäuschen.

---

# 15. Profil

Anzeigen:
- Name
- Microsoft-Firmenkonto
- Verbindungsstatus
- Rolle, sofern real ermittelbar
- Abmelden
- Konto wechseln

Kein Passwortfeld.

## Admin
Wenn die Firmenberechtigungen den Benutzer tatsächlich als Admin/geeigneten Leser ausweisen, darf zusätzlich angeboten werden:
- Team-Galerie
- Benutzer-/Uploader-Übersicht

UI-Verstecken allein ist keine Zugriffskontrolle.

---

# 16. Datenschutz / Public GitHub

Vor jedem Commit prüfen:
- keine Passwörter
- keine OAuth-Tokens
- keine Refresh-Tokens
- keine Client-Secrets
- keine privaten Schlüssel
- keine Firmen-Cookies
- keine Test-Accounts mit echten Zugangsdaten
- keine `.env` mit Secrets
- keine geheimen URLs mit eingebetteten Credentials

Falls Build-Konfigurationen nötig sind:
- öffentliche IDs dürfen versioniert werden, wenn sie keine Secrets sind
- echte Secrets dürfen nicht in der APK benötigt werden
- Android-App als Public Client behandeln

---

# 17. Bestehende Exporte

Die bisherige lokale Exportfunktion bleibt erhalten, sofern sie heute funktioniert:
- ZIP teilen
- ZIP speichern
- TXT / bestehende Exportstruktur

Diese Exporte sind **getrennt** vom Teams-Foto-Upload.

Teams bekommt nur nummerierte JPGs.

---

# 18. Return

Return darf durch das Delivery-Redesign nicht beschädigt werden.

Bestehende Regeln beibehalten, insbesondere:
- Logger-Stopp-Bestätigung, falls aktuell erforderlich
- Return Number
- Container-Typ / Order
- Quantity
- Quick
- Additional Work
- vorhandener Return-Fotoworkflow
- bestehende Exportdaten

Neue Teams-Synchronisation darf Return-Fotos analog hochladen, aber nur die nummerierten JPG-Dateien.

---

# 19. Migration

Falls Room-Schema erweitert werden muss, Migration sauber implementieren.

Bevorzugte neue Felder nur wenn erforderlich, z. B.:
- workflowPhase
- completionTriggeredAt
- loggerStartedAt
- teamsSyncState
- teamsSyncError
- lastSyncAttemptAt
- per-photo remote state / uploadedAt / remote identifier

Bestehende lokale Jobs dürfen nicht verloren gehen.

Kein destructive migration fallback für produktive Nutzerdaten.

---

# 20. Tests

## Unit Tests
Mindestens:
- Delivery mit 1 Container: 5/6 ist nicht vollständig
- 6/6 wird vollständig
- 3 Container: 18/18 erst dann vollständig
- Foto 6 kann später ergänzt werden
- Completion-Event wird nur einmal ausgelöst
- Timer wird nur einmal gestartet
- Reopen ändert Timer nicht
- Retry startet Timer nicht neu
- Teams-Queue wird nur bei vollständigem Auftrag angelegt
- Teams-Dateiliste enthält nur JPG-Fotos
- kein ZIP/TXT/META im Teams-Payload
- führende Null bleibt erhalten
- bestehende Delivery-TXT-Felder Quick/Additional Work leer
- Return-Regeln bleiben erhalten

## Android / Integration
- mehrere Container
- zuerst bei allen 1–5 fotografieren
- App schliessen
- Auftrag erneut öffnen
- Foto 6 später ergänzen
- letzter Container vollständig
- Timer startet
- Upload-Queue startet
- offline → pending
- online → retry
- kein Doppelupload
- Android Share enthält nur JPGs
- Abmelden / anderer Microsoft-Benutzer
- Prozessneustart
- Gerät rebooten: bestehende Logger-Erinnerung wiederherstellen

## Visuelle Tests
- kleine Android-Breite
- mehrere Aufträge sichtbar
- dunkles Lager-/helles Lagerumfeld
- lange KT-/Order-Werte
- 1, 3, 10 Container
- Fehlerstatus
- Offlinebanner
- Timer
- Galerie

---

# 21. Akzeptanzkriterien

Der Build ist erst fertig, wenn:

1. Bestehende native Android-App wurde aktualisiert, nicht neu gebaut.
2. Delivery hat korrekt 6 Fotos pro Container.
3. Fotos 1–5 können für alle Container zuerst aufgenommen werden.
4. Foto 6 kann später ergänzt werden.
5. Erst alle Container mit 6/6 machen den Auftrag vollständig.
6. Vollständigkeit startet den 30-Minuten-Timer genau einmal.
7. Vollständigkeit startet/queued Teams-Sync automatisch.
8. Offline verhindert den Timer nicht.
9. Nach Netzrückkehr wird Sync fortgesetzt.
10. Teams erhält ausschließlich nummerierte JPGs.
11. Kein ZIP, TXT, Excel oder META wird an Teams übertragen.
12. Ziel ist `Op530-Lal`.
13. Mehrere Mitarbeiter können ihr eigenes Microsoft-Firmenkonto verwenden.
14. Keine Microsoft-Passwörter oder Secrets werden gespeichert oder committed.
15. Logout/Kontowechsel funktioniert.
16. Uploadfehler löschen keine lokalen Fotos.
17. Retry erzeugt keine Dubletten.
18. Bestehender lokaler ZIP/TXT-Export funktioniert weiter.
19. Return funktioniert unverändert.
20. Terminal-Dark-Redesign ist konsistent auf Aufträge, Kamera/Details, Galerie und Profil.
21. Keine Fake-Cloud-/Admin-Funktionen.
22. Bestehende Tests bleiben grün und neue Tests sind grün.
23. `assembleDebug`, Unit Tests und Lint laufen erfolgreich.
24. README und technische Doku werden aktualisiert.

---

# 22. README-Update

README anpassen auf:

## Container Track
Native Android App für Offline-Containerdokumentation mit:
- Delivery / Return
- Multi-Container
- OCR
- 6-stufigem Delivery-Fotoworkflow
- spätem Befüllt-Foto
- automatischem Logger-Timer
- Microsoft-/Teams-Foto-Sync
- Offline Queue
- Android Share
- lokalem Export

Sicherheitsabschnitt:
- öffentliches GitHub-Repo
- kein Passwort im Code
- kein Client Secret
- Microsoft Public-Client-Login
- Zugriff auf Firmenressourcen nur für tatsächlich berechtigte Firmenkonten

---

# 23. Codex-Arbeitsauftrag

Arbeite in kleinen, überprüfbaren Commits.

Empfohlene Reihenfolge:

1. Baseline analysieren + Tests
2. Datenmodell / Completion-State
3. Delivery 2-Phasen-Workflow
4. Timer-Trigger umstellen
5. persistente Sync-Queue
6. Microsoft Public-Client-Auth
7. echter Teams-/Datei-Upload für `Op530-Lal`
8. Android-Share-Fallback nur JPG
9. Terminal-Dark Redesign
10. Galerie / Profil
11. Migrationen
12. Tests
13. README / QA-Doku
14. finaler Regression-Lauf

Bei Unsicherheit gilt:
**Bestehende funktionierende Logik erhalten, nicht neu erfinden.**

Wenn eine Microsoft-Schnittstelle oder Berechtigung in der Firmenumgebung nicht verfügbar ist:
- Implementierung sauber hinter Interface kapseln
- echten Fehler-/Disconnected-State zeigen
- Android Share als ehrlichen Fallback verwenden
- niemals Fake-Success anzeigen.
