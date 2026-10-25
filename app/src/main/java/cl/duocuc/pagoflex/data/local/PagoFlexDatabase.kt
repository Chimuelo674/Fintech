package cl.duocuc.pagoflex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duocuc.pagoflex.data.local.dao.*
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.data.seed.InitialDataSeeder
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UsuarioEntity::class,
        EmpresaEntity::class,
        CompromisoEntity::class,
        PagoEntity::class,
        ReporteEntity::class,
        RecomendacionEntity::class,
        ReconocimientoEntity::class,
        HistorialRecomendacionEntity::class,
        // Catálogos (MER 2.2)
        RolEntity::class,
        EstadoCompromisoEntity::class,
        EstadoReporteEntity::class,
        EstadoRecomendacionEntity::class,
        MotivoReporteEntity::class,
        MotivoAnulacionEntity::class,
        CanalEntity::class,
        MedioPagoEntity::class,
        RubroEntity::class,
        ComunaEntity::class,
        AreaEntity::class,
        ReconocimientoCatalogoEntity::class
    ],
    // v2: historial de 12 meses. v3: historial de recomendaciones. v4: índices, UNIQUE y FK del MER.
    // v6: catálogos con claves foráneas.
    // v5: pago y reporte sin columnas redundantes (persona/empresa vía compromiso; "a tiempo" y recargo se calculan).
    // Al subir la versión, la base anterior se borra y se vuelve a cargar (no hay datos reales que conservar).
    version = 6,
    exportSchema = false
)
abstract class PagoFlexDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun compromisoDao(): CompromisoDao
    abstract fun pagoDao(): PagoDao
    abstract fun reporteDao(): ReporteDao
    abstract fun recomendacionDao(): RecomendacionDao
    abstract fun reconocimientoDao(): ReconocimientoDao
    abstract fun historialRecomendacionDao(): HistorialRecomendacionDao
    abstract fun catalogoDao(): CatalogoDao

    companion object {
        @Volatile
        private var INSTANCE: PagoFlexDatabase? = null

        fun getDatabase(context: Context): PagoFlexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PagoFlexDatabase::class.java,
                    "pagoflex_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed database asynchronously on creation
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                InitialDataSeeder.seedDatabase(database)
                                PagoFlexRepository(database).sincronizarEstados()
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                // También cubre la recarga tras una migración destructiva,
                                // donde Room llama a onOpen pero no a onCreate.
                                InitialDataSeeder.seedDatabase(database)
                                PagoFlexRepository(database).sincronizarEstados()
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
