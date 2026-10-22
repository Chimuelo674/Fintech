package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecomendacionDao {
    @Query("SELECT * FROM recomendacion WHERE idUsuarioFinal = :idUsuario ORDER BY fecha DESC")
    fun getRecomendacionesByUsuario(idUsuario: String): Flow<List<RecomendacionEntity>>

    @Query("SELECT * FROM recomendacion ORDER BY fecha DESC")
    fun getAllRecomendaciones(): Flow<List<RecomendacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecomendaciones(recomendaciones: List<RecomendacionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecomendacion(recomendacion: RecomendacionEntity)

    @Update
    suspend fun updateRecomendacion(recomendacion: RecomendacionEntity)
}
