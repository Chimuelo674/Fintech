package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reporte WHERE idUsuarioFinal = :idUsuario ORDER BY fechaCreacion DESC")
    fun getReportesByUsuario(idUsuario: String): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reporte ORDER BY fechaCreacion DESC")
    fun getAllReportes(): Flow<List<ReporteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportes(reportes: List<ReporteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReporte(reporte: ReporteEntity)

    @Update
    suspend fun updateReporte(reporte: ReporteEntity)
}
