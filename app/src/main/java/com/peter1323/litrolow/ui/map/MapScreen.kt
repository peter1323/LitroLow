package com.peter1323.litrolow.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peter1323.litrolow.R
import com.peter1323.litrolow.data.model.Station
import com.peter1323.litrolow.ui.components.FuelChips
import com.peter1323.litrolow.ui.components.MessageView
import com.peter1323.litrolow.ui.components.StationCard

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Pantalla del mapa: se elige un punto (dirección o ubicación) y muestra lo cercano con la más barata destacada. */
@Composable
fun MapScreen(viewModel: MapViewModel, isDark: Boolean, onStationClick: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AddressSearch(viewModel::searchAddress)
        MyLocationButton(viewModel::useCurrentLocation, viewModel::onPermissionDenied)
        FuelChips(state.fuel, viewModel::selectFuel)
        if (state.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        state.problem?.let { ProblemNotice(it) }

        when {
            state.center == null -> MessageView(stringResource(R.string.map_explanation))
            state.nearby.isEmpty() -> MessageView(stringResource(R.string.map_no_stations))
            else -> {
                StationsMap(state, isDark, onStationClick, Modifier.weight(1f).padding(top = 8.dp))
                state.cheapest?.let { CheapestCard(it, state, onStationClick) }
            }
        }
    }
}

/** Campo para escribir una dirección, localidad o código postal. */
@Composable
private fun AddressSearch(onSearch: (String) -> Unit) {
    var address by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val search = {
        keyboard?.hide()
        onSearch(address)
    }
    OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.map_address_hint)) },
        trailingIcon = {
            IconButton(onClick = search) {
                Icon(Icons.Default.Search, stringResource(R.string.map_search))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
    )
}

/** Botón que pide el permiso solo si hace falta; la explicación del uso está visible antes de pulsarlo. */
@Composable
private fun MyLocationButton(onGranted: () -> Unit, onDenied: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) onGranted() else onDenied()
    }
    OutlinedButton(
        onClick = {
            val isGranted = LOCATION_PERMISSIONS.any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (isGranted) onGranted() else launcher.launch(LOCATION_PERMISSIONS)
        },
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Place, contentDescription = null)
            Text(stringResource(R.string.map_use_location))
        }
    }
}

/** Explica qué ha fallado y qué puede hacer el usuario. */
@Composable
private fun ProblemNotice(problem: MapProblem) {
    val message = when (problem) {
        MapProblem.LOCATION_UNAVAILABLE -> R.string.map_location_unavailable
        MapProblem.PERMISSION_DENIED -> R.string.map_permission_denied
        MapProblem.ADDRESS_NOT_FOUND -> R.string.map_address_not_found
    }
    Text(
        stringResource(message),
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** Tarjeta de la gasolinera más barata de la zona; da la misma información que el marcador destacado. */
@Composable
private fun CheapestCard(
    station: Station,
    state: MapUiState,
    onStationClick: (String) -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.map_cheapest),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        StationCard(station, { onStationClick(station.id) }, Modifier.padding(top = 4.dp), state.fuel)
    }
}
