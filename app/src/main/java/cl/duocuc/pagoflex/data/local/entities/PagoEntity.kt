package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pago confirmado de un compromiso; equivale al comprobante (RN-05). Igual que en el MER:
 * - un pago por compromiso (RN-06, FK única) y número de comprobante único;
 * - la persona y la empresa NO se repiten aquí: se obtienen vía compromiso (3FN);
 * - "¿a tiempo?" y el recargo NO se guardan: se calculan (fecha de pago ≤ vencimiento;
 *   recargo = monto pagado − monto del compromiso). Ver MER 2.3.
 */
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
    @PrimaryKey val idPago: String,        // e.g. "CPR-000451"
    val numeroComprobante: String,
    val idCompromiso: String,
    val fechaHora: String,                 // "yyyy-MM-dd HH:mm"
    val montoPagado: Double,               // monto + recargo al momento del pago (hecho histórico, RN-05)
    val canal: String = "App",             // "App", "Portal de pagos", "Agente conversacional" (RN-07)
    val medioPago: String = "Pago simulado" // solo el nombre del medio (Aclaraciones 1 y 6, RN-19)
)
