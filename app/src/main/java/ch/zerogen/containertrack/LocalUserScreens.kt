package ch.zerogen.containertrack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
private fun PinField(value:String,onChange:(String)->Unit,label:String) {
    OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true,
        visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword))
}

@Composable
fun LocalLoginScreen(users:List<LocalUser>,busy:Boolean,onBootstrap:(String,String)->Unit,onLogin:(String,String)->Unit) {
    var name by remember{mutableStateOf("")}
    var selected by remember{mutableStateOf<String?>(null)}
    var pin by remember(selected){mutableStateOf("")}
    var repeat by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Image(painterResource(R.drawable.ic_container),null,Modifier.size(96.dp).align(Alignment.CenterHorizontally))
        Text("Container Track",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.headlineMedium)
        Text(if(users.isEmpty())"Lokale Benutzer einrichten" else "Willkommen zurück",style=MaterialTheme.typography.headlineMedium)
        Text("Nur auf diesem Gerät · keine Online-Anmeldung",color=Muted)
        if(users.isEmpty()) {
            Information("Lege den ersten Administrator an. Bestehende Aufträge bleiben erhalten und sind für den Admin zugänglich.")
            OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Admin-Name")},singleLine=true)
            PinField(pin,{pin=it},"Admin-PIN (6–12 Ziffern)")
            PinField(repeat,{repeat=it},"PIN wiederholen")
            PrimaryButton("Administrator einrichten",!busy && name.isNotBlank() && pin.length>=6 && pin==repeat){onBootstrap(name,pin)}
        } else {
            users.filter{it.active}.forEach{user->
                OutlinedButton(onClick={selected=user.id},enabled=!busy,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.small) {
                    Icon(if(selected==user.id)Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,null)
                    Spacer(Modifier.width(12.dp));Text(user.name,Modifier.weight(1f));Text(user.role.label,color=Muted)
                }
            }
            PinField(pin,{pin=it},"PIN")
            PrimaryButton("Anmelden",!busy && selected!=null && pin.isNotBlank()){selected?.let{onLogin(it,pin)}}
        }
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Benutzer und PIN gelten ausschließlich für diese App-Installation. Fotos werden über die Teams-App geteilt.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LocalProfileScreen(user:LocalUser,busy:Boolean,onUsers:()->Unit,onLogout:()->Unit,onContinue:()->Unit,onPin:(String,String)->Unit) {
    var changingPin by remember{mutableStateOf(false)}
    var oldPin by remember{mutableStateOf("")};var newPin by remember{mutableStateOf("")}
    var settings by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        ScreenTitle(user.name,"Lokal angemeldet · offline nutzbar")
        StatusBadge(user.role.label,confirmed=true)
        Information(if(user.role==LocalRole.ADMIN)"Du verwaltest lokale Benutzer und alle Aufträge auf diesem Gerät." else "Du siehst und bearbeitest deine eigenen lokalen Aufträge.")
        if(user.role==LocalRole.ADMIN)OutlineAction("Benutzer & Rechte",Icons.Outlined.People,!busy,onUsers)
        OutlineAction("Eigene PIN ändern",Icons.Outlined.Lock,!busy){oldPin="";newPin="";changingPin=true}
        OutlineAction("Benutzer wechseln",Icons.Outlined.SwitchAccount,!busy,onLogout)
        OutlineAction("Geräteeinstellungen",Icons.Outlined.Settings,!busy){settings=!settings}
        if(settings)PermissionsContent()
        PrimaryButton("Zurück zu Aufträgen",!busy,onContinue)
        Text("ZeroGen · ${BuildConfig.VERSION_NAME}",color=Muted)
    }
    if(changingPin)AlertDialog(onDismissRequest={if(!busy)changingPin=false},title={Text("Eigene PIN ändern")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        PinField(oldPin,{oldPin=it},"Bisherige PIN");PinField(newPin,{newPin=it},"Neue PIN (6–12 Ziffern)")
    }},confirmButton={TextButton(onClick={onPin(oldPin,newPin);changingPin=false},enabled=!busy && newPin.length>=6){Text("PIN speichern")}},dismissButton={TextButton(onClick={changingPin=false},enabled=!busy){Text("Abbrechen")}})
}

@Composable
fun LocalUserAccessScreen(actor:LocalUser,users:List<LocalUser>,busy:Boolean,onAdd:(String,LocalRole,String)->Unit,onUpdate:(String,String,LocalRole,Boolean,String?)->Unit) {
    var editing by remember{mutableStateOf<LocalUser?>(null)}
    var adding by remember{mutableStateOf(false)}
    var name by remember{mutableStateOf("")};var pin by remember{mutableStateOf("")}
    var role by remember{mutableStateOf(LocalRole.EMPLOYEE)};var active by remember{mutableStateOf(true)}
    if(actor.role!=LocalRole.ADMIN){Information("Nur Administratoren dürfen Benutzer verwalten.");return}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Lokale Benutzer",style=MaterialTheme.typography.headlineMedium)
        Text("${users.count{it.active}} aktive Benutzer · nur auf diesem Gerät",color=Muted)
        users.forEach{user->
            HorizontalDivider(color=Hairline)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person,null,tint=if(user.active)Lime else Muted)
                Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(user.name,style=MaterialTheme.typography.titleMedium);Text("${user.role.label} · ${if(user.active)"Aktiv" else "Deaktiviert"}",color=Muted)}
                IconButton(onClick={editing=user;name=user.name;role=user.role;active=user.active;pin=""},enabled=!busy){Icon(Icons.Outlined.Edit,"Benutzer ${user.name} bearbeiten")}
            }
        }
        PrimaryIconButton("Benutzer hinzufügen",Icons.Outlined.PersonAdd,!busy){adding=true;editing=null;name="";pin="";role=LocalRole.EMPLOYEE;active=true}
        Information("Admin: lokale Benutzer und alle lokalen Aufträge. Mitarbeiter: eigene Aufträge, Fotos und Teilen. Der letzte aktive Admin kann nicht deaktiviert oder herabgestuft werden.")
    }
    if(adding || editing!=null)AlertDialog(onDismissRequest={if(!busy){adding=false;editing=null}},title={Text(if(adding)"Benutzer hinzufügen" else "Benutzer bearbeiten")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Benutzername")},singleLine=true)
        Row {LocalRole.entries.forEach{value->FilterChip(role==value,{role=value},label={Text(value.label)})}}
        if(!adding)Row(verticalAlignment=Alignment.CenterVertically){Switch(active,{active=it});Text("Benutzer aktiv")}
        PinField(pin,{pin=it},if(adding)"PIN (6–12 Ziffern)" else "Neue PIN (optional)")
        if(!adding)Text("Leer lassen, um die bisherige PIN zu behalten.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={if(adding)onAdd(name,role,pin) else editing?.let{onUpdate(it.id,name,role,active,pin.takeIf{it.isNotBlank()})};adding=false;editing=null},enabled=!busy && name.isNotBlank() && (!adding || pin.length>=6)){Text("Speichern")}},dismissButton={TextButton(onClick={adding=false;editing=null},enabled=!busy){Text("Abbrechen")}})
}
