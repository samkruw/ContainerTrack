# ContainerTrack 1.4.3 — Fotoinfos ohne KT

Die KT-Nummer entfällt aus sämtlichen Foto-Begleittexten und aus den kopierbaren Auftragsinfos. Die Übertragungsansicht zeigt die Auftragsreferenz statt einer KT-Kennung. Auftrag/Referenz, Container, Typ, tatsächliche Fotoanzahl und Return-Arbeiten bleiben enthalten. Das separate KT-Feld in Erfassung/Übersicht und bereits gespeicherte Daten bleiben erhalten; eine vollständige Entfernung aus der App ist nicht Bestandteil dieser Änderung.

Fotodateien heißen weiterhin ausschließlich nach Container und Reihenfolge, beispielsweise 0708_01.jpg. Ein zusätzlicher Android-Test prüft die tatsächlich über den ContentProvider gemeldeten DISPLAY_NAME-Werte und URI-Dateinamen. „shared-photos“ ist ein privater Cacheordner, kein Dateiname. Ob Teams mit „Shared Photos“ eine Fotogruppe bezeichnet oder einzelne Dateien umbenennt, ist ohne Rückmeldung vom Nutzergerät noch nicht geklärt. Es wird keine Korrektur des internen Teams-Verhaltens behauptet.

Keine Änderung an Aufträgen, Fotos, Datenbankschema, Fristen oder Signaturanforderungen. Teams entscheidet weiter über Ziele und Textübernahme; der ausdrückliche Kopierweg bleibt verfügbar. Nur Debug-APK: 1.4.3 / versionCode 10. Aktuelle Prüfnachweise: [QA_STATUS.md](QA_STATUS.md).
