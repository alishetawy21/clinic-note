package com.example

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ClinNoteJsDialog(
  dialog: JsDialogRequest,
  onConfirm: (String?) -> Unit,
  onDismiss: () -> Unit,
) {
  when (dialog) {
    is JsDialogRequest.Alert -> {
      AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.app_name)) },
        text = { Text(text = dialog.message) },
        confirmButton = {
          Button(
            onClick = { onConfirm(null) },
            modifier = Modifier.testTag("js_alert_ok_button"),
          ) {
            Text(text = stringResource(R.string.dialog_ok))
          }
        },
      )
    }

    is JsDialogRequest.Confirm -> {
      AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.app_name)) },
        text = { Text(text = dialog.message) },
        confirmButton = {
          Button(
            onClick = { onConfirm(null) },
            modifier = Modifier.testTag("js_confirm_ok_button"),
          ) {
            Text(text = stringResource(R.string.dialog_confirm))
          }
        },
        dismissButton = {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("js_confirm_cancel_button"),
          ) {
            Text(text = stringResource(R.string.dialog_cancel))
          }
        },
      )
    }

    is JsDialogRequest.Prompt -> {
      var inputText by remember(dialog) { mutableStateOf(dialog.defaultValue) }
      AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialog.message) },
        text = {
          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            singleLine = false,
            maxLines = 4,
            label = { Text(text = stringResource(R.string.dialog_input_label)) },
            modifier = Modifier.fillMaxWidth().testTag("js_prompt_input"),
          )
        },
        confirmButton = {
          Button(
            onClick = { onConfirm(inputText) },
            modifier = Modifier.testTag("js_prompt_ok_button"),
          ) {
            Text(text = stringResource(R.string.dialog_ok))
          }
        },
        dismissButton = {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("js_prompt_cancel_button"),
          ) {
            Text(text = stringResource(R.string.dialog_cancel))
          }
        },
      )
    }
  }
}

@Composable
fun ClinNoteExportDialog(
  exportRequest: ExportRequest,
  onSaveToDevice: () -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val formattedSize =
    remember(exportRequest.sizeBytes) {
      Formatter.formatShortFileSize(context, exportRequest.sizeBytes)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.export_ready_title),
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text =
            stringResource(R.string.file_size_label, exportRequest.fileName, formattedSize),
          style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onSaveToDevice,
            modifier = Modifier.weight(1f).testTag("export_save_device_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Download,
              contentDescription = stringResource(R.string.export_save_device),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.export_save_device))
          }
          OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f).testTag("export_share_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = stringResource(R.string.export_share),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.export_share))
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.padding(top = 4.dp).testTag("export_close_button"),
      ) {
        Text(text = stringResource(R.string.close))
      }
    },
  )
}

@Composable
fun ClinNoteExitDialog(
  onConfirmExit: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.exit_prompt_title)) },
    text = { Text(text = stringResource(R.string.exit_prompt_message)) },
    confirmButton = {
      Button(
        onClick = onConfirmExit,
        modifier = Modifier.testTag("exit_confirm_button"),
      ) {
        Text(text = stringResource(R.string.exit_confirm))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("exit_cancel_button"),
      ) {
        Text(text = stringResource(R.string.dialog_cancel))
      }
    },
  )
}
