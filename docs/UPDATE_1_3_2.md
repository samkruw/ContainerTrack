# ContainerTrack 1.3.2 — direkt in Teams teilen

Der Button „In Teams teilen“ ist in der Galerie für die ausgewählten Original-JPGs und in der Übertragungsansicht für die vorhandenen Auftragsfotos verfügbar. Eine Microsoft-Konfiguration in ContainerTrack ist dafür nicht erforderlich; auch unvollständige Aufträge können bereits vorhandene Fotos manuell teilen.

ContainerTrack öffnet direkt den JPEG-Teilen-Empfänger der installierten Microsoft-Teams-App. Chat bzw. Kanal werden anschließend innerhalb von Teams gewählt, soweit die installierte Teams-Version und das angemeldete Konto diese Ziele anbieten. Ein Kanal wird durch Android-Sharing weder vorgegeben noch automatisch bestätigt. Teams benötigt sein eigenes angemeldetes Konto und die entsprechenden Zugriffsrechte.

Ist kein Teams-Empfänger verfügbar, öffnet sich der normale Android-Teilen-Dialog. Es werden nur die ausgewählten nummerierten Original-JPG-Kopien mit temporärer Lesefreigabe übergeben; kein ZIP, TXT oder META. Der allgemeine Android-Teilen-Button bleibt verfügbar.

Manuelles Teilen verändert keine Upload-Bestätigung, keinen Completion-Marker und keinen Timer. Die automatische Graph-Übertragung und deren bestätigter Status benötigen weiterhin die Firmenkonfiguration. VersionCode 6, App-ID und Datenbankschema unverändert. Nur Debug-APK.

Tests prüfen Direktstart ohne vorgeschalteten Chooser, Fallback bei fehlender Teams-App einschließlich Lesefreigaben, exakte Auswahlbytes und den Galerie-Button mit ausgewählten Foto-IDs. Die Übertragungsansicht bietet den Button ausdrücklich auch ohne ContainerTrack-Firmenkonto an. Echte Empfänger-/Kanalauswahl in der proprietären Teams-App muss auf dem Nutzergerät geprüft werden.
