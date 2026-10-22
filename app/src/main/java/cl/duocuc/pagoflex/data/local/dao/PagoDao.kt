package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {
    @Query("SELECT * FROM pago WHERE idUsuarioFinal = :idUsuario ORDER BY fechaHora DESC")
    fun getPagosByUsuario(idUsuario: String): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pago WHERE idCompromiso = :idCompromiso LIMIT 1")
    suspend fun getPagoByCompromiso(idCompromiso: String): PagoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagos(pagos: List<PagoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPago(pago: PagoEntity)
}
