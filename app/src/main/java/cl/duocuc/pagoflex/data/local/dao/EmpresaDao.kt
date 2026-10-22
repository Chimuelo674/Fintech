package cl.duocuc.pagoflex.data.local.dao

import androidx.room.*
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {
    @Query("SELECT * FROM empresa_cliente WHERE idEmpresa = :id")
    suspend fun getEmpresaById(id: String): EmpresaEntity?

    @Query("SELECT * FROM empresa_cliente")
    fun getAllEmpresas(): Flow<List<EmpresaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmpresas(empresas: List<EmpresaEntity>)
}
