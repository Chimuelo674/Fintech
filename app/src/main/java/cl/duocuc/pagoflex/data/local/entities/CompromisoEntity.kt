package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compromiso")
data class CompromisoEntity(
    @PrimaryKey val idCompromiso: String, // e.g. "CP-2026-0101"
    val folio: String,
    val idUsuarioFinal: String,
    val idEmpresa: String,
    val concepto: String,
    val numeroCuota: Int? = null,
    val totalCuotas: Int? = null,
    val monto: Double,
    val fechaVencimiento: String,        // "YYYY-MM-DD"
    val estado: String,                   // "Pendiente", "Vencido", "Pagado", "En revisión", "Anulado"
    val recargoAplicado: Double = 0.0,
    val registradoPor: String = "PagoFlex",
    val fechaRegistro: String = "2026-10-01",
    val motivoAnulacion: String? = null,
    val comentarioAnulacion: String? = null,
    val ejecutivoAnulacion: String? = null,
    val fechaAnulacion: String? = null
)
