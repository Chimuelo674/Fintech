package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historial_recomendacion",
    indices = [Index("idRecomendacion"), Index("estado")],
    foreignKeys = [
        ForeignKey(entity = RecomendacionEntity::class, parentColumns = ["idRecomendacion"], childColumns = ["idRecomendacion"]),
        ForeignKey(entity = EstadoRecomendacionEntity::class, parentColumns = ["nombre"], childColumns = ["estado"])
    ]
)
data class HistorialRecomendacionEntity(
    @PrimaryKey(autoGenerate = true) val idHistorial: Int = 0,
    val idRecomendacion: String,
    val estado: String,
    val idAgente: String,
    val fecha: String,
    val motivo: String? = null
)
