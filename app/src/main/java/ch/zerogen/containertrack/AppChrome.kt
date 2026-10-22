package ch.zerogen.containertrack

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BrandHeader(identity:MicrosoftIdentity?,onProfile:()->Unit) {
    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_container),null,Modifier.size(36.dp));Spacer(Modifier.width(8.dp))
            Text("Container Track",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            IconButton(onClick=onProfile) {
                if(identity==null) Icon(Icons.Outlined.Settings,"Profil und Einstellungen",tint=Muted)
                else Surface(shape=androidx.compose.foundation.shape.CircleShape,color=Hairline) {
                    Box(Modifier.size(34.dp),contentAlignment=Alignment.Center){Text(identity.displayName.split(' ').filter{it.isNotBlank()}.take(2).joinToString(""){it.first().uppercase()},style=MaterialTheme.typography.labelLarge)}
                }
            }
        }
        HorizontalDivider(color=Hairline)
    }
}
@Composable
fun ReferenceBottomBar(route:String,enabled:Boolean,onNavigate:(String)->Unit) {
    Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color=Hairline)
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            listOf(Triple("jobs","Aufträge",Icons.Outlined.Description),Triple("scan","Kamera",Icons.Outlined.PhotoCamera),Triple("gallery","Galerie",Icons.Outlined.Image)).forEachIndexed{index,(path,label,icon)->
                if(index>0) VerticalDivider(Modifier.height(38.dp).align(Alignment.CenterVertically),color=Hairline)
                val selected=route==path || (path=="scan" && (route.startsWith("photo/") || route.startsWith("scan/")))
                Column(Modifier.weight(1f).fillMaxHeight().clickable(enabled=enabled,role=Role.Tab){onNavigate(path)}.padding(top=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Icon(icon,null,tint=if(selected)Lime else Muted,modifier=Modifier.size(23.dp))
                    Text(label,style=MaterialTheme.typography.labelMedium,color=if(selected)Lime else Muted)
                    if(selected) HorizontalDivider(Modifier.width(52.dp),thickness=2.dp,color=Lime)
                }
            }
        }
    }
}
