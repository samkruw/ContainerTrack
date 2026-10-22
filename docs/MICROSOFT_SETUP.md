# Microsoft-Konfiguration für den Debug-Build

Die App verwendet OAuth/MSAL als Public Client. Es gibt kein Client Secret und kein Passwortformular. Benötigt wird eine Single-Tenant-App-Registrierung im eigenen Microsoft-Entra-Tenant mit Android-Plattform und delegierten Graph-Berechtigungen `Files.ReadWrite.All` sowie `Channel.ReadBasic.All`. Firmenrichtlinien können Admin-Zustimmung verlangen. Zugriff wird mit dem angemeldeten Benutzer geprüft, nicht über eine erfundene Admin-Rolle.

Diese öffentlichen Repository-Variablen werden von GitHub Actions in BuildConfig übernommen (alternativ gleichnamige Gradle-Properties/Umgebungsvariablen):

| Variable | Inhalt |
| --- | --- |
| CT_MICROSOFT_TENANT_ID | Tenant-GUID |
| CT_MICROSOFT_CLIENT_ID | App-/Client-GUID |
| CT_MICROSOFT_REDIRECT_URI | Für Android registriertes `msauth://ch.zerogen.containertrack/<Signaturhash>` |
| CT_MICROSOFT_TEAM_ID | GUID des Teams |
| CT_MICROSOFT_CHANNEL_ID | ID des Kanals Op530-Lal |

Der Android-Paketname und der tatsächliche Debug-Zertifikat-Hash müssen in Entra übereinstimmen. Bei neu generiertem Debug-Keystore kann sich dieser Hash ändern. Für wiederholbar anmeldbare Debug-Builds muss GitHub den gleichen, sicher verwalteten Debug-Keystore verwenden; dies benötigt keine Release-Signierung. Keine Schlüssel, Token oder Passwörter im Chat oder Repository ablegen. Die Redirect-URI muss exakt zum registrierten Android-Hash passen, einschließlich URL-Codierung.

Graph prüft den Kanalnamen vor dem Schreiben und ermittelt den zugehörigen Dateienordner. Gespeichert wird unter `<Job-ID>/<Container-Nummer>/0708_01.jpg`. Private/geteilte Kanäle können durch Tenant-/SharePoint-Richtlinien eingeschränkt sein. Fehler führen zu sichtbarem Wartestatus oder Upload-Fehler, niemals zu einer Erfolgsbehauptung.

Abnahme mit echtem Firmenkonto: anmelden; abgeschlossenen Testauftrag übertragen; ausschließlich nummerierte JPEGs im richtigen Kanal prüfen; Netz unterbrechen und wiederherstellen; nur unbestätigte Dateien werden erneut gesendet; abmelden/Konto wechseln und sicherstellen, dass fremde Queue nicht übertragen wird. Ein Android-Teilen-Dialog ist kein Nachweis der Graph-Integration.

Solange die Variablen fehlen, sind Anmeldung und automatischer Upload deaktiviert. Lokale Funktionen, manuelles Foto-Teilen und ZIP-Export bleiben verfügbar. Ein echter Microsoft-End-to-End-Test ist dann offen.
