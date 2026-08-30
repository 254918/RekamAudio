package com.example.rekamaudio.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.rekamaudio.R
import com.example.rekamaudio.data.model.Recording

@Composable
fun RenameDialog(
    recording: Recording,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val (text, setText) = remember(recording.fileName) { mutableStateOf(recording.fileName.removeSuffix(".m4a")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_recording)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = setText,
                label = { Text(stringResource(R.string.name_label)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(text) }) { Text(stringResource(R.string.rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
