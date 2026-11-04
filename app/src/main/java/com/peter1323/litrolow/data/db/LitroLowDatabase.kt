package com.peter1323.litrolow.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import com.peter1323.litrolow.data.model.Favorite
import com.peter1323.litrolow.data.model.PriceAlert
import com.peter1323.litrolow.data.model.PricePoint
import com.peter1323.litrolow.data.model.Station
import kotlinx.coroutines.flow.Flow

/** Consultas de gasolineras, historial y favoritas, que siempre se usan juntas. */
@Dao
interface StationDao {

    /** Observa todas las gasolineras; la lista se actualiza sola cuando cambian los datos. */
    @Query("SELECT * FROM stations")
    fun observeAll(): Flow<List<Station>>

    /** Observa una gasolinera concreta para la pantalla de detalle. */
    @Query("SELECT * FROM stations WHERE id = :id")
    fun observeById(id: String): Flow<Station?>

    /** Lee todas las gasolineras una vez, para compararlas con los datos nuevos. */
    @Query("SELECT * FROM stations")
    suspend fun getAll(): List<Station>

    /** Lee las gasolineras de una provincia, para compararlas con los datos nuevos. */
    @Query("SELECT * FROM stations WHERE provinceId = :provinceId")
    suspend fun getByProvince(provinceId: String): List<Station>

    /** Lee una gasolinera una vez; la usan las alertas. */
    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun getById(id: String): Station?

    /** Devuelve la fecha de la actualización más antigua, o null si aún no hay datos. */
    @Query("SELECT MIN(updatedAt) FROM stations")
    suspend fun oldestUpdate(): Long?

    /** Inserta o actualiza gasolineras. */
    @Upsert
    suspend fun upsertStations(stations: List<Station>)

    /** Borra las gasolineras que la última descarga completa ya no incluye. */
    @Query("DELETE FROM stations WHERE updatedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /** Añade puntos al historial de precios. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(points: List<PricePoint>)

    /** Borra el historial antiguo para que la base de datos no crezca sin límite. */
    @Query("DELETE FROM price_history WHERE recordedAt < :timestamp")
    suspend fun deleteHistoryOlderThan(timestamp: Long)

    /** Guarda gasolineras e historial a la vez: o se guarda todo o nada, para no dejar datos a medias. */
    @Transaction
    suspend fun saveSnapshot(stations: List<Station>, history: List<PricePoint>) {
        upsertStations(stations)
        insertHistory(history)
    }

    /** Observa el historial de una gasolinera, del punto más antiguo al más reciente. */
    @Query("SELECT * FROM price_history WHERE stationId = :stationId ORDER BY recordedAt")
    fun observeHistory(stationId: String): Flow<List<PricePoint>>

    /** Observa las gasolineras marcadas como favoritas. */
    @Query("SELECT stations.* FROM stations JOIN favorites ON stations.id = favorites.stationId")
    fun observeFavorites(): Flow<List<Station>>

    /** Observa si una gasolinera es favorita. */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationId = :stationId)")
    fun observeIsFavorite(stationId: String): Flow<Boolean>

    /** Marca una gasolinera como favorita. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    /** Quita una gasolinera de favoritas. */
    @Query("DELETE FROM favorites WHERE stationId = :stationId")
    suspend fun deleteFavorite(stationId: String)
}

/** Consultas de las alertas de precio. */
@Dao
interface AlertDao {

    /** Observa las alertas para mostrarlas en pantalla. */
    @Query("SELECT * FROM price_alerts ORDER BY stationName")
    fun observeAll(): Flow<List<PriceAlert>>

    /** Lee las alertas una vez; la usa la comprobación periódica. */
    @Query("SELECT * FROM price_alerts")
    suspend fun getAll(): List<PriceAlert>

    /** Crea o actualiza una alerta. */
    @Upsert
    suspend fun upsert(alert: PriceAlert)

    /** Borra una alerta. */
    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Base de datos local de la app. */
@Database(
    entities = [Station::class, PricePoint::class, Favorite::class, PriceAlert::class],
    version = 1,
    exportSchema = false,
)
abstract class LitroLowDatabase : RoomDatabase() {

    /** Da acceso a gasolineras, historial y favoritas. */
    abstract fun stationDao(): StationDao

    /** Da acceso a las alertas. */
    abstract fun alertDao(): AlertDao

    companion object {
        /** Crea la base de datos en el almacenamiento privado de la app. */
        fun create(context: Context): LitroLowDatabase =
            Room.databaseBuilder(context, LitroLowDatabase::class.java, "litrolow.db").build()
    }
}
