package ch.zerogen.containertrack

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(identity:MicrosoftIdentity?,status:String,configured:Boolean,busy:Boolean,
    onLogin:(Activity)->Unit,onLogout:()->Unit,onSwitch:(Activity)->Unit,onContinue:()->Unit={},onAccess:()->Unit={}) {
    val activity=LocalActivity.current
    var settings by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(identity==null) {
            Spacer(Modifier.height(12.dp))
            Image(painterResource(R.drawable.ic_container),null,Modifier.size(104.dp).align(Alignment.CenterHorizontally))
            Text("Container Track",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.headlineMedium)
            Text("Fotodokumentation für dein Team",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted)
            Spacer(Modifier.height(12.dp))
            Text("Willkommen zurück",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.headlineMedium)
            Text("Melde dich mit deinem Firmenkonto an.",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted)
            OutlinedButton(onClick={activity?.let(onLogin)},enabled=configured && activity!=null && !busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=MaterialTheme.shapes.small,border=androidx.compose.foundation.BorderStroke(1.5.dp,Lime)) {
                Image(painterResource(R.drawable.ic_microsoft),null,Modifier.size(25.dp));Spacer(Modifier.width(12.dp));Text("Mit Microsoft anmelden",fontWeight=FontWeight.Bold,color=if(configured)MaterialTheme.colorScheme.onSurface else Muted)
            }
            Text(if(configured)status else "Firmenanmeldung ist für diesen Build noch nicht eingerichtet.",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted,style=MaterialTheme.typography.bodySmall)
            Text("Nicht verbunden",Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=Muted,style=MaterialTheme.typography.labelSmall)
            TextButton(onClick=onContinue,modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Offline weiterarbeiten")}
            HorizontalDivider(color=Hairline)
            ProfileBenefit(Icons.Outlined.People,"Dein Firmenkonto ist dein persönlicher Zugang")
            ProfileBenefit(Icons.Outlined.CloudOff,"Fotos bleiben offline gespeichert, bis sie gesendet sind")
            ProfileBenefit(Icons.Outlined.VerifiedUser,"Teamzugriff richtet sich nach den Firmenberechtigungen")
        } else {
            Text("DEIN FIRMENKONTO",color=Lime,style=MaterialTheme.typography.labelLarge)
            Text(identity.displayName,style=MaterialTheme.typography.headlineLarge)
            Text(identity.username,color=Muted)
            StatusBadge(status,confirmed=true)
            HorizontalDivider(color=Hairline)
            OutlineAction("Konto wechseln",Icons.Outlined.SwitchAccount,!busy && activity!=null){activity?.let(onSwitch)}
            OutlineAction("Abmelden",Icons.Outlined.Logout,!busy,onLogout)
        }
        HorizontalDivider(color=Hairline)
        OutlineAction("Benutzer & Rechte",Icons.Outlined.People,onClick=onAccess)
        OutlineAction("Geräteeinstellungen",Icons.Outlined.Settings){settings=!settings}
        if(settings) PermissionsContent()
        Text("ZeroGen · ${BuildConfig.VERSION_NAME}",Modifier.fillMaxWidth().padding(vertical=12.dp),textAlign=TextAlign.Center,color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun ProfileBenefit(icon:androidx.compose.ui.graphics.vector.ImageVector,text:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Icon(icon,null,Modifier.size(30.dp),tint=Lime);Text(text,style=MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color=Hairline)
}
@Composable
fun UserAccessScreen(identity:MicrosoftIdentity?) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        ScreenTitle("Benutzer & Rechte","Anmeldung mit Firmenkonto")
        HorizontalDivider(color=Hairline)
        if(identity!=null) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Outlined.AccountCircle,null,Modifier.size(48.dp),tint=Lime)
                Column {Text(identity.displayName,style=MaterialTheme.typography.titleLarge);Text(identity.username,color=Muted)}
            }
            StatusBadge("Angemeldet",confirmed=true)
        } else Information("Kein Firmenkonto verbunden.")
        HorizontalDivider(color=Hairline)
        Text("Rechte & Zugriffe",style=MaterialTheme.typography.titleLarge)
        Information("Die Berechtigungen werden von Microsoft 365 vergeben. Eine Administratorrolle oder eine Team-Benutzerliste ist in diesem Build nicht bestätigt.")
        ProfileBenefit(Icons.Outlined.PhotoLibrary,"Galerie: tatsächlich auf diesem Gerät gespeicherte Fotos")
        ProfileBenefit(Icons.Outlined.CloudUpload,"Teams: Übertragung nur mit Zugriff auf den Zielkanal")
        OutlineAction("Teamzugriff verwalten",Icons.Outlined.ManageAccounts,false){}
        Text("Benutzer einladen und Rechte ändern: über die Firmenadministration.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}
