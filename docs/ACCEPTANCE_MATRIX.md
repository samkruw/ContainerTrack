# V1.4.0 — lokale Benutzer und Abschluss über Teams

| Anforderung | Nachweis | Status |
| --- | --- | --- |
| Ohne Server: lokale PIN-Profile, Admin/Mitarbeiter, Benutzerwechsel | LocalUserStoreTest, LocalUsersUiTest, ReferenceScreenshotsTest | bestanden: CI39905918718 |
| Mitarbeiter nur eigene Aufträge; Admin auch vorhandene Daten | LocalJobAccessTest, LocalUserPolicyTest | bestanden: CI39905918718 |
| PIN-Hash/Sperre, letzter Admin, Rollen-/Aktivierungs-/PIN-Verwaltung | LocalUserPolicyTest, LocalUserStoreTest, LocalUsersUiTest | bestanden: CI39905918718 |
| Logger-Aktion verlangt passenden angemeldeten Benutzer | ReminderReceiverTest | bestanden: CI39905918718 |
| Abschluss startet Teilen; kein neuer automatischer Transport | SelectedPhotoSharingTest, ReferenceScreenshotsTest, LocalJobAccessTest | bestanden: CI39905918718 |
| Native Oberfläche und vorhandene Migration/OCR/Export/Timer | 67 Android-Tests, 42 Unit-Tests je Variante, manuelle Sichtprüfung | bestanden: CI39905918718 |

[Debug-APK und vollständiger Prüfstatus](QA_STATUS.md), [aktuelle Ansichten](../design/verification/reference-ui-v1.4.0.jpg). Ältere Tabellen dokumentieren frühere Anforderungen und Prüfstände.

# V1.3.2 — manuell direkt in Teams teilen

| Anforderung | Nachweis | Status |
| --- | --- | --- |
| Teams ohne ContainerTrack-Firmenkonfiguration öffnen | PhotoSharingTest, ReferenceScreenshotsTest | bestanden: CI39843067438 |
| Fehlende Teams-App: Android-Chooser mit unveränderten Leserechten | PhotoSharingTest | bestanden: CI39843067438 |
| Nur ausgewählte Original-JPGs, kein bestätigter Upload-/Timer-Effekt | SelectedPhotoSharingTest, GalleryProfileTest | bestanden: CI39843067438 |
| Reale Zielauswahl im installierten Teams | Teams-Version, Konto und Rechte auf Nutzergerät | Hardware-Abnahme offen |

[Debug-APK und vollständiger Prüfstatus](QA_STATUS.md), [Bildschirmnachweis](../design/verification/reference-ui-v1.3.2.jpg).

# V1.3.1 — ausgewählte Bildvorlagen

Nachweis: [QA_STATUS.md](QA_STATUS.md), [visueller Abgleich](../design-qa.md), [acht native Ansichten](../design/verification/reference-ui-v1.3.1.jpg).

| Anforderung | Nachweis | Status |
| --- | --- | --- |
| Acht Graphit-/Lime-Ansichten inklusive echter App-Navigation | ReferenceScreenshotsTest, manueller Referenzvergleich | bestanden: CI39839563123 |
| Erster Container atomar, führende Null erhalten | InitialContainerTest | bestanden: CI39839563123 |
| Original-JPG-Auswahl/Share ohne Timer-/Queue-Änderung | PhotoSelectionTest, SelectedPhotoSharingTest | bestanden: CI39839563123 |
| Bestehende Migration, Delivery, Return, OCR, Timer, Upload-/Exportregeln | 56 Android-Tests und 39 Unit-Tests je Variante | bestanden: CI39839563123 |
| CI erhält Fehlerstatus und sammelt Captures in beiden Fällen | DeviceRunnerTest, finaler grüner Gesamtlauf | bestanden: CI39839563123 |
| Reale Firmenübertragung, zentrale Team-Galerie und Adminverwaltung | Firmenkonfiguration/echte Rechte erforderlich | offen; keine simulierten Erfolge |

Die folgenden Tabellen dokumentieren frühere Abnahmestände.

# V1.3 Ergänzung (22.10.2026)

| Anforderung | Nachweis | Status |
| --- | --- | --- |
| Delivery 01–05 je Container, 06 später | WorkflowRulesTest, CompletionTest, TwoPhaseDeliveryTest | bestanden: CI39807919494 |
| Timer/Queue einmal beim vollständigen Abschluss | CompletionTest, ExportSharingTest | bestanden: CI39807919494 |
| Migration ohne Verlust / keine historischen Massen-Uploads | MigrationTest mit originalem Schema 1 | bestanden: CI39807919494 |
| Konto-Bindung, Foto-Version, partieller Retry | GraphPhotoClientTest, PhotoSyncEngineTest, PhotoSyncTest | bestanden: CI39807919494 |
| Nur JPEG an Teams, separater ZIP-Export | PhotoSharingTest, GraphPhotoClientTest, ShareChooserTest | bestanden: CI39807919494 |
| Drei Tabs, Plus, Filter, Karten, echte Galerie/Profil | NavigationTest, OrderFiltersTest, GalleryFiltersTest, Screenshots | bestanden: CI39807919494 |
| Echte Firmenanmeldung und Kanalübertragung | Microsoft-End-to-End-Abnahme | offen: öffentliche Konfiguration fehlt |
| Debug-APK aus GitHub | aktueller Actions-Lauf laut QA_STATUS.md | erfolgreich: CI39807919494 |

Historische Matrix folgt; Aussagen zum alten fehlenden Internetrecht und Export-Timer gelten nur für den damaligen Stand.

# Container Track Android — Acceptance Matrix

## Delivery
- [ ] Order Number Pflicht
- [ ] ZRH Number Pflicht
- [ ] Order / Container-Typ Pflicht
- [ ] Quantity 1..50
- [ ] Quick nicht erforderlich und im Delivery-Formular ausgeblendet
- [ ] additional Work nicht erforderlich und im Delivery-Formular ausgeblendet
- [ ] OCR exakt 4-stellig
- [ ] führende Null bleibt
- [ ] erster gültiger Treffer wird übernommen
- [ ] Duplicate blockiert
- [ ] Manual Entry
- [ ] Torch wenn Hardware unterstützt
- [ ] Foto 1 leerer Innenraum
- [ ] Foto 2 Front
- [ ] Foto 3 Rückseite/Türen
- [ ] Foto 4 links
- [ ] Foto 5 rechts
- [ ] Foto 6 befüllter Innenraum
- [ ] kein Skip
- [ ] Dateinamen 01..06
- [ ] mehrere Container
- [ ] ZIP Root = Order Number
- [ ] Container-Unterordner
- [ ] TXT Quick und additional Work leer, auch bei historischen Werten
- [ ] TXT OUT
- [ ] TXT KT = ZRH Number
- [ ] TXT Order = Container-Typ
- [ ] Quantity korrekt
- [ ] alle Container im TXT
- [ ] Export nur bei Vollständigkeit
- [ ] 30-Minuten-Reminder nach Export
- [ ] Re-Export erzeugt keinen zweiten/reset Timer
- [ ] Countdown sichtbar
- [ ] Standby Notification via AlarmManager
- [ ] Logger gestartet muss bestätigt werden

## Return
- [ ] Formular zuerst gesperrt
- [ ] Logger gestoppt zuerst bestätigen
- [ ] loggerStoppedAt exakt bei Bestätigung
- [ ] Return Number Pflicht
- [ ] Order / Container-Typ Pflicht
- [ ] Quantity 1..50
- [ ] Quick Pflicht
- [ ] additional Work Pflicht
- [ ] `Nothing` und `Empty` in beiden Feldern akzeptiert
- [ ] OCR 4-stellig
- [ ] Duplicate Block
- [ ] exakt 1 Inhaltsfoto pro Container
- [ ] Dateiname 01
- [ ] ZIP Root = Return Number
- [ ] Container-Unterordner
- [ ] TXT IN
- [ ] TXT KT = Return Number
- [ ] TXT Order = Container-Typ
- [ ] alle Container im TXT

## Offline / Privacy
- [ ] Prozess-Tod verliert keinen Job
- [ ] Neustart stellt Job wieder her
- [ ] Bilder intern
- [ ] OCR im Flugmodus
- [ ] Export im Flugmodus
- [ ] Logger im Flugmodus
- [ ] kein INTERNET im merged manifest
- [ ] keine Analytics-/Cloud-Datenhaltungs-SDKs; technische ML-Kit-Hilfsartefakte dokumentiert

## UI / Anti-Slop
- [ ] Referenzbild verwendet
- [ ] Dark Navy / Electric Blue
- [ ] keine fremden Logos
- [ ] keine fremden Firmennamen
- [ ] keine Placeholder Cards
- [ ] keine nutzlosen Buttons
- [ ] keine Fake-Daten
- [ ] keine Marketingtexte
- [ ] Touch Targets >=48dp
- [ ] Fehler am Feld
- [ ] Permission-Denial sauber
- [ ] Start, Aufträge, Scannen, Export, Mehr funktionieren
- [ ] Screenshot-Regressionstests für Kern-Screens

## Update 1.2 / Teilen und Kompatibilität
- [ ] ZIP teilen ist Primäraktion, ZIP speichern bleibt unabhängig verfügbar
- [ ] ACTION_SEND, application/zip, EXTRA_STREAM und ClipData enthalten lesbare FileProvider-URI
- [ ] Nur temporärer Lesezugriff, keine internen Foto-/Datenbankdateien freigegeben
- [ ] Android Chooser öffnet; Abbruch gilt nicht als Versandbestätigung
- [ ] Re-Share/Re-Export verändert weder Reminder-ID noch Frist
- [ ] bestehende Room-Daten, Fotos und Reminder nach Update mit identischem Signaturschlüssel erhalten
- [ ] Glasflächen auf Formularen, Export, Statuskarten und Navigation konsistent

Checkboxen sind die vollständige Abnahmeliste, keine automatischen Testresultate. Aktuelle Nachweise und verbleibende Geräteprüfungen: [QA_STATUS.md](QA_STATUS.md).
