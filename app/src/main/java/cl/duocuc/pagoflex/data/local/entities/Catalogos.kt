package cl.duocuc.pagoflex.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Catálogos (MER 2.2): estados, motivos, canales, medios de pago, rubros, comunas, áreas,
 * roles y reconocimientos viven en tablas propias, y las demás tablas los referencian con
 * claves foráneas. Así no puede guardarse un valor inconsistente ("pendiente", "Pendiente ",
 * "PENDIENTE"…) y se pueden agregar valores sin cambiar la estructura.
 *
 * Decisión de diseño: la clave de cada catálogo es su nombre (clave natural) en vez de un
 * id numérico. Sigue en 3FN (el nombre identifica la fila y no hay dependencias transitivas)
 * y evita traducir id <-> nombre en cada pantalla y regla de negocio.
 */

/** Roles de la app (R-01 a R-03). */
@Entity(tableName = "rol")
data class RolEntity(
    @PrimaryKey val codigo: String,   // USUARIO_FINAL, AGENTE, EJECUTIVO
    val nombre: String,
    val descripcion: String
)

/** RN-01: Pendiente, Vencido, Pagado, En revisión, Anulado. */
@Entity(tableName = "estado_compromiso")
data class EstadoCompromisoEntity(@PrimaryKey val nombre: String)

/** RN-09: Abierto, En gestión, Resuelto. */
@Entity(tableName = "estado_reporte")
data class EstadoReporteEntity(@PrimaryKey val nombre: String)

/** RN-13: Recibida → Contactada → En evaluación → Se unió / Descartada. [orden] evita saltarse etapas. */
@Entity(tableName = "estado_recomendacion")
data class EstadoRecomendacionEntity(@PrimaryKey val nombre: String, val orden: Int)

/** RN-09: motivos de un reporte de problema. */
@Entity(tableName = "motivo_reporte")
data class MotivoReporteEntity(@PrimaryKey val nombre: String)

/** RN-21: motivos de anulación. */
@Entity(tableName = "motivo_anulacion")
data class MotivoAnulacionEntity(@PrimaryKey val nombre: String)

/** RN-07: App, Portal de pagos, Agente conversacional. */
@Entity(tableName = "canal")
data class CanalEntity(@PrimaryKey val nombre: String)

/** Aclaraciones 1 y 6: solo el nombre del medio simulado, nunca datos bancarios (RN-19). */
@Entity(tableName = "medio_pago")
data class MedioPagoEntity(@PrimaryKey val nombre: String)

/** Rubros de empresas clientes y de organizaciones recomendadas. */
@Entity(tableName = "rubro")
data class RubroEntity(@PrimaryKey val nombre: String)

/** Comunas de usuarios finales y de organizaciones recomendadas. */
@Entity(tableName = "comuna")
data class ComunaEntity(@PrimaryKey val nombre: String)

/** Áreas de PagoFlex: Soporte, Comercial. */
@Entity(tableName = "area")
data class AreaEntity(@PrimaryKey val nombre: String)

/** Catálogo de reconocimientos simbólicos (RN-16). */
@Entity(tableName = "reconocimiento")
data class ReconocimientoCatalogoEntity(
    @PrimaryKey val nombre: String,
    val descripcion: String
)

/** Valores fijos de los catálogos, tal como los definen los anexos. */
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
