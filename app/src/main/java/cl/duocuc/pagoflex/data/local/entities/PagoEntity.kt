package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pago")
data class PagoEntity(
    @PrimaryKey val idPago: String,        // e.g. "CPR-000451"
    val numeroComprobante: String,
    val idCompromiso: String,
    val idUsuarioFinal: String,
    val idEmpresa: String,
    val fechaHora: String,                 // "YYYY-MM-DD HH:MM"
    val montoPagado: Double,
    val recargoPagado: Double = 0.0,
    val canal: String = "App",             // "App", "Portal de pagos", "Agente conversacional"
    val medioPago: String = "Tarjeta de débito",
    val aTiempo: Boolean = true
)
