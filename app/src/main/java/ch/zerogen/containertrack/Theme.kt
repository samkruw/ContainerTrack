package ch.zerogen.containertrack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF0C100F)
val Graphite = Color(0xFF19201C)
val Lime = Color(0xFFB7CD65)
val ElectricBlue = Lime // Kept for existing capture/reminder callers while the palette changes.
val OnElectricBlue = Navy
val LightBlue = Color(0xFFD2DDB7)
val Success = Color(0xFF65D6A0)
val Warning = Color(0xFFE6BB68)

@Composable
fun ContainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Lime, onPrimary = Navy, primaryContainer = Color(0xFF303A23), onPrimaryContainer = Color(0xFFEDF3D8),
        background = Navy, onBackground = Color(0xFFF0F2EA), surface = Navy, onSurface = Color(0xFFF0F2EA),
        surfaceVariant = Graphite, onSurfaceVariant = Color(0xFFBEC7BC), outline = Color(0xFF667365), error = Color(0xFFFF9990)
    ), shapes = Shapes(small = RoundedCornerShape(8.dp),medium = RoundedCornerShape(12.dp),large = RoundedCornerShape(20.dp))) {
        Surface(Modifier.fillMaxSize(),color = Navy,content = content)
    }
}

@Composable
fun GlassSurface(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
                 shape: Shape = MaterialTheme.shapes.medium, content: @Composable () -> Unit) {
    val border = BorderStroke(1.dp,Color(0xFF344036))
    if (onClick == null) Surface(modifier,shape = shape,color = Graphite,border = border,content = content)
    else Surface(onClick = onClick,modifier = modifier,shape = shape,color = Graphite,border = border,content = content)
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 4.dp,bottom = 8.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title,fontSize = 24.sp,fontWeight = FontWeight.Bold,lineHeight = 30.sp)
        subtitle?.let { Text(it,style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick,enabled = enabled,modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lime,contentColor = Navy)) { Text(text,fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Information(text: String, warning: Boolean = false) {
    GlassSurface {
        Text(text,Modifier.fillMaxWidth().padding(14.dp),style = MaterialTheme.typography.bodyMedium,
            color = if (warning) Warning else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
