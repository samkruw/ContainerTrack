package ch.zerogen.containertrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF061F3B)
val Graphite = Color(0xFF1E293B)
val ElectricBlue = Color(0xFF3882F6)
val LightBlue = Color(0xFF60A5FA)
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)

@Composable
fun ContainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = LightBlue, onPrimary = Navy, primaryContainer = ElectricBlue,
        onPrimaryContainer = Color.White, background = Navy, onBackground = Color(0xFFF8FAFC),
        surface = Navy, onSurface = Color(0xFFF8FAFC), surfaceVariant = Graphite,
        onSurfaceVariant = Color(0xFFBAC7D8), outline = Color(0xFF64748B), error = Color(0xFFEF4444)
    ), shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp))) {
        Box(Modifier.fillMaxSize().background(Navy).drawBehind {
            drawRect(Brush.radialGradient(listOf(ElectricBlue.copy(alpha = .26f), Color.Transparent),
                center = Offset(size.width * .95f, size.height * .12f), radius = size.width * 1.2f))
            drawRect(Brush.radialGradient(listOf(LightBlue.copy(alpha = .12f), Color.Transparent),
                center = Offset(0f, size.height * .7f), radius = size.width))
        }) { content() }
    }
}

@Composable
fun GlassSurface(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
                 shape: Shape = MaterialTheme.shapes.medium, content: @Composable () -> Unit) {
    val panel = modifier.background(Brush.linearGradient(listOf(
        Color(0xC72D4B6C), Color(0xA616314E), Color(0xB31B3859))), shape)
    val edge = BorderStroke(1.dp, Brush.linearGradient(listOf(
        Color.White.copy(alpha = .30f), LightBlue.copy(alpha = .08f), LightBlue.copy(alpha = .22f))))
    if (onClick == null) Surface(modifier = panel, color = Color.Transparent, shape = shape,
        border = edge, content = content)
    else Surface(onClick = onClick, modifier = panel, color = Color.Transparent, shape = shape,
        border = edge, content = content)
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Navy)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Information(text: String, warning: Boolean = false) {
    GlassSurface {
        Text(text, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium,
            color = if (warning) Warning else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
