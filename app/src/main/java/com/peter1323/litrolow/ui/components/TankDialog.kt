package com.peter1323.litrolow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peter1323.litrolow.R

// Solo son atajos: se puede escribir cualquier cantidad válida.
private val TANK_SHORTCUTS = listOf(30, 40, 50, 60)

/** Diálogo para indicar los litros que se suelen echar, con atajos o escribiendo cualquier cantidad. */
@Composable
fun TankDialog(litres: Int, onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(litres.toString()) }
    var isInvalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tank_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.tank_question))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TANK_SHORTCUTS.forEach { shortcut ->
                        FilterChip(
                            selected = text == shortcut.toString(),
                            onClick = { text = shortcut.toString(); isInvalid = false },
                            label = { Text(shortcut.toString()) },
                            colors = selectedChipColors(),
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; isInvalid = false },
                    label = { Text(stringResource(R.string.tank_litres)) },
                    isError = isInvalid,
                    supportingText = { if (isInvalid) Text(stringResource(R.string.tank_invalid)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onSave(text)) onDismiss() else isInvalid = true }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
