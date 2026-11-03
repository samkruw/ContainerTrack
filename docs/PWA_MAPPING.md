# Verhaltensabgleich mit der alten PWA

Referenz: `samkruw/ContainerTrack`, main-Commit `ce930e822cfd5d43cf9e6b1005e80bf0716ee1c4`. Originaldateien unter `legacy-pwa/` sind unveränderte Verhaltensreferenzen und werden nicht in die APK eingebunden.

| PWA-Verhalten | Native Umsetzung |
|---|---|
| Setup-Pflichtfelder, Quantity, freie Quick-/Work-Texte | JobDraft.errors + SetupScreen; erst validiert in Room speichern |
| Vierstellige Nummer, führende Null, erster OCR-Treffer | Rules.firstContainerNumber + CameraX ImageAnalysis + gebündeltes ML Kit |
| Duplicate innerhalb eines Auftrags | Repository-Prüfung und eindeutiger Room-Index jobId/containerNumber |
| Lieferung: sechs feste Fotos, Return: ein Foto | photoLabel/nextPhotoStep und Repository-Prüfung jeder Aufnahme |
| Browserbilder/Zustand | interne JPEG-Dateien und Room statt Browser-/Web-Speicher |
| TXT OUT/ZRH und IN/Return Number | excelText mit exakt vorgegebenem Header; Order bleibt Container-Typ |
| ZIP-Ordnerstruktur, Dateinamen 01–06 | native ZipOutputStream; SAF-Speicherortwahl |
| 30 Minuten ab erstem Export, Re-Export ohne Reset | transaktional persistierter erster Export und eindeutiger Room-Reminder |
| Return verlangt Logger-Stop-Bestätigung | gesperrtes Compose-Formular mit Bestätigungszeitpunkt |
| Browser-Timer/ServiceWorker-Benachrichtigungen | AlarmManager, BroadcastReceiver, NotificationChannel, Boot-Recovery |
| CDN/Tesseract.js und PWA-Installation | entfallen; native APK und gebündeltes OCR-Modell |

Die PWA weist selbst auf eine mögliche Internetverbindung für den ersten OCR-Start hin. Diese Abhängigkeit wird nicht übernommen. Das vom Masterprompt genannte separate UI-Referenzbild liegt auch im geprüften Repository nicht vor.
