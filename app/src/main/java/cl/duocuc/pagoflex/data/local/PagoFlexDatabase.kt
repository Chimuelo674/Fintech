package cl.duocuc.pagoflex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duocuc.pagoflex.data.local.dao.*
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.data.seed.InitialDataSeeder
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
        ReconocimientoEntity::class
    ],
    version = 1,
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
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed database asynchronously on creation
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                InitialDataSeeder.seedDatabase(database)
                            }
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                InitialDataSeeder.seedDatabase(database)
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
