package com.peter1323.litrolow.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.peter1323.litrolow.LitroLowApplication
import com.peter1323.litrolow.R
import com.peter1323.litrolow.ui.alerts.AlertsScreen
import com.peter1323.litrolow.ui.alerts.AlertsViewModel
import com.peter1323.litrolow.ui.detail.DetailScreen
import com.peter1323.litrolow.ui.detail.DetailViewModel
import com.peter1323.litrolow.ui.favorites.FavoritesScreen
import com.peter1323.litrolow.ui.favorites.FavoritesViewModel
import com.peter1323.litrolow.ui.map.MapScreen
import com.peter1323.litrolow.ui.map.MapViewModel
import com.peter1323.litrolow.ui.stations.StationsScreen
import com.peter1323.litrolow.ui.stations.StationsViewModel

/** Pestañas de la barra inferior, con su ruta de navegación. */
private enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    STATIONS("stations", R.string.tab_stations, Icons.AutoMirrored.Filled.List),
    MAP("map", R.string.tab_map, Icons.Default.Place),
    FAVORITES("favorites", R.string.tab_favorites, Icons.Default.Favorite),
    ALERTS("alerts", R.string.tab_alerts, Icons.Default.Notifications),
}

private const val DETAIL_ARG = "stationId"

/** Raíz de la interfaz: barra superior, barra inferior y navegación entre pantallas. */
@Composable
fun LitroLowRoot(isDark: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isTab = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        topBar = { TopBar(isTab, isDark, onToggleTheme, navController::popBackStack) },
        bottomBar = { if (isTab) BottomBar(currentRoute, navController) },
    ) { padding ->
        AppNavHost(navController, isDark, Modifier.padding(padding))
    }
}

/** Barra superior con el nombre de la app, la flecha de volver y el cambio de tema. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(isTab: Boolean, isDark: Boolean, onToggleTheme: () -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        navigationIcon = {
            if (!isTab) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    painterResource(if (isDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                    stringResource(if (isDark) R.string.theme_to_light else R.string.theme_to_dark),
                )
            }
        },
    )
}

/** Barra inferior; al cambiar de pestaña conserva el estado de la anterior. */
@Composable
private fun BottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == currentRoute,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/** Declara todas las pantallas y crea cada ViewModel con sus dependencias. */
@Composable
private fun AppNavHost(navController: NavHostController, isDark: Boolean, modifier: Modifier) {
    val app = LocalContext.current.applicationContext as LitroLowApplication
    val openDetail: (String) -> Unit = { navController.navigate("detail/$it") }
    NavHost(navController, startDestination = Tab.STATIONS.route, modifier = modifier) {
        composable(Tab.STATIONS.route) {
            StationsScreen(viewModel { StationsViewModel(app.stationRepository, app.alertRepository) }, openDetail)
        }
        composable(Tab.MAP.route) {
            val mapViewModel = viewModel {
                MapViewModel(
                    app.stationRepository.stations,
                    app.locationRepository::currentLocation,
                    app.locationRepository::geocode,
                )
            }
            MapScreen(mapViewModel, isDark, openDetail)
        }
        composable(Tab.FAVORITES.route) {
            FavoritesScreen(viewModel { FavoritesViewModel(app.stationRepository) }, openDetail)
        }
        composable(Tab.ALERTS.route) {
            AlertsScreen(viewModel { AlertsViewModel(app.alertRepository) }, openDetail)
        }
        composable("detail/{$DETAIL_ARG}") { entry ->
            val stationId = requireNotNull(entry.arguments?.getString(DETAIL_ARG))
            DetailScreen(viewModel { DetailViewModel(stationId, app.stationRepository, app.alertRepository) })
        }
    }
}
