# Teilplan A — Delivery, Completion und Migration

Master: [V1.3-Plan](2026-10-22-containertrack-v1-3.md). Dessen Constraints und Basispfade gelten. Dieser Abschnitt ist unabhängig von Firmenzugängen testbar.

## Task 1: Zwei Delivery-Phasen als reine Fachregel

**Files:** `main/Rules.kt`, `main/Database.kt`, neu `unit/WorkflowRulesTest.kt`.

**Interfaces:** `enum class WorkflowPhase { LOCAL_INCOMPLETE, READY_FOR_FILL, COMPLETE }`; `fun workflowPhase(kind: JobKind, quantity: Int, stepsByContainer: List<Set<Int>>): WorkflowPhase`; `fun canAddContainer(kind: JobKind, existingSteps: List<Set<Int>>): Boolean`. `JobWithContainers.phase` delegiert an diese Regeln; `complete` bleibt eine Prüfung der vollständigen Soll-Schritte.

- [ ] Tests zuerst: `deliveryFiveOfSixIsReadyForFill` (1 Container, 1..5 => READY_FOR_FILL und nicht COMPLETE), `threeContainersNeedEighteenPhotos` (6/6,6/6,5/6 nicht vollständig; 18/18 vollständig), `canContinueAfterFivePhotos` (Delivery 1..5 erlaubt weiteren Container, 1..4 nicht), `returnKeepsOnePhotoRule`, `invalidStepsNeverComplete` (zusätzlicher/ungültiger Schritt erfüllt Soll nicht).
- [ ] `./gradlew testDebugUnitTest --tests '*WorkflowRulesTest'` ausführen; fehlende Regeln müssen den roten Lauf erklären.
- [ ] Reine Regeln implementieren; READY_FOR_FILL nur wenn alle Quantity-Container existieren und alle 1..5 besitzen. Reihenfolge, führende Null und bisherige TXT-/Return-Regeln erhalten.
- [ ] Dieselben Tests und gesamte Unit-Suite grün ausführen; Commit `feat: model two-phase delivery workflow`.

## Task 2: Datenmodell und echte Migration 1 → 2

**Files:** `main/Database.kt`, neu `main/DatabaseMigrations.kt`, `main/ContainerTrackApp.kt`, `app/schemas/`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, neu `androidTest/MigrationTest.kt`.

**Interfaces:** `JobEntity.completionTriggeredAt: Long?`; neue `UploadJobEntity(jobId: String, ownerAccountId: String?, state: String, lastError: String?, lastAttemptAt: Long?)` mit Job-FK/CASCADE; neue `UploadPhotoEntity(photoId: String, jobId: String, remotePath: String, uploadedAt: Long?, remoteItemId: String?, uploadedByAccountId: String?)` mit Foto-/Job-FK/CASCADE. `UploadPhotoEntity.photoId` ist PK und identifiziert eine unveränderliche Bildversion; keine Tokenfelder. `MIGRATION_1_2: Migration` wird bei Room registriert. Room-Testing-Abhängigkeit in Version 2.8.3 ergänzen.

- [ ] Mit `MigrationTestHelper` anhand des versionierten echten v1-Schemas Job, Container, Foto und offenen/bestätigten Reminder anlegen; Migration auf v2 prüfen. Assertions: alle IDs/Pfade/Draftwerte sowie dueAt/confirmedAt unverändert, neue Tabellen vorhanden, keine automatische Uploadarbeit für archivierte Altjobs.
- [ ] Migrationstest im Emulator/CI rot ausführen; fehlende Migration darf nicht durch leere Testdaten verdeckt sein.
- [ ] Migration implementieren. Historisch vollständige Jobs bekommen verarbeitete Completion-Markierung aus bestehenden Zeitpunkten; unvollständige Jobs bleiben unmarkiert. Historisch vollständige Delivery-Jobs ohne Reminder werden beim Abgleich einmalig mit neuer Frist ab Abgleichzeit berücksichtigt; vorhandene Reminder bleiben unverändert. Keine historische Massen-Upload-Queue.
- [ ] Neues exportiertes Schema 2 prüfen; Migrationstest und bestehende Persistenztests grün. Commit `feat: migrate completion and photo upload state without data loss`.

## Task 3: Atomarer Abschluss und Timer unabhängig vom Export

**Files:** `main/Repository.kt`, neu `main/CompletionEffects.kt`, `main/ContainerTrackApp.kt`, `main/Reminders.kt`, `androidTest/PersistenceTest.kt`, `androidTest/ExportSharingTest.kt`, neu `androidTest/CompletionTest.kt`.

**Interfaces:** `data class CompletionEffects(val reminder: LoggerReminder?, val uploadJobId: String?)`; `fun interface UploadScheduler { fun enqueue(jobId: String) }`; Repository erhält diesen Scheduler als Abhängigkeit. `TrackRepository.reconcilePendingEffects(): Unit` plant persistierte offene Reminder/Queue erneut. Bestehende Repository-Mutationen bleiben suspend-Funktionen; Netzwerkarbeit ist nie Teil einer DB-Transaktion.

- [ ] Tests zuerst: Drei Delivery-Container je mit Fotos 1..5 speichern, DB wieder öffnen, drei späte Fotos 6 ergänzen; vor letztem Foto keine neue Queue/Frist, danach genau ein Reminder und drei × sechs Queue-Einträge. `completionSurvivesReopenWithoutTimerReset`, `shareAndExportNeverResetCompletionTimer`, `commitBeforeSchedulingRecoversAfterRestart` und `returnCompletionQueuesPhotosWithoutDeliveryReminder` prüfen. Schedulerfehler im Test injizieren, nicht Firmenzugriffe.
- [ ] Relevante Instrumentationstests rot ausführen; bisherigen Export-Timer als erwartete alte Ursache dokumentieren.
- [ ] `addContainer` benutzt Task-1-Regel. In `savePhoto` dieselbe Transaktion für Foto, Containerabschluss, Auftrag, ersten Completion-Zeitpunkt, Reminder und Queue verwenden. Eindeutiger Reminder-Jobindex und bedingter erster Zeitpunkt verhindern doppelte Effekte. Dateiersatz entfernt alte Versionsbestätigung und aktualisiert Queue; Löschung macht den Auftrag unvollständig, setzt ersten Timer aber nicht zurück.
- [ ] `finishExport` setzt nur ersten Exportzeitpunkt und behält lokale Exportbereinigung; keine Timer-Neuanlage durch Export. Bestehende Tests an fachlich neuen Trigger anpassen, ihre ZIP-/TXT-/Share-Assertions nicht entfernen. Kanalbeschreibung auf „nach vollständiger Lieferung“ ändern.
- [ ] App-Start gleicht Reminder/Queue ab; Export/Retry/Boot-/Bestätigungsregeln testen. Gesamte Unit-/Android-Regression grün; Commit `feat: trigger logger and durable photo queue on first completion`.

## Task 4: Spätes Foto 6 im vorhandenen Aufnahmeflow

**Files:** `main/CaptureScreens.kt`, `main/JobScreens.kt`, `main/TrackViewModel.kt`, `androidTest/NavigationTest.kt`, neu `androidTest/TwoPhaseDeliveryTest.kt`.

**Interfaces:** Bestehende `ScanScreen`, `PhotoScreen`, `JobScreen` und ViewModel-Methoden bleiben die Aufnahme-API; zusätzliche Rückkehr-/Weiterer-Container-Aktion nutzt dieselben Callbacks.

- [ ] UI-Test zuerst: Auftrag Quantity 3, Container 0708 mit fünf Bildern, weiterer Container erreichbar; 0710/0712 gleich behandeln; App/Activity neu öffnen, sichtbare 5/6-Zustände, Foto 6 gezielt ergänzen. Return muss weiterhin 1/1 verlangen.
- [ ] Rot verifizieren, dann Aufnahme-/Details-UI so ergänzen, dass Foto 6 später möglich und fehlende Befüllt-Bilder erreichbar sind; CameraX und OCR unverändert nutzen.
- [ ] Mengen 1/3/10 und lange Referenz auf kleiner Breite prüfen; Basisnavigation bleibt bis Teilplan C funktionsfähig. Tests grün; Commit `feat: allow batch empty documentation before filled photos`.
