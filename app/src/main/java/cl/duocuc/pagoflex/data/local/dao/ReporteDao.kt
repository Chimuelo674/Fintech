package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    // reporte → compromiso → usuario (MER 2.3)
    @Query(
        "SELECT r.* FROM reporte r INNER JOIN compromiso c ON c.idCompromiso = r.idCompromiso " +
            "WHERE c.idUsuarioFinal = :idUsuario ORDER BY r.fechaCreacion DESC"
    )
    fun getReportesByUsuario(idUsuario: String): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reporte ORDER BY fechaCreacion DESC")
    fun getAllReportes(): Flow<List<ReporteEntity>>

    @Query(
        "SELECT r.* FROM reporte r INNER JOIN compromiso c ON c.idCompromiso = r.idCompromiso " +
            "WHERE c.idUsuarioFinal = :idUsuario"
    )
    suspend fun getReportesByUsuarioOnce(idUsuario: String): List<ReporteEntity>

    @Query("SELECT * FROM reporte WHERE idCompromiso = :idCompromiso")
    suspend fun getReportesByCompromisoOnce(idCompromiso: String): List<ReporteEntity>

    @Query("SELECT MAX(idReporte) FROM reporte WHERE idReporte LIKE 'REP-%'")
    suspend fun getMaxIdReporte(): String?

    @Query("SELECT * FROM reporte WHERE idReporte = :id")
    suspend fun getReporteById(id: String): ReporteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportes(reportes: List<ReporteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReporte(reporte: ReporteEntity)

    @Update
    suspend fun updateReporte(reporte: ReporteEntity)
}
