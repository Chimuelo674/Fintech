package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reporte")
data class ReporteEntity(
    @PrimaryKey val idReporte: String,     // e.g. "REP-0011"
    val idCompromiso: String,
    val idUsuarioFinal: String,
    val motivo: String,                    // "Ya pagué por otro medio", "El monto no corresponde", "No reconozco este cobro", "Otro"
    val comentario: String? = null,
    val fechaCreacion: String,             // "YYYY-MM-DD"
    val estado: String = "Abierto",        // "Abierto", "En gestión", "Resuelto"
    val idAgente: String? = null,
    val fechaToma: String? = null,
    val respuesta: String? = null,
    val fechaResolucion: String? = null
)
