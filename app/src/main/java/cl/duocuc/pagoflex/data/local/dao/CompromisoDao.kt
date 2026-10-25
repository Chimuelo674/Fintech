package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompromisoDao {
    @Query("SELECT * FROM compromiso WHERE idUsuarioFinal = :idUsuario ORDER BY fechaVencimiento ASC")
    fun getCompromisosByUsuario(idUsuario: String): Flow<List<CompromisoEntity>>

    @Query("SELECT * FROM compromiso ORDER BY fechaVencimiento ASC")
    fun getAllCompromisos(): Flow<List<CompromisoEntity>>

    /** Último folio con el prefijo dado (ej. 'CP-2026-'), para el correlativo. */
    @Query("SELECT MAX(folio) FROM compromiso WHERE folio LIKE :prefijo || '%'")
    suspend fun getMaxFolio(prefijo: String): String?

    @Query("SELECT * FROM compromiso")
    suspend fun getAllCompromisosOnce(): List<CompromisoEntity>

    /** Lectura puntual (no Flow), apta para usarse dentro de una transacción. */
    @Query("SELECT * FROM compromiso WHERE idUsuarioFinal = :idUsuario ORDER BY fechaVencimiento ASC")
    suspend fun getCompromisosByUsuarioOnce(idUsuario: String): List<CompromisoEntity>

    @Query("SELECT * FROM compromiso WHERE idEmpresa = :idEmpresa ORDER BY fechaVencimiento ASC")
    fun getCompromisosByEmpresa(idEmpresa: String): Flow<List<CompromisoEntity>>

    @Query("SELECT * FROM compromiso WHERE idCompromiso = :idCompromiso")
    suspend fun getCompromisoById(idCompromiso: String): CompromisoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompromisos(compromisos: List<CompromisoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompromiso(compromiso: CompromisoEntity)

    @Update
    suspend fun updateCompromiso(compromiso: CompromisoEntity)
}
