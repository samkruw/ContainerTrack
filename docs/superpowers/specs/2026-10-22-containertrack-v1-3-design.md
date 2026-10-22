# ContainerTrack 1.3 — technische Spezifikation

## Verbindliche Grundlage

Der Benutzer hat am 22.10.2026 bestätigt: Das [V1.3-Handoff](../../requirements/ContainerTrack-Codex-Update-V1.3.md) ist massgeblich; die [Terminal-Dark-Vorgabe](../../requirements/2026-10-22-container-track-ui-update.md) dient ausschliesslich als Designvorlage. Deren Aussagen zu PWA, fünf Fotos und unverändertem Timer-Trigger gelten nicht. Bestehende native Kotlin-/Compose-App auf `codex/native-android-app`, Ausgangscommit `7a3e8f48db54fc1a169c7afbb08c7435d8f372e3`, App-ID `ch.zerogen.containertrack`. Ziel: Debug-APK 1.3.0 / versionCode 4. Keine Release-Signierung als Voraussetzung.

Erfolg bedeutet: zuerst für mehrere Delivery-Container je Fotos 1–5 aufnehmen, später Foto 6; beim erstmaligen vollständigen Auftrag atomar lokalen Logger und Foto-Sync vormerken. Die App bleibt ohne Anmeldung und Internet für lokale Erfassung, OCR, Timer und Export nutzbar. Microsoft-Funktionen zeigen nur echte Zustände.

## Architekturentscheidung

Die bestehende App wird um Completion-Zustand, persistente Foto-Queue, WorkManager und einen gekapselten Microsoft-Adapter erweitert. Room, Repository, ViewModel, CameraX, OCR, lokale Bilder und Alarm-/Boot-Receiver bleiben die Basis. Das bewahrt Daten und ermöglicht automatischen Upload ohne eigenes Backend.

Reiner Android-Share wäre kleiner, erfüllt aber automatischen Upload/Retry nicht. Ein eigenes Backend wäre zusätzliche Infrastruktur und ist nicht vorgesehen. Diese Alternativen werden nicht implementiert.

## 1. Delivery und einmaliger Abschluss

Delivery behält sechs Slots und ihre Namen/Dateinamen: leerer Innenraum, Front, Rückseite/Türen, links, rechts, befüllter Innenraum. Return bleibt bei seinem bestehenden Inhaltsfoto und Logger-Stopp-Gate.

Ein weiterer Delivery-Container darf erfasst werden, sobald die bisher erfassten Container mindestens die Schritte 1–5 besitzen. Bei Return bleibt die bisherige Abschlussregel. Nummern bleiben vierstellige Strings; führende Nullen, Duplicate-Schutz und Schrittfolge bleiben erhalten. Nach fünf Delivery-Fotos kann der Benutzer weitere Container aufnehmen oder zum Auftrag zurückkehren; Foto 6 wird später über dessen Slot ergänzt.

`READY_FOR_FILL` bedeutet: alle vorgesehenen Container existieren, alle besitzen Fotos 1–5 und mindestens einem fehlt Foto 6. `COMPLETE` erfordert weiterhin 6 × Quantity Fotos bzw. bei Return 1 × Quantity. Container und Auftrag zeigen sowohl fehlende Bilder als auch vollständige Counts, nicht nur ein unspezifisches Statuslabel.

Beim Fotoabschluss erzeugt dieselbe Room-Transaktion erstmals `completionTriggeredAt`, genau einen Delivery-Reminder mit Frist +30 Minuten und die Foto-Queue. Ein eindeutiger Job-Schlüssel und bedingtes Setzen des Zeitpunkts verhindern Doppelereignisse. Ein Prozessabbruch nach Commit verliert weder Frist noch Queue. AlarmManager/WorkManager werden danach angestossen; App-Start und Boot gleichen persistierte Arbeit erneut ab.

Export/Share, Wiederöffnen, Bildersatz und Retry setzen diesen ersten Zeitpunkt nicht zurück. `exportedAt` bleibt ein davon unabhängiger erster Exportzeitpunkt. Return erzeugt keine Delivery-Erinnerung. Lokale ZIP-/TXT-/META-Struktur und Delivery-Leerwerte für Quick/additional Work bleiben erhalten.

## 2. Room-Migration und vorhandene Daten

Schema 1 wird durch eine explizite, getestete Migration erweitert: dauerhafter erster Abschlusszeitpunkt und separate Queue-/Dateibestätigungsdaten. Keine destruktive Migration, keine Änderung vorhandener Job-/Container-/Foto-IDs oder Fotopfade. Alarm-ID, dueAt und confirmedAt bleiben für bestehende Reminder unverändert.

Bereits vor dem Update vollständige/archivierte Jobs lösen keinen automatischen Massen-Upload historischer Bilder aus. Ihre Abschlussmarkierung wird als bereits verarbeitet übernommen; vorhandene Erinnerungen bleiben erhalten. Für alte vollständige Delivery-Jobs ohne frühere Erinnerung wird eine fehlende Erinnerung bei der ersten lokalen Abgleichprüfung genau einmal ab diesem Zeitpunkt erzeugt. Ein gezielter manueller Teams-Auftrag kann alte vollständige Jobs separat einreihen. Unvollständige alte Jobs erhalten die neue Automatik beim tatsächlichen Abschluss.

Pro Datei persistiert die Queue: Job-/Foto-Zuordnung, Bildversion, deterministischer Remote-Pfad, Kontozuordnung, Status, bestätigte Remote-ID/Bestätigungszeit und letzte sichere Fehlermeldung. Tokens gehören nicht in Room.

## 3. Upload und Idempotenz

WorkManager nutzt Netzwerkbedingungen, persistente Arbeit pro Job und exponentiellen Backoff. Wiederherstellung funktioniert nach App-Neustart; Android-Force-Stop bleibt eine dokumentierte Plattformgrenze. Lokale Fotos werden durch Netzwerkfehler niemals gelöscht.

Upload erfolgt ausschliesslich für aktuelle nummerierte JPG-Fotos vollständiger Jobs. ZIP, TXT, CSV, META, Loggerdaten oder Datenbankdateien sind kein Teams-Payload. Die Dateiliste entsteht aus den Fotoentitäten, nicht aus einem Verzeichnis-Glob oder dem Exportarchiv.

Eindeutige Remote-Pfade innerhalb der Kanal-Dateiablage und lokale Bestätigungen je Bild vermeiden zusätzliche Dateien bei Retry. Nur bestätigte HTTP-Antworten mit gültiger Dateiidentität markieren ein Bild als übertragen. Fehler nach Remote-Erfolg, aber vor lokalem Commit führen zu einer erneuten Ablage am selben Pfad, nicht zu einem zweiten Dateinamen. Dateiersatz verwirft die Bestätigung dieser Bildversion; serialisierte Arbeit und Versionsprüfungen verhindern eine Erfolgsmeldung für veraltete Inhalte. Ist der Auftrag wieder unvollständig, pausiert dessen Upload.

Offline, fehlende Anmeldung, fehlende Konfiguration und Zugriffsverweigerung werden unterschieden. Netzwerk-/temporäre Serverfehler werden wiederholt; dauerhafte Berechtigungs-/Zielkonfigurationsfehler verlangen eine sichtbare Benutzeraktion statt einer Endlosschleife. Fehlertexte enthalten keine Tokens oder geheimen Upload-URLs.

## 4. Microsoft Public Client und Ziel

MSAL verwaltet Benutzer und Tokens über seine vorgesehene Android-Ablage. Kein Passwortfeld, kein Client Secret, keine selbst implementierte Tokenablage. Unterstützt werden Anmeldung, Abmeldung und Kontowechsel mit dem jeweiligen Firmenkonto. Das Profil zeigt reale MSAL-Kontodaten und den tatsächlichen Verbindungszustand; nicht belegbare Rollen werden nicht erfunden.

Benötigte öffentliche Konfiguration: Tenant-ID, Android-Public-Client-ID, passende registrierte Redirect-URI sowie stabile Team-/Kanal-IDs für `Op530-Lal`. Der Kanalname allein ist kein eindeutiger Zugang. Eine Tenant-/Kanalprüfung und Microsofts tatsächliche Berechtigungen begrenzen das Ziel. Graph löst die echte Kanal-Dateiablage auf; der Uploadadapter verarbeitet nur Fotos und bestätigt den tatsächlichen Erfolg.

Die genauen delegierten Graph-Scopes und MSAL-/Redirect-Anforderungen werden vor Implementierung gegen die aktuelle offizielle Microsoft-Dokumentation geprüft. Organisations-Consent und Kanalzugriff müssen in der Firmenumgebung freigegeben sein. Eine Debug-APK benötigt eine zu ihrer Signatur passende registrierte Redirect-Konfiguration; wechselnde CI-Debug-Schlüssel dürfen nicht als funktionierende Anmeldung ausgegeben werden.

Queue-Arbeit ist an die tatsächlich gewählte Kontenidentität gebunden. Abmeldung/Kontowechsel stoppt deren Hintergrundzugriff und überträgt fremde Restarbeit nicht stillschweigend unter einem neuen Konto. Ohne Konto lokal erstellte Aufträge benötigen vor der ersten Kontozuordnung eine ausdrückliche Übertragungsaktion; lokale Erfassung und Timer bleiben davon unabhängig.

Fehlt die öffentliche Microsoft-Konfiguration, ist die App weiterhin baubar und nutzbar, zeigt aber „Microsoft nicht eingerichtet“. Ohne echte Firmenfreigabe werden weder erfolgreiche Anmeldung noch Teams-Übertragung behauptet. Die ausstehende Konfiguration ist ein Abnahmeblocker für diesen Teil, kein Grund für Fake-Daten.

## 5. Foto-Share und lokaler Export

Der neue Foto-Share benutzt FileProvider-Cachekopien mit den bestehenden nummerierten JPG-Dateinamen, `image/jpeg`, ACTION_SEND/ACTION_SEND_MULTIPLE, ClipData und ausschliesslich temporärem Lesezugriff. Er gibt keine internen Fotopfade, DB-Dateien oder beliebige Dateien frei. Die Provider-Whitelist wird nur um den separaten Foto-Share-Cache erweitert.

Das Teilen-Bottom-Sheet bietet Teams nur dann als echte Aktion, wenn eine direkte Verbindung oder ein realer installierter Teams-Share-Empfänger verfügbar ist; sonst ist Android-Teilen der erklärte Fallback. Ein geschlossener Chooser oder aufgerufenes Share-Intent bestätigt keinen Versand. ZIP teilen/ZIP speichern bleiben getrennte Exportaktionen und werden nicht dem automatischen Teams-Upload untergeschoben.

## 6. Terminal Dark und Navigation

Hauptziele: Aufträge, Galerie, Profil; hervorgehobene Plus-Aktion für neue Aufträge. Graphit/Schwarz, helle Texte, zurückhaltendes Lime, textlich erklärte Statusfarben, ausreichend grosse Touchziele und kompakte Listen. Keine neuen Pflichtfelder für das Redesign, keine Blend-/Glow-Effekte auf Kamera oder Fotos.

Aufträge: Suche nach KT/Auftrag/Return, Filter Aktiv (Standard), Befüllt-Foto fehlt, Upload-Fehler, Alle; echte Container-/Foto-Counts, Workflowstatus, Syncstatus und derselbe gespeicherte Logger-Countdown wie in Details. Ein Job-Bottom-Sheet führt zu Aufnahme, Details und Foto-Share.

Details: aufklappbare Containerkarten, 1/6 bis 6/6 bei Delivery und 1/1 bei Return, echte Vorschauen und Aufnahme-/Ansichts-/Ersetzen-Aktionen. Foto 6 wird als „Befüllt“ klar hervorgehoben. Navigation, Suche und Filter überstehen sinnvoll den Rückweg aus Details.

Galerie: tatsächliche lokale Bilder mit KT-/Containerfiltern, Raster und Vollbild. Fremde, nur auf anderen Smartphones gespeicherte Fotos werden nicht angezeigt. Ein Mitarbeiterfilter ist nur bei tatsächlich vorhandenen Uploaderdaten sinnvoll. Keine erfundene zentrale/Admin-Galerie. Globale Firmenbilder und Admin-Menüs bleiben ausgeschlossen, solange kein echtes durchgesetztes Berechtigungsmodell sie ermöglicht.

Profil: reale Microsoft-Identität/Verbindung, Logout/Kontowechsel, vorhandene lokale Berechtigungs-/Datenschutzeinstellungen. Ohne MSAL-Anmeldung kein vorgetäuschtes Konto oder Adminstatus.

## 7. Datenschutz- und CI-Vertrag

INTERNET und ACCESS_NETWORK_STATE werden für die ausdrücklich beauftragte Microsoft-Anbindung benötigt. Die bisherigen Offline-only-Privacy-Gates müssen daher gezielt angepasst werden, nicht einfach entfernt: keine WebView, keine Analytics, kein Client Secret, kein Klartextverkehr, Backup weiterhin deaktiviert, Netzwerkcode ausschliesslich in Auth-/Sync-Adaptern. OAuth- und Firmenzugriffe sind nach Freischaltung nicht mehr „Local-only“; README und Profil erklären dies.

CI behält Tests, Lint, Debug-APK, Emulatorprüfungen, Permission-Audit und Screenshots. Der neue Permission-Audit prüft die beabsichtigte begrenzte Berechtigungsliste statt der nun falschen Aussage „kein INTERNET“. Tests dürfen keine Firmenzugänge benötigen. Test-Transporte sind nur für automatisierte Tests; Produktzustände entstehen aus echten Daten.

## 8. Abnahme und Ausführungsgrenzen

Unit: 5/6 versus 6/6, 18/18 bei drei Containern, spätes Foto 6, genau ein Completion-Ereignis/Timer, Retry/Wiederöffnung ohne Reset, vollständige Jobs als einzige Queue-Quelle, nur JPG-Payload und beibehaltene Return-/TXT-Regeln.

Android: Migration mit vorhandenen Daten/Fotos/Remindern, mehrere Container mit zunächst 1–5, Prozessneustart und später Foto 6, persistente Queue, wiederholte oder unterbrochene Uploadversuche, Kontobindung, Foto-Share-Berechtigungen und unveränderter ZIP-Export. Bestehende Navigationstests werden an die drei echten Hauptziele angepasst. Kamera/OCR bleiben mit gebündeltem Modell offline prüfbar.

Visual QA: Aufträge/Details/Kamera/Galerie/Profil, kleine Breite und grosse Schrift, lange Kennungen, 1/3/10 Container, Offline-/Fehler-/Timerzustände; keine Golden-Baselines ohne tatsächliche Sichtprüfung etablieren.

Ein Debug-Build mit lokalen Tests kann ohne Firmenkonto abgeschlossen werden. Der echte OAuth-/Teams-End-to-End-Nachweis setzt öffentliche Microsoft-Konfiguration, Consent, Zugriff auf `Op530-Lal` und ein eigenes berechtigtes Firmenkonto voraus. Diese Abnahme wird bei fehlenden Voraussetzungen ausdrücklich als ungetestet/offen dokumentiert. Debug-APK bleibt der angeforderte Auslieferungsweg.
