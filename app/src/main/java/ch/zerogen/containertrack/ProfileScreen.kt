package ch.zerogen.containertrack

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(identity: MicrosoftIdentity?, status: String, configured: Boolean, busy: Boolean,
                  onLogin: (Activity)->Unit, onLogout: ()->Unit, onSwitch: (Activity)->Unit) {
    val activity=LocalContext.current as? Activity
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        ScreenTitle("Profil","Microsoft und Geräteeinstellungen")
        GlassSurface { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(identity?.displayName ?: "Nicht verbunden",style=MaterialTheme.typography.titleLarge)
            identity?.let { Text(it.username) }
            Text(status)
            if(!configured) Information("Microsoft ist noch nicht eingerichtet. Tenant, App-Registrierung, Debug-Redirect und Teams-Ziel müssen für den Build konfiguriert werden.",warning=true)
            if(identity==null) PrimaryButton("Mit Firmenkonto anmelden",enabled=configured && activity!=null && !busy) { activity?.let(onLogin) }
            else {
                OutlinedButton(onClick=onLogout,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Abmelden") }
                OutlinedButton(onClick={activity?.let(onSwitch)},enabled=!busy && activity!=null,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Konto wechseln") }
            }
            Text("Teams: Op530-Lal · ausschließlich nummerierte JPEG-Fotos. Ohne Verbindung bleiben Aufträge und Fotos lokal erhalten.",style=MaterialTheme.typography.bodySmall)
        } }
        PermissionsContent()
    }
}
