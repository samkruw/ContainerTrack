package ch.zerogen.containertrack

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Suppress("DEPRECATION")
@Composable
fun PhotoShareInfoAction(text:String,enabled:Boolean=true,compact:Boolean=false) {
    val clipboard=LocalClipboardManager.current
    var copied by remember(text){mutableStateOf(false)}
    val label=if(compact) {if(copied)"Infos kopiert" else "Infos kopieren"} else {if(copied)"Auftragsinfos kopiert" else "Auftragsinfos kopieren"}
    OutlineAction(label,Icons.Outlined.ContentCopy,enabled && text.isNotBlank()) {
        clipboard.setText(AnnotatedString(text))
        copied=true
    }
}
