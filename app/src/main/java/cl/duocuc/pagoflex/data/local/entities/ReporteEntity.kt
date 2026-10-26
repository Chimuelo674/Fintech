package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reporte",
    indices = [Index("idCompromiso"), Index("estado"), Index("motivo")],
    foreignKeys = [
        ForeignKey(entity = CompromisoEntity::class, parentColumns = ["idCompromiso"], childColumns = ["idCompromiso"]),
        ForeignKey(entity = MotivoReporteEntity::class, parentColumns = ["nombre"], childColumns = ["motivo"]),
        ForeignKey(entity = EstadoReporteEntity::class, parentColumns = ["nombre"], childColumns = ["estado"])
    ]
)
data class ReporteEntity(
    @PrimaryKey val idReporte: String,
    val idCompromiso: String,
    val motivo: String,
    val comentario: String? = null,
    val fechaCreacion: String,
    val estado: String = "Abierto",
    val idAgente: String? = null,
    val fechaToma: String? = null,
    val respuesta: String? = null,
    val fechaResolucion: String? = null
)
