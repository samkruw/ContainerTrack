package ch.zerogen.containertrack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bounded camera workspace: reserve controls first, give remaining space to the image. */
@Composable
fun CaptureWorkspace(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    media: @Composable (Modifier) -> Unit,
    actions: @Composable () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().testTag("capture-workspace")) {
        val landscape = maxWidth > maxHeight && maxWidth >= 480.dp
        val railWidth = (maxWidth * .42f).coerceIn(220.dp, 320.dp)
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(railWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    header()
                    Spacer(Modifier.weight(1f))
                    actions()
                }
                media(Modifier.weight(1f).fillMaxHeight().testTag("capture-media"))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                header()
                media(Modifier.weight(1f).fillMaxWidth().testTag("capture-media"))
                actions()
            }
        }
    }
}

@Composable
fun CaptureContextHeader(title: String, subtitle: String, status: String, onEdit: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(lineHeight = 16.sp), fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(subtitle, status).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp), color = Lime, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Container-Nummer bearbeiten") }
    }
}

@Composable
internal fun CaptureAction(text: String, enabled: Boolean = true, primary: Boolean = false,
                           modifier: Modifier = Modifier, onClick: () -> Unit) {
    val content: @Composable RowScope.() -> Unit = {
        Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (primary) Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled,
        shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), content = content)
    else OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled,
        shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), content = content)
}
