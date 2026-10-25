package cl.duocuc.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duocuc.pagoflex.data.local.entities.HistorialRecomendacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialRecomendacionDao {
    @Query("SELECT * FROM historial_recomendacion ORDER BY fecha DESC, idHistorial DESC")
    fun getAll(): Flow<List<HistorialRecomendacionEntity>>

    @Insert
    suspend fun insert(item: HistorialRecomendacionEntity)

    @Insert
    suspend fun insertAll(items: List<HistorialRecomendacionEntity>)
}
