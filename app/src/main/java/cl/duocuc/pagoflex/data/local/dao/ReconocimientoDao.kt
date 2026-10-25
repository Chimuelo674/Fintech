package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.ReconocimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReconocimientoDao {
    @Query("SELECT * FROM usuario_reconocimiento WHERE idUsuarioFinal = :idUsuario ORDER BY fechaObtencion DESC")
    fun getReconocimientosByUsuario(idUsuario: String): Flow<List<ReconocimientoEntity>>

    @Query("SELECT * FROM usuario_reconocimiento WHERE idUsuarioFinal = :idUsuario")
    suspend fun getReconocimientosByUsuarioOnce(idUsuario: String): List<ReconocimientoEntity>

    @Insert
    suspend fun insertReconocimiento(reconocimiento: ReconocimientoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconocimientos(reconocimientos: List<ReconocimientoEntity>)
}
