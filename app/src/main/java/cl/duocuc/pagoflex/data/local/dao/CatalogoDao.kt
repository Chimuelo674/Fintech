package cl.duocuc.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duocuc.pagoflex.data.local.entities.*
import kotlinx.coroutines.flow.Flow

/** Lectura y carga de catálogos. IGNORE: agregar un valor que ya existe no hace nada. */
@Dao
interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRoles(items: List<RolEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEstadosCompromiso(items: List<EstadoCompromisoEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEstadosReporte(items: List<EstadoReporteEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEstadosRecomendacion(items: List<EstadoRecomendacionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMotivosReporte(items: List<MotivoReporteEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMotivosAnulacion(items: List<MotivoAnulacionEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCanales(items: List<CanalEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMediosPago(items: List<MedioPagoEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRubros(items: List<RubroEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertComunas(items: List<ComunaEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAreas(items: List<AreaEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertReconocimientos(items: List<ReconocimientoCatalogoEntity>)

    @Query("SELECT * FROM reconocimiento")
    fun getReconocimientos(): Flow<List<ReconocimientoCatalogoEntity>>

    @Query("SELECT nombre FROM rubro ORDER BY nombre")
    fun getRubros(): Flow<List<String>>

    @Query("SELECT nombre FROM comuna ORDER BY nombre")
    fun getComunas(): Flow<List<String>>
}
