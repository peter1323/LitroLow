package com.peter1323.litrolow.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.data.repository.AlertSaveResult
import kotlinx.coroutines.launch

/**
 * Acción de crear una alerta; [trigger] dibuja el botón que la lanza. Pide el permiso de notificaciones justo en este momento, porque es
 * cuando el usuario entiende para qué sirve, y crea la alerta aunque lo rechace: puede activarlo luego.
 */
@Composable
fun CreateAlertAction(
    station: Station,
    onCreate: suspend (FuelType, String) -> AlertSaveResult,
    trigger:@Composable (onClick: () -> Unit) -> Unit,
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
    if (showDialog) {
        AlertFormDialog(R.string.create_alert, soldFuels, onSave = onCreate, onDismiss = { showDialog = false })
    }
}

/** Texto que explica por qué no se ha podido guardar una alerta. */
@get:StringRes
private val AlertSaveResult.message: Int
    get() = if (this == AlertSaveResult.DUPLICATE) R.string.alert_duplicate else R.string.alert_invalid_price

/** Diálogo para elegir el combustible y el precio máximo; sirve para crear y para editar una alerta. */
@Composable
fun AlertFormDialog(
    @StringRes title: Int,
    fuels: List<FuelType>,
    initialFuel: FuelType = fuels.first(),
    initialPrice: String = "",
    onSave: suspend (FuelType, String) -> AlertSaveResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fuel by rememberSaveable { mutableStateOf(initialFuel) }
    var maxPrice by rememberSaveable { mutableStateOf(initialPrice) }
    var problem by rememberSaveable { mutableStateOf<AlertSaveResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelChips(fuel, { fuel = it; problem = null }, fuels = fuels)
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { maxPrice = it; problem = null },
                    label = { Text(stringResource(R.string.alert_max_price)) },
                    placeholder = { Text(stringResource(R.string.alert_price_example)) },
                    isError = problem != null,
                    supportingText = { problem?.let { Text(stringResource(it.message)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val result = onSave(fuel, maxPrice)
                    if (result == AlertSaveResult.SAVED) {
                        Toast.makeText(context, R.string.alert_saved, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        problem = result
                    }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
