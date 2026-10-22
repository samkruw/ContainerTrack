package ch.zerogen.containertrack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy=Color(0xFF151A1D)
val Graphite=Color(0xFF1B2125)
val Lime=Color(0xFFBEFF42)
val ElectricBlue=Lime
val OnElectricBlue=Navy
val LightBlue=Color(0xFFC4D1B7)
val Success=Lime
val Warning=Color(0xFFE5BD69)
val Muted=Color(0xFFA8B2B9)
val Hairline=Color(0xFF3B4348)
private val Inter=FontFamily(Font(R.font.inter,FontWeight.Normal),Font(R.font.inter,FontWeight.Medium),Font(R.font.inter,FontWeight.SemiBold),Font(R.font.inter,FontWeight.Bold),Font(R.font.inter,FontWeight.ExtraBold))
private fun type(size:Int,weight:FontWeight=FontWeight.Normal,line:Int=size+6)=TextStyle(fontFamily=Inter,fontWeight=weight,fontSize=size.sp,lineHeight=line.sp)
@Composable
fun ContainerTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Lime,onPrimary=Navy,primaryContainer=Color(0xFF2B3920),onPrimaryContainer=Lime,
        secondary=Lime,onSecondary=Navy,secondaryContainer=Color(0xFF2B3920),onSecondaryContainer=Lime,
        background=Navy,onBackground=Color(0xFFF8FAF9),surface=Navy,onSurface=Color(0xFFF8FAF9),
        surfaceVariant=Graphite,onSurfaceVariant=Muted,outline=Hairline,error=Color(0xFFFF8181)),
        typography=Typography(headlineLarge=type(36,FontWeight.ExtraBold,40),headlineMedium=type(30,FontWeight.Bold,36),titleLarge=type(23,FontWeight.Bold),titleMedium=type(18,FontWeight.SemiBold),bodyLarge=type(16),bodyMedium=type(14),bodySmall=type(12),labelLarge=type(14,FontWeight.SemiBold),labelMedium=type(12,FontWeight.Medium),labelSmall=type(11)),
        shapes=Shapes(small=RoundedCornerShape(5.dp),medium=RoundedCornerShape(7.dp),large=RoundedCornerShape(10.dp))) {
        Surface(Modifier.fillMaxSize(),color=Navy,content=content)
    }
}
@Composable
fun GlassSurface(modifier:Modifier=Modifier,onClick:(()->Unit)?=null,shape:Shape=MaterialTheme.shapes.medium,content:@Composable ()->Unit) {
    if(onClick==null) Surface(modifier,shape=shape,color=Graphite,border=BorderStroke(1.dp,Hairline),content=content)
    else Surface(onClick=onClick,modifier=modifier,shape=shape,color=Graphite,border=BorderStroke(1.dp,Hairline),content=content)
}
@Composable
fun ScreenTitle(title:String,subtitle:String?=null) {
    Column(Modifier.padding(top=8.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,style=MaterialTheme.typography.headlineLarge)
        subtitle?.let{Text(it,style=MaterialTheme.typography.bodyLarge,color=Muted)}
    }
}
@Composable
fun PrimaryButton(text:String,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=MaterialTheme.shapes.small,
        colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Navy)) { Text(text,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Bold) }
}
@Composable
fun OutlineAction(text:String,icon:ImageVector,enabled:Boolean=true,onClick:()->Unit) {
    OutlinedButton(onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,Hairline),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {
        Icon(icon,null,Modifier.size(22.dp));Spacer(Modifier.width(12.dp));Text(text,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}
@Composable
fun Information(text:String,warning:Boolean=false) {
    Text(text,Modifier.fillMaxWidth().padding(vertical=10.dp),style=MaterialTheme.typography.bodyMedium,color=if(warning)Warning else Muted)
}
@Composable
fun StatusBadge(text:String,confirmed:Boolean=false,error:Boolean=false) {
    Surface(color=if(error)Color(0xFF3D2428) else if(confirmed)Color(0xFF29391D) else Color(0xFF272E33),shape=RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(if(confirmed)Icons.Outlined.Check else if(error)Icons.Outlined.Info else Icons.Outlined.Description,null,Modifier.size(16.dp),tint=if(confirmed)Lime else if(error)MaterialTheme.colorScheme.error else Muted)
            Text(text,style=MaterialTheme.typography.labelMedium,color=if(confirmed)Lime else if(error)MaterialTheme.colorScheme.error else Muted)
        }
    }
}
@Composable
fun PrimaryIconButton(text:String,icon:ImageVector,enabled:Boolean=true,onClick:()->Unit) {
    Button(onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=MaterialTheme.shapes.small,colors=ButtonDefaults.buttonColors(containerColor=Lime,contentColor=Navy)) {
        Icon(icon,null,Modifier.size(24.dp));Spacer(Modifier.width(12.dp));Text(text,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Bold)
    }
}
@Composable
fun NumberDisc(number:String) {
    Surface(color=Lime,shape=androidx.compose.foundation.shape.CircleShape) {
        Box(Modifier.size(38.dp),contentAlignment=androidx.compose.ui.Alignment.Center){Text(number,color=Navy,fontSize=22.sp,fontWeight=FontWeight.Bold)}
    }
}
