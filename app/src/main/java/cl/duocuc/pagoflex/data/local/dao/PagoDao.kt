package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {
    @Query(
        "SELECT p.* FROM pago p INNER JOIN compromiso c ON c.idCompromiso = p.idCompromiso " +
            "WHERE c.idUsuarioFinal = :idUsuario ORDER BY p.fechaHora DESC"
    )
    fun getPagosByUsuario(idUsuario: String): Flow<List<PagoEntity>>

    @Query(
        "SELECT p.* FROM pago p INNER JOIN compromiso c ON c.idCompromiso = p.idCompromiso " +
            "WHERE c.idUsuarioFinal = :idUsuario"
    )
    suspend fun getPagosByUsuarioOnce(idUsuario: String): List<PagoEntity>

    @Query(
        "SELECT p.* FROM pago p INNER JOIN compromiso c ON c.idCompromiso = p.idCompromiso " +
            "WHERE c.idEmpresa = :idEmpresa"
    )
    fun getPagosByEmpresa(idEmpresa: String): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pago WHERE idCompromiso = :idCompromiso LIMIT 1")
    suspend fun getPagoByCompromiso(idCompromiso: String): PagoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagos(pagos: List<PagoEntity>)

    @Query("SELECT * FROM pago WHERE numeroComprobante = :numero LIMIT 1")
    suspend fun getPagoByNumero(numero: String): PagoEntity?

    @Query("SELECT MAX(numeroComprobante) FROM pago WHERE numeroComprobante LIKE 'CPR-%'")
    suspend fun getMaxNumeroComprobante(): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPago(pago: PagoEntity)
}
