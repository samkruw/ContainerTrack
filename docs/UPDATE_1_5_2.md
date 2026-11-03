# Update 1.5.2 – Sauberfotos bei Return überspringen

Nach dem Pflicht-Inhaltsfoto kann für jeden bereits sauberen Return-Container auf dem Tab „Sauber“ die Aktion „Sauberfotos überspringen“ bestätigt werden. Dieser Status wird lokal pro Container gespeichert und lässt sich bis zum ZIP-Export oder zur E-Mail-Bestätigung rückgängig machen. Für nicht übersprungene Container bleibt die bisherige Regel: genau so viele Sauberfotos wie Anfangsfotos.

Das Überspringen ist nur möglich, solange für den Container noch kein Sauberfoto vorliegt. Sind schon Sauberfotos vorhanden, können sie einzeln gelöscht und danach übersprungen werden. Nach dem Überspringen sind neue Sauber-Aufnahmen bis zum Rückgängigmachen gesperrt. Die E-Mail führt die übersprungenen Container im Text auf und hängt alle tatsächlich vorhandenen Fotos an. ZIP-Metadaten enthalten den Status je Container. Die 24h-Erinnerung endet weiterhin erst nach ausdrücklicher Bestätigung „E-Mail gesendet“.

Die Room-Migration 3→4 ergänzt `cleanPhotoSkipped` mit Standardwert `false`. Damit behalten bestehende Returns ihre bisherige Fotopflicht. Eine Änderung des Status erhöht die Dokumentationsrevision und macht eine vorbereitete E-Mail ungültig.
