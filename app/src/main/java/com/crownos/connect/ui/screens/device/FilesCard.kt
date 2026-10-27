package com.crownos.connect.ui.screens.device

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.crownos.connect.files.DownloadsPublisher
import com.crownos.connect.files.IncomingTransfer
import com.crownos.connect.files.ReceivedFile
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownProgressBar
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingRowContent
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider

@Composable
fun FilesCard(transfer: IncomingTransfer?, received: List<ReceivedFile>, onDismissFailure: () -> Unit) {
    if (transfer == null && received.isEmpty()) return
    val context = LocalContext.current
    transfer?.failure?.let {
        CrownCallout("A file did not arrive", Tone.Warning, body = it)
    }
    SettingsCardTitled("Files") {
        if (transfer != null && transfer.failure == null) {
            SettingRowContent {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingRow(
                        "Receiving ${transfer.fileName ?: "a file"}",
                        description = progressText(context, transfer),
                        icon = CrownIcons.FileDown,
                    )
                    CrownProgressBar(transfer.fraction, Modifier.fillMaxWidth())
                }
            }
            if (received.isNotEmpty()) SettingsDivider()
        }
        if (transfer?.failure != null) {
            SettingRow("Clear the error", icon = CrownIcons.X) {
                CrownButton("Dismiss", onDismissFailure, size = ButtonSize.Small)
            }
            if (received.isNotEmpty()) SettingsDivider()
        }
        received.forEachIndexed { index, file ->
            SettingRow(
                file.name,
                description = "${Formatter.formatShortFileSize(context, file.bytes)} · Downloads/CrownConnect",
                icon = CrownIcons.Folder,
            ) {
                CrownButton("Open", {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(file.uri, DownloadsPublisher.mimeOf(file.name))
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }, size = ButtonSize.Small)
            }
            if (index < received.lastIndex) SettingsDivider()
        }
    }
}

private fun progressText(context: Context, transfer: IncomingTransfer): String {
    if (transfer.totalBytes <= 0) return "Connecting to ${transfer.peerName}…"
    val delivered = Formatter.formatShortFileSize(context, transfer.deliveredBytes)
    val total = Formatter.formatShortFileSize(context, transfer.totalBytes)
    return "$delivered of $total from ${transfer.peerName}"
}
