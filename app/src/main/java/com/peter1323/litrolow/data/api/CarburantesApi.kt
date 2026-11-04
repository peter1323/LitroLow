package com.peter1323.litrolow.data.api

import com.peter1323.litrolow.data.model.FuelPrices
import com.peter1323.litrolow.data.model.Station
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** Endpoints del Ministerio que usa la app. */
interface CarburantesApi {

    /** Descarga todas las gasolineras de España (unos 12 MB). */
    @GET("EstacionesTerrestres/")
    suspend fun getAllStations(): StationsResponse

    /** Descarga una sola provincia (unos 200 KB); las alertas la usan para no gastar datos móviles. */
    @GET("EstacionesTerrestres/FiltroProvincia/{provinceId}")
    suspend fun getProvinceStations(@Path("provinceId") provinceId: String): StationsResponse
}

/** Respuesta de la API: resultado de la consulta y lista de gasolineras. */
@Serializable
data class StationsResponse(
    @SerialName("ResultadoConsulta") val result: String = "",
    @SerialName("ListaEESSPrecio") val stations: List<StationDto> = emptyList(),
)

/** Gasolinera tal como llega de la API, con todos los números en texto. */
@Serializable
data class StationDto(
    @SerialName("IDEESS") val id: String,
    @SerialName("Rótulo") val brand: String = "",
    @SerialName("Dirección") val address: String = "",
    @SerialName("Localidad") val locality: String = "",
    @SerialName("Provincia") val province: String = "",
    @SerialName("IDProvincia") val provinceId: String = "",
    @SerialName("Horario") val schedule: String = "",
    @SerialName("Latitud") val latitude: String = "",
    @SerialName("Longitud (WGS84)") val longitude: String = "",
    @SerialName("Precio Gasolina 95 E5") val price95: String = "",
    @SerialName("Precio Gasolina 98 E5") val price98: String = "",
    @SerialName("Precio Gasoleo A") val priceDiesel: String = "",
    @SerialName("Precio Gasoleo Premium") val priceDieselPremium: String = "",
) {
    /**
     * Pasa la gasolinera al modelo de la app, normalizando la marca para que "Repsol" y "REPSOL"
     * sean la misma. Devuelve null si no trae coordenadas, porque no se podría situar en el mapa.
     */
    fun toStation(updatedAt: Long): Station? {
        return Station(
            id = id,
            brand = brand.trim().uppercase(),
            address = address.trim(),
            locality = locality.trim(),
            province = province.trim(),
            provinceId = provinceId,
            schedule = schedule.trim(),
            latitude = parseDecimal(latitude) ?: return null,
            longitude = parseDecimal(longitude) ?: return null,
            prices = FuelPrices(
                parsePrice(price95),
                parsePrice(price98),
                parsePrice(priceDiesel),
                parsePrice(priceDieselPremium),
            ),
            updatedAt = updatedAt,
        )
    }
}

private const val BASE_URL =
    "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/"

/** Crea el cliente de la API, con tiempos de espera amplios porque la respuesta completa es muy grande. */
fun createCarburantesApi(): CarburantesApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CarburantesApi::class.java)
}
