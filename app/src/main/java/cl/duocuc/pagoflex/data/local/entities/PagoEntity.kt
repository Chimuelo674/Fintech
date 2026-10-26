package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pago",
    indices = [
        Index(value = ["idCompromiso"], unique = true),
        Index(value = ["numeroComprobante"], unique = true),
        Index("canal"), Index("medioPago")
    ],
    foreignKeys = [
        ForeignKey(entity = CompromisoEntity::class, parentColumns = ["idCompromiso"], childColumns = ["idCompromiso"]),
        ForeignKey(entity = CanalEntity::class, parentColumns = ["nombre"], childColumns = ["canal"]),
        ForeignKey(entity = MedioPagoEntity::class, parentColumns = ["nombre"], childColumns = ["medioPago"])
    ]
)
data class PagoEntity(
    @PrimaryKey val idPago: String,
    val numeroComprobante: String,
    val idCompromiso: String,
    val fechaHora: String,
    val montoPagado: Double,
    val canal: String = "App",
    val medioPago: String = "Pago simulado"
)
