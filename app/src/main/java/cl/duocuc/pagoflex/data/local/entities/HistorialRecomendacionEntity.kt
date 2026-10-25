package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cada cambio de estado de una recomendación hecho por un agente (RN-13, RNF-15).
 * Equivale a la tabla historial_recomendacion del MER.
 */
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
    val estado: String,       // estado al que pasó
    val idAgente: String,
    val fecha: String,        // yyyy-MM-dd
    val motivo: String? = null // obligatorio si pasa a Descartada
)
