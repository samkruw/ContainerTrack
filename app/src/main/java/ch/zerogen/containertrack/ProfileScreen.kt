package ch.zerogen.containertrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceSettingsScreen(onContinue:()->Unit={}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Einstellungen",style=MaterialTheme.typography.headlineLarge)
        Text("Lokal auf diesem Gerät",color=Lime)
        Text("Aufträge und Fotos bleiben auf diesem Gerät gespeichert. Zum Senden öffnet die App den Teilen-Dialog von Android oder Teams.",color=Muted)
        HorizontalDivider(color=Hairline)
        Text("Geräteeinstellungen",style=MaterialTheme.typography.titleLarge)
        PermissionsContent()
        OutlineAction("Zurück zu Aufträgen",Icons.Outlined.ArrowBack,onClick=onContinue)
        Text("ZeroGen · ${BuildConfig.VERSION_NAME}",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}
