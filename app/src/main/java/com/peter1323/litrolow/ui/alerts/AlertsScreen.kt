package com.peter1323.litrolow.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.FuelType
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.model.PriceLevel
import com.peter1323.litrolow.ui.components.AlertFormDialog
import com.peter1323.litrolow.ui.components.BrandBadge
import com.peter1323.litrolow.ui.components.BrandHeader
import com.peter1323.litrolow.ui.components.LoadingView
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.PricePill
import com.peter1323.litrolow.ui.components.formatAlertPrice
import com.peter1323.litrolow.ui.components.formatDecimal
import com.peter1323.litrolow.ui.components.formatPrice

private const val CENTS_PER_EURO = 100

/** Lista de alertas de precio, o una explicación de cómo crearlas si aún no hay ninguna. */
@Composable
fun AlertsScreen(viewModel: AlertsViewModel, onAlertClick: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingView()
        state.alerts.isEmpty() -> MessageView(stringResource(R.string.empty_alerts))
        else -> LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BrandHeader {
                    Text(stringResource(R.string.tab_alerts), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.alerts_met_count, state.metCount, state.alerts.size))
                }
            }
            item { NotificationsOffNotice() }
            items(state.alerts, key = { it.id }) { alert ->
                AlertCard(
                    alert,
                    state.currentPrices[alert.id],
                    state.soldFuels[alert.stationId].orEmpty(),
                    viewModel,
                    Modifier.padding(horizontal = 16.dp),
                ) { onAlertClick(alert.stationId) }
            }
        }
    }
}

/** Avisa si las notificaciones están desactivadas, porque las alertas no podrían llegar. */
@Composable
private fun NotificationsOffNotice() {
    if (!NotificationManagerCompat.from(LocalContext.current).areNotificationsEnabled()) {
        Text(
            stringResource(R.string.notifications_off),
            Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Tarjeta de una alerta con su gasolinera, precio actual, precio máximo y botones de editar y borrar. */
@Composable
private fun AlertCard(
    alert: PriceAlert,
    currentPrice: Double?,
    soldFuels: List<FuelType>,
    viewModel: AlertsViewModel,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(alert.stationName)
                Text(alert.stationName, Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold)
            }
            currentPrice?.let { AlertProgress(it, alert.maxPrice) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.alert_summary, stringResource(alert.fuel.label), formatAlertPrice(alert.maxPrice)),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_alert))
                }
                IconButton(onClick = { viewModel.delete(alert.id) }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete_alert))
                }
            }
        }
    }
    if (isEditing) {
        AlertFormDialog(
            title = R.string.edit_alert,
            // Si la gasolinera ya no está en los datos, al menos se puede corregir el precio.
            fuels = (soldFuels + alert.fuel).distinct(),
            initialFuel = alert.fuel,
            initialPrice = formatAlertPrice(alert.maxPrice),
            onSave = { fuel, price -> viewModel.update(alert, fuel, price) },
            onDismiss = { isEditing = false },
        )
    }
}

/** Precio actual junto a si la alerta se cumple o cuánto le falta. */
@Composable
private fun AlertProgress(currentPrice: Double, maxPrice: Double) {
    val isMet = currentPrice <= maxPrice
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(formatPrice(currentPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        PricePill(
            if (isMet) {
                stringResource(R.string.alert_met)
            } else {
                stringResource(R.string.alert_cents_left, formatDecimal((currentPrice - maxPrice) * CENTS_PER_EURO, 1))
            },
            if (isMet) PriceLevel.CHEAP else PriceLevel.MEDIUM,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
