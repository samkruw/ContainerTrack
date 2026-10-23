# Teilplan B — Microsoft, Queue und Foto-Share

Master: [V1.3-Plan](2026-10-22-containertrack-v1-3.md), Teilplan A vollständig. Alle Produktionsquellen liegen im dort definierten Paket. Echte Firmenabnahme bleibt von Konfiguration/Consent abhängig; lokale Tests verwenden nur Testadapter.

## Task 1: Microsoft Public Client und sichere Kontobindung

**Files:** neu `main/MicrosoftConfig.kt`, `main/MicrosoftAccountProvider.kt`, `main/MsalAccountProvider.kt`, `unit/MicrosoftConfigTest.kt`, `androidTest/AccountSessionTest.kt`; ändern `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, neue öffentliche Konfigurationsvorlage unter `app/src/main/res/raw/`, `tools/check_privacy.py`, `tools/test_quality_tools.py`, `.github/workflows/android.yml`.

**Interfaces:** `MicrosoftConfig(tenantId: String, clientId: String, redirectUri: String, teamId: String, channelId: String)` mit `configured: Boolean`; `MicrosoftIdentity(accountId: String, displayName: String, username: String)`; `MicrosoftAccountProvider` liefert `StateFlow<MicrosoftIdentity?>`, `suspend fun signIn(activity: Activity)`, `suspend fun signOut()`, `suspend fun accessToken(accountId: String): String`. Tokens nur kurzlebig im Adapter, nie Room/SavedState/Logs. `classifyConfiguration(...)` unterscheidet nicht eingerichtet und ungültig.

- [ ] Offizielle MSAL-Android- und Graph-Dokumentation zu Public Client, Single Account, Redirect/Signatur, Kanal-Dateiablage und delegierten Scopes lesen; geprüfte kompatible MSAL-/WorkManager-Versionen exakt pinnen. Nur öffentliche IDs abfragen/konfigurieren. Keine Secrets als APK-Abhängigkeit.
- [ ] Tests zuerst: fehlende IDs => CONFIG_REQUIRED statt Fake-Konto; falscher Tenant/Redirect => erklärter Fehler; Logout löscht aktive Identität; token request mit fremder accountId => AccountMismatch. Im Testadapter keine Produktions-Dummyidentität.
- [ ] Rot verifizieren; MSAL-Adapter und gekapselte öffentliche Konfiguration implementieren. Nicht konfigurierte Builds dürfen kompilieren und lokal arbeiten; Anmeldung erklärt fehlende Einrichtung.
- [ ] INTERNET/ACCESS_NETWORK_STATE gezielt hinzufügen. Privacy-Gate erhält explizite Berechtigungs- und Netzwerkadapter-Whitelist; WebView, Klartext, Analytics und Secrets bleiben gesperrt. Tests zuerst belegen: erlaubter Syncadapter besteht, beliebiger Netzwerkcode im Aufnahmepaket und unerwartete Rechte scheitern. CI-APK-Audit an neue explizite Rechte anpassen, nicht entfernen. MSALs etwaige transitive Rechte/Telemetrie prüfen und dokumentieren, statt sie stillschweigend zuzulassen.
- [ ] Tests und Build grün; Commit `feat: add tenant-scoped Microsoft public client authentication`.

## Task 2: Echtes Graph-Ziel und JPG-only Dateivertrag

**Files:** neu `main/GraphPhotoClient.kt`, `main/PhotoUploadManifest.kt`, `main/SyncFailure.kt`, `unit/PhotoUploadManifestTest.kt`, `unit/GraphPhotoClientTest.kt`.

**Interfaces:** `UploadCandidate(photoId: String, jobId: String, file: File, remotePath: String, fileName: String)`; `fun uploadManifest(batch: JobWithContainers): List<UploadCandidate>`; `RemotePhoto(id: String, eTag: String?)`; `ResolvedTeamsFolder(driveId: String, itemId: String)`; `GraphPhotoClient` ist ein Interface mit `suspend fun resolveFolder(config: MicrosoftConfig, accountId: String): ResolvedTeamsFolder` und `suspend fun upload(candidate: UploadCandidate, folder: ResolvedTeamsFolder, accountId: String): RemotePhoto`. `HttpGraphPhotoClient` ist die Produktionsimplementierung mit injizierbarem Transport. `GraphTransport.execute(request: GraphRequest): GraphResponse` ist suspend; Request umfasst Methode, relativen Graph-Pfad, kurzlebiges Zugriffstoken und optionale Bilddatei, Response HTTP-Status, JSON und Retry-After-Sekunden. Nur der Adapter baut HTTPS-URLs für erwartete Microsoft-Hosts; keine Request-/Tokenlogs.

- [ ] Tests zuerst: Delivery sechs nummerierte JPGs je Container (0708_01 bis 0708_06), Return nur vorhandene Sollbilder; unvollständiger Job wird abgelehnt; ZIP/TXT/META/DB können nie Kandidaten werden. Unicode/Leerzeichen/Sonderzeichen in sicheren Referenzen werden korrekt URL-kodiert. Wiederholung desselben Bilds nutzt denselben Remote-Pfad; verschiedene lokale Job-IDs kollidieren nicht.
- [ ] HTTP-Vertragstests rot: echte JSON-/Statuscodefälle für Dateien, fehlende ID, falsches Ziel, 401/403, 429/Retry-After und 5xx. Erfolg nur bei bestätigter gültiger Remote-Dateiidentität.
- [ ] Graph-Auflösung per stabilen Team-/Kanal-IDs mit Kanalprüfung Op530-Lal implementieren. Direkter unterstützter Graph-PUT an deterministische Dateipfade; keine Kanalnachrichten und keine Archivanhänge. Offizielle Grössenlimits prüfen; zu grosse Bilder als echte Fehler behandeln, nicht kürzen/verlieren. Netzwerk- und Token-Logging ausschliessen.
- [ ] Vertragstests grün; Commit `feat: upload only numbered photos to the configured Teams folder`.

## Task 3: WorkManager und dauerhafte Datei-Bestätigung

**Files:** neu `main/PhotoSyncWorker.kt`, `main/WorkManagerUploadScheduler.kt`, `main/PhotoSyncRepository.kt`, `androidTest/PhotoSyncTest.kt`; ändern `main/Database.kt`, `main/ContainerTrackApp.kt`, `main/Repository.kt`.

**Interfaces:** `PhotoSyncRepository.sync(jobId: String, accountId: String): SyncOutcome`; `sealed interface SyncOutcome` mit `Done`, `Blocked(reason: String)`, `Retry`, `Failed(reason: String)`; `WorkManagerUploadScheduler` implementiert Teilplan A `UploadScheduler`. `bindUnassignedJob(jobId: String, accountId: String): Unit` ist eine ausdrückliche Benutzeraktion. DAO bestätigt nur die aktuelle Bildversion und erwartete Kontobindung; bei Erfolg setzt sie uploadedByAccountId aus der tatsächlich verwendeten Kontenidentität.

- [ ] Tests zuerst: Offline-Constraint lässt Queue pending; nach Netzfreigabe nur fehlende Dateien senden; bestärkter Remote-Erfolg vor lokalem Commit führt beim Wiederholen zum gleichen Pfad, nicht zu einer zweiten Datei. Prozess-/DB-Neustart erhält Queue. Fehler löscht keine lokalen Fotos. 401 führt zu Anmeldung, 403 zu erklärtem Zugriffserror, 429/5xx zu Retry.
- [ ] Review-Fokus prüfen: Bild wird während suspendiertem Upload ersetzt => alter Erfolg bestätigt neue photoId nie; serieller Worker nimmt die aktuelle Version erneut auf, bevor der Job SYNCED wird. Neuer Benutzer darf wartende Jobs des alten Kontos nicht senden. Ohne Konto angelegte Jobs benötigen ausdrückliche Zuordnung. Wieder unvollständige Jobs pausieren.
- [ ] Rot verifizieren; WorkManager-Netzwerkbedingung, exponentiellen Backoff und serialisierte Unique-Work-Kette pro Job (`photo-sync:<jobId>`, APPEND_OR_REPLACE) verwenden. Mehrfaches Einreihen darf keine parallelen Uploads und kein verlorenes Abschlussereignis erzeugen. Kontogebundene Work-Tags erlauben Logout-Stopp; App-Start nimmt berechtigte persistierte Restarbeit wieder auf.
- [ ] Erfolgsstatus erst wenn alle aktuellen Versionen bestätigt sind; sichere Fehlerkategorien ohne Token/URL-Inhalte persistieren. Reale Konten für UI/Logs nicht als Testdaten verwenden. Tests plus Completion-/Return-Regression grün; Commit `feat: persist retryable account-bound photo synchronization`.

## Task 4: Ehrlicher JPG-Share-Fallback, separater ZIP-Export

**Files:** `main/ExportSharing.kt`, `main/Repository.kt`, `main/TrackViewModel.kt`, `app/src/main/res/xml/export_paths.xml`, `app/src/main/AndroidManifest.xml`; neu `androidTest/PhotoSharingTest.kt`.

**Interfaces:** `TrackRepository.preparePhotoShare(jobId: String, teamsPackage: String? = null): Intent`; `photoShareIntent(context: Context, photos: List<File>, teamsPackage: String?): Intent`. ZIP-APIs `prepareShare` und `export` bleiben getrennt.

- [ ] Tests zuerst: 1 JPG => ACTION_SEND; mehrere JPGs => ACTION_SEND_MULTIPLE; image/jpeg, EXTRA_STREAM und ClipData deckungsgleich, jeder nummerierte Anhang lesbar und nur Leserecht. Kein ZIP/TXT/META enthalten. Provider verweigert Dateien ausserhalb des engen Photo-Share-Caches. Sonderzeichen in Referenz beeinträchtigen Pfadprüfung nicht.
- [ ] Rot verifizieren; unveränderliche nummerierte Cachekopien unter separatem `shared-photos/` anlegen. Teams-Paket nur bei echtem installierten Empfänger wählen; anderenfalls echten Android-Chooser nutzen. Chooser-Aufruf/Abbruch erzeugt nie SYNCED und ändert keine Reminderfrist.
- [ ] ZIP teilen/speichern und bestehende FileProvider-Tests grün erhalten; Commit `feat: share numbered JPEG photos separately from local exports`.
