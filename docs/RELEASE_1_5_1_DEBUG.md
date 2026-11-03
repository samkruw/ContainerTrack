# Container Track 1.5.1 – Debug-Testversion

Die APK kann unter **Assets → ContainerTrack-1.5.1-debug.apk** direkt heruntergeladen werden.
Die separate `.sha256`-Datei enthält die Prüfsumme.

- Flexible Kameravorschau mit festem Auslöser und kompakten Aktionen.
- Seitliche Bedienung im Querformat und eigene Übersicht für gespeicherte Fotos.
- OCR pausiert während der manuellen Nummerneingabe.
- Return-Zusatzfotos, gleich viele Leer-/Sauber-Fotos, 24h-Erinnerung und Auftrags-E-Mail bleiben erhalten.

**Die Geräte- und visuelle Abnahme ist noch offen.** Unit-Tests, Lint und der Debug-Build
waren erfolgreich; der vollständige GitHub-Gerätejob ist fehlgeschlagen und seine
konkrete Ursache noch nicht eingegrenzt. Diese Vorabversion ist keine vollständig
freigegebene stabile Version.

Die APK wird unverändert aus dem erfolgreichen Build-Job von
[Lauf 40834801102](https://github.com/samkruw/ContainerTrack/actions/runs/40834801102)
übernommen (App-Commit `651f2acd0b4c5ea28406b77ab507bb9aa318a015`, Version 1.5.1 / Code 13).
Sie wird weder neu gebaut noch neu signiert. Ein Update einer bereits installierten
APK setzt ein übereinstimmendes Signaturzertifikat voraus. Eine Deinstallation würde
lokale Aufträge und Fotos löschen.
