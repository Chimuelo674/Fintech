package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * MER: folio único; FK a la persona y a la empresa. La unicidad de RN-20 (persona, empresa,
 * concepto, cuota) se valida en la app y no con un índice UNIQUE, porque un compromiso
 * Anulado no debe impedir registrar uno nuevo igual (FA-12: "se anula y se registra uno nuevo").
 */
@Entity(
    tableName = "compromiso",
    indices = [
        Index(value = ["folio"], unique = true), Index("idUsuarioFinal"), Index("idEmpresa"),
        Index("fechaVencimiento"), Index("estado"), Index("motivoAnulacion")
    ],
    foreignKeys = [
        ForeignKey(entity = UsuarioEntity::class, parentColumns = ["idUsuario"], childColumns = ["idUsuarioFinal"]),
        ForeignKey(entity = EmpresaEntity::class, parentColumns = ["idEmpresa"], childColumns = ["idEmpresa"]),
        ForeignKey(entity = EstadoCompromisoEntity::class, parentColumns = ["nombre"], childColumns = ["estado"]),
        ForeignKey(entity = MotivoAnulacionEntity::class, parentColumns = ["nombre"], childColumns = ["motivoAnulacion"])
    ]
)
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
