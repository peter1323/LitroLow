package com.peter1323.litrolow.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.peter1323.litrolow.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Con menos de dos puntos no hay línea que dibujar.
private const val MIN_POINTS_FOR_CHART = 2

/** Gráfica de la evolución del precio, o una explicación si todavía no hay datos suficientes. */
@Composable
fun PriceHistoryChart(points: List<ChartPoint>) {
    if (points.size < MIN_POINTS_FOR_CHART) {
        Text(
            stringResource(R.string.history_not_enough),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        producer.runTransaction {
            lineModel { series(points.indices.toList(), points.map { it.price }) }
        }
    }
    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(valueFormatter = priceFormatter()),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dateFormatter(points)),
        ),
        producer,
        Modifier.fillMaxWidth().height(220.dp),
    )
}

/** Formatea el eje vertical con los tres decimales de los precios. */
@Composable
private fun priceFormatter(): CartesianValueFormatter =
    remember { CartesianValueFormatter.decimal(decimalCount = 3) }

/**
 * Formatea el eje horizontal con la fecha de cada punto. Los puntos se dibujan por posición y no
 * por fecha porque los registros llegan a intervalos irregulares, según cuándo se abra la app.
 */
@Composable
private fun dateFormatter(points: List<ChartPoint>): CartesianValueFormatter = remember(points) {
    val format = SimpleDateFormat("dd/MM", Locale.forLanguageTag("es-ES"))
    CartesianValueFormatter { _, x, _ ->
        points.getOrNull(x.toInt())?.let { format.format(Date(it.recordedAt)) } ?: " "
    }
}
