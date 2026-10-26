package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @PrimaryKey val idCompromiso: String,
    val folio: String,
    val idUsuarioFinal: String,
    val idEmpresa: String,
    val concepto: String,
    val numeroCuota: Int? = null,
    val totalCuotas: Int? = null,
    val monto: Double,
    val fechaVencimiento: String,
    val estado: String,
    val recargoAplicado: Double = 0.0,
    val registradoPor: String = "PagoFlex",
    val fechaRegistro: String = "2026-10-01",
    val motivoAnulacion: String? = null,
    val comentarioAnulacion: String? = null,
    val ejecutivoAnulacion: String? = null,
    val fechaAnulacion: String? = null
)
