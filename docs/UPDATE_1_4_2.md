# ContainerTrack 1.4.2 — Fotos mit Auftragsinformationen teilen

Bisher enthielt die Fotoübergabe lediglich Bilder und einen Betreff. Nun wird ein Nachrichtentext als Android EXTRA_TEXT ergänzt: Auftrag/Referenz, KT bei Lieferungen, Containerkennungen, Containertyp und Anzahl der tatsächlich geteilten Fotos. Returns ergänzen Quick und Zusatzarbeiten. Bei einer Galerieauswahl werden nur die ausgewählten Fotos und zugehörigen Container/Aufträge beschrieben. Betreff, Fotoanhänge und temporäre Leserechte bleiben erhalten, auch beim Android-Fallback ohne Teams.

Teams entscheidet selbst, welche Empfänger und Ziele beim Teilen angeboten werden. Einige Versionen bieten hier nur Chats an. ContainerTrack kann keine Kanalauswahl erzwingen. Die bisherige Zusage „Chat oder Kanal auswählen“ wurde aus den aktiven Hinweisen entfernt. Für einen Kanal zuerst die Fotos in der Galerie herunterladen, dann Teams direkt öffnen und den Beitrag dort mit Fotos und kopierten Auftragsinfos erstellen; die App benötigt weiterhin keine Firmenkonfiguration oder eigene OAuth-Anmeldung.

Teams kann Begleittext zusammen mit Fotoanhängen ignorieren. Deshalb bieten die Übertragungsansicht und die Galerie mit Fotoauswahl ausdrücklich „Auftragsinfos kopieren“ beziehungsweise „Infos kopieren“. Die Zwischenablage wird nur auf diesen Tastendruck geändert; es erfolgt kein automatisches Kopieren. Danach lässt sich der Text im Teams-Beitrag einfügen. In der Übertragungsansicht ist der vollständige Begleittext sichtbar. Ein erfolgreiches Teilen oder Kopieren bestätigt keinen Versand und ändert keine Timer, Fristen oder Auftragsdaten.

Keine Benutzer-/Rollenverwaltung, kein eigener Online-Dienst, keine Team-Galerie oder automatischen Graph-Uploads. App-ID und Room-Schema 2 bleiben erhalten. Nur Debug-APK: 1.4.2 / versionCode 9. Eine Update-Installation setzt weiterhin dasselbe Signaturzertifikat voraus; nicht zur Aktualisierung deinstallieren.

Prüfnachweise: [QA_STATUS.md](QA_STATUS.md). Die tatsächliche Übernahme des Begleittexts und die angebotenen Teams-Ziele müssen mit der installierten Teams-Version geprüft werden; die App bietet dafür den manuellen Kopierweg.
