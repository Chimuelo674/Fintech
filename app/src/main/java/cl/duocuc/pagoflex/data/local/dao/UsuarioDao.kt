package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE idUsuario = :id")
    suspend fun getUsuarioById(id: String): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE idRol = :rol")
    fun getUsuariosByRol(rol: String): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuario")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuarios(usuarios: List<UsuarioEntity>)

    @Update
    suspend fun updateUsuario(usuario: UsuarioEntity)
}
