package com.peter1323.litrolow.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.Station

/**
 * Acción de crear una alerta; [trigger] dibuja el botón que la lanza. Pide el permiso de notificaciones justo en este momento, porque es
 * cuando el usuario entiende para qué sirve, y crea la alerta aunque lo rechace: puede activarlo luego.
 */
@Composable
fun CreateAlertAction(
    station: Station,
    onCreate: (FuelType, String) -> Boolean,
    trigger: @Composable (onClick: () -> Unit) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        showDialog = true
    }
    val soldFuels = FuelType.entries.filter { station.prices.of(it) != null }
    if (soldFuels.isEmpty()) return

    trigger {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showDialog = true
        }
    }
    if (showDialog) CreateAlertDialog(soldFuels, onCreate) { showDialog = false }
}

/** Diálogo para elegir el combustible y el precio máximo de la alerta. */
@Composable
private fun CreateAlertDialog(
    fuels: List<FuelType>,
    onCreate: (FuelType, String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var fuel by rememberSaveable { mutableStateOf(fuels.first()) }
    var maxPrice by rememberSaveable { mutableStateOf("") }
    var isInvalid by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_alert)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelChips(fuel, { fuel = it }, fuels = fuels)
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it; isInvalid = false },
                    label = { Text(stringResource(R.string.alert_max_price)) },
                    isError = isInvalid,
                    supportingText = { if (isInvalid) Text(stringResource(R.string.alert_invalid_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
        }
        },
        confirmButton = {
            TextButton(onClick = {
                if (onCreate(fuel, maxPrice)) {
                    Toast.makeText(context, R.string.alert_saved, Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    isInvalid = true
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
