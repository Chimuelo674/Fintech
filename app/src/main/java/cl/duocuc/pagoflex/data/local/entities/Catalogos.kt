package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rol")
data class RolEntity(
    @PrimaryKey val codigo: String,
    val nombre: String,
    val descripcion: String
)

@Entity(tableName = "estado_compromiso")
data class EstadoCompromisoEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "estado_reporte")
data class EstadoReporteEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "estado_recomendacion")
data class EstadoRecomendacionEntity(@PrimaryKey val nombre: String, val orden: Int)

@Entity(tableName = "motivo_reporte")
data class MotivoReporteEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "motivo_anulacion")
data class MotivoAnulacionEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "canal")
data class CanalEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "medio_pago")
data class MedioPagoEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "rubro")
data class RubroEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "comuna")
data class ComunaEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "area")
data class AreaEntity(@PrimaryKey val nombre: String)

@Entity(tableName = "reconocimiento")
data class ReconocimientoCatalogoEntity(
    @PrimaryKey val nombre: String,
    val descripcion: String
)

object ValoresCatalogo {
    val ROLES = listOf(
        RolEntity("USUARIO_FINAL", "Usuario final", "Ve su situación, paga, revisa comprobantes, reporta problemas y usa las funciones de hábito."),
        RolEntity("AGENTE", "Agente PagoFlex", "Gestiona reportes de problema y recomendaciones."),
        RolEntity("EJECUTIVO", "Ejecutivo de empresa cliente", "Registra y anula compromisos de su empresa y sigue su cobranza.")
    )
    val ESTADOS_COMPROMISO = listOf("Pendiente", "Vencido", "Pagado", "En revisión", "Anulado")
    val ESTADOS_REPORTE = listOf("Abierto", "En gestión", "Resuelto")
    val ESTADOS_RECOMENDACION = listOf("Recibida" to 1, "Contactada" to 2, "En evaluación" to 3, "Se unió" to 4, "Descartada" to 5)
    val MOTIVOS_REPORTE = listOf("Ya pagué por otro medio", "No reconozco este cobro", "El monto no corresponde", "Otro")
    val MOTIVOS_ANULACION = listOf("Repactación", "Error de registro", "Pagado directamente a la empresa", "Otro")
    val CANALES = listOf("App", "Portal de pagos", "Agente conversacional")
    val MEDIOS_PAGO = listOf("Tarjeta de débito", "Tarjeta de crédito", "Transferencia", "Pago simulado")
    val AREAS = listOf("Soporte", "Comercial")
    val RECONOCIMIENTOS = listOf(
        ReconocimientoCatalogoEntity("Pago Puntual", "Pagaste a tiempo más del 90% de tus compromisos del último año."),
        ReconocimientoCatalogoEntity("Cliente Ejemplar", "Pagaste a tiempo el 95% o más de tus compromisos del último año."),
        ReconocimientoCatalogoEntity("Embajador PagoFlex", "Recomendaste una organización que se unió a PagoFlex.")
    )
}
