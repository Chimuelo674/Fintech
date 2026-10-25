package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Problema reportado por el usuario sobre un compromiso suyo. Como en el MER, la persona NO se
 * guarda aquí: se obtiene vía compromiso (reporte → compromiso → usuario), sin redundancia.
 */
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
    @PrimaryKey val idReporte: String,     // e.g. "REP-0011"
    val idCompromiso: String,
    val motivo: String,                    // "Ya pagué por otro medio", "El monto no corresponde", "No reconozco este cobro", "Otro"
    val comentario: String? = null,
    val fechaCreacion: String,             // "YYYY-MM-DD"
    val estado: String = "Abierto",        // "Abierto", "En gestión", "Resuelto"
    val idAgente: String? = null,
    val fechaToma: String? = null,
    val respuesta: String? = null,
    val fechaResolucion: String? = null
)
