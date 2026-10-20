package ch.zerogen.containertrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    ), shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp)),
        content = content)
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
        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun Information(text: String, warning: Boolean = false) {
    Surface(color = Graphite, shape = MaterialTheme.shapes.medium) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium,
            color = if (warning) Warning else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
