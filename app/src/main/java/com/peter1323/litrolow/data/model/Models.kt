package com.peter1323.litrolow.data.model

import androidx.annotation.StringRes
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.peter1323.litrolow.R

/** Combustibles que muestra la app; cada uno sabe qué texto usar en pantalla. */
enum class FuelType(@param:StringRes val label: Int) {
    GASOLINE_95(R.string.fuel_95),
    GASOLINE_98(R.string.fuel_98),
    DIESEL(R.string.fuel_diesel),
    DIESEL_PREMIUM(R.string.fuel_diesel_premium),
}

/** Precios de los cuatro combustibles; null significa que no se vende ese combustible. */
data class FuelPrices(
    val price95: Double?,
    val price98: Double?,
    val priceDiesel: Double?,
    val priceDieselPremium: Double?,
) {
    /** Devuelve el precio del combustible indicado. */
    fun of(fuel: FuelType): Double? = when (fuel) {
        FuelType.GASOLINE_95 -> price95
        FuelType.GASOLINE_98 -> price98
        FuelType.DIESEL -> priceDiesel
        FuelType.DIESEL_PREMIUM -> priceDieselPremium
    }
}

/**
 * Gasolinera con sus precios. Es también la tabla de Room, para no duplicar la clase
 * y evitar conversiones que no aportarían nada.
 */
@Entity(tableName = "stations", indices = [Index("provinceId")])
data class Station(
    @PrimaryKey val id: String,
    val brand: String,
    val address: String,
    val locality: String,
    val province: String,
    val provinceId: String,
    val schedule: String,
    val latitude: Double,
    val longitude: Double,
    @Embedded val prices: FuelPrices,
    val updatedAt: Long,
)

/** Foto de los precios de una gasolinera en un momento dado; alimenta la gráfica de historial. */
@Entity(tableName = "price_history", primaryKeys = ["stationId", "recordedAt"])
data class PricePoint(
    val stationId: String,
    val recordedAt: Long,
    @Embedded val prices: FuelPrices,
)

/** Marca una gasolinera como favorita; solo guarda el id porque el resto está en [Station]. */
@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val stationId: String)

/**
 * Alerta de precio. Copia el nombre y la provincia de la gasolinera para poder listarla y
 * consultarla aunque esa gasolinera aún no esté en la caché. [notified] evita repetir el aviso
 * mientras el precio siga por debajo del máximo.
 */
@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: String,
    val stationName: String,
    val provinceId: String,
    val fuel: FuelType,
    val maxPrice: Double,
    val notified: Boolean = false,
)

/** Motivos por los que no se han podido actualizar los datos; la interfaz muestra un mensaje para cada uno. */
enum class LoadError { NO_INTERNET, API_FAILURE }

/** Punto geográfico, ya venga de la ubicación del usuario o de una dirección escrita. */
data class GeoPoint(val latitude: Double, val longitude: Double)
