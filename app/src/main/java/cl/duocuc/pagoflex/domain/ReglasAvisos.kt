package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj

/**
 * Avisos de vencimiento personalizados (RF-11, RN-15, Aclaración nº 3), como funciones puras.
 *
 * Qué se avisa, en orden de importancia para el negocio (Aclaración 3):
 *  - ALTA: hoy vence un compromiso (último día sin recargo).
 *  - ALTA: un compromiso vence en N días, con N = la anticipación que eligió la persona (1, 3 o 5).
 *  - MEDIA: un compromiso quedó vencido (solo el primer día, para no saturar).
 * Reglas RN-15: como máximo un aviso por compromiso al día; nunca entre las 22:00 y las 08:00;
 * nunca de compromisos Pagados, Anulados o En revisión; nada si la persona los desactivó.
 * Tono positivo, sin culpar ni amenazar (RN-11, RNF-04).
 */
object ReglasAvisos {

    val OPCIONES_ANTICIPACION = listOf(1, 3, 5)
    const val HORA_INICIO = 8   // desde las 08:00
    const val HORA_FIN = 22     // hasta antes de las 22:00

    /** En orden de prioridad (Aclaración 3): alta, alta, media, media, media. */
    enum class Tipo { VENCE_HOY, PROXIMO, VENCIDO, PAGO_REGISTRADO, REPORTE_RESUELTO }

    data class Aviso(
        val idCompromiso: String,
        val tipo: Tipo,
        val titulo: String,
        val mensaje: String,
        /** Identifica el aviso para no repetirlo. Los de vencimiento usan el compromiso (RN-15: uno al día). */
        val clave: String = idCompromiso
    )

    /** RN-15: los avisos se entregan solo entre las 08:00 y las 21:59. */
    fun horarioPermitido(hora: Int): Boolean = hora in HORA_INICIO until HORA_FIN

    private fun describir(c: CompromisoEntity): String =
        c.concepto + (c.numeroCuota?.let { " (cuota $it/${c.totalCuotas})" } ?: "")

    /**
     * Avisos que corresponden HOY. Devuelve como máximo uno por compromiso
     * (si un compromiso calza en dos reglas, gana la de mayor prioridad).
     */
    fun avisosDelDia(
        avisosActivos: Boolean,
        diasAnticipacion: Int,
        compromisos: List<CompromisoEntity>,
        nombreEmpresa: (String) -> String,
        hoy: String = Reloj.hoy()
    ): List<Aviso> {
        if (!avisosActivos) return emptyList() // HU-07 CA-2
        return compromisos.mapNotNull { c ->
            val empresa = nombreEmpresa(c.idEmpresa)
            val monto = CurrencyFormatter.formatCLP(c.monto)
            when {
                // RN-15: no se avisa de Pagados, Anulados ni En revisión
                c.estado != "Pendiente" && c.estado != "Vencido" -> null

                c.estado == "Pendiente" && c.fechaVencimiento == hoy -> Aviso(
                    c.idCompromiso, Tipo.VENCE_HOY,
                    "Hoy vence un compromiso",
                    "Hoy es el último día sin recargo para ${describir(c)} de $empresa ($monto). Págalo en menos de un minuto desde la app."
                )

                c.estado == "Pendiente" && Fechas.diasEntre(hoy, c.fechaVencimiento) == diasAnticipacion -> Aviso(
                    c.idCompromiso, Tipo.PROXIMO,
                    "Se acerca un vencimiento",
                    "${describir(c)} de $empresa vence el ${Fechas.mostrar(c.fechaVencimiento)} ($monto). " +
                        "Quedan $diasAnticipacion ${if (diasAnticipacion == 1) "día" else "días"}: puedes dejarlo listo hoy."
                )

                // Solo el día siguiente al vencimiento (no todos los días, para no saturar)
                c.estado == "Vencido" && Fechas.diasAtraso(c.fechaVencimiento, hoy) == 1 -> Aviso(
                    c.idCompromiso, Tipo.VENCIDO,
                    "Te falta poco para quedar al día",
                    "Te falta ${describir(c)} de $empresa para quedar al día " +
                        "(${CurrencyFormatter.formatCLP(c.monto + c.recargoAplicado)}). Puedes pagarlo ahora desde la app."
                )

                else -> null
            }
        }.sortedBy { it.tipo.ordinal }
    }

    /**
     * Novedades de hoy (Aclaración 3, prioridad media): un pago quedó registrado, por cualquier
     * canal (da tranquilidad, P-03), y un reporte de problema fue resuelto (PB-08).
     */
    fun avisosDeNovedades(
        avisosActivos: Boolean,
        pagos: List<PagoEntity>,
        compromisos: Map<String, CompromisoEntity>,
        reportes: List<ReporteEntity>,
        nombreEmpresa: (String) -> String,
        hoy: String = Reloj.hoy()
    ): List<Aviso> {
        if (!avisosActivos) return emptyList()
        val porPago = pagos.filter { it.fechaHora.take(10) == hoy }.mapNotNull { p ->
            val c = compromisos[p.idCompromiso] ?: return@mapNotNull null
            Aviso(
                c.idCompromiso, Tipo.PAGO_REGISTRADO,
                "Tu pago quedó registrado",
                "Registramos tu pago de ${CurrencyFormatter.formatCLP(p.montoPagado)} a ${nombreEmpresa(c.idEmpresa)} " +
                    "(${describir(c)}) por ${p.canal}. Comprobante ${p.numeroComprobante}.",
                clave = "pago|${p.numeroComprobante}"
            )
        }
        val porReporte = reportes.filter { it.estado == "Resuelto" && it.fechaResolucion == hoy }.map { r ->
            val empresa = compromisos[r.idCompromiso]?.let { nombreEmpresa(it.idEmpresa) } ?: "tu compromiso"
            Aviso(
                r.idCompromiso, Tipo.REPORTE_RESUELTO,
                "Respondimos tu reporte",
                "Soporte respondió tu reporte ${r.idReporte} sobre $empresa. Revisa la respuesta en Mis reportes.",
                clave = "reporte|${r.idReporte}"
            )
        }
        return porPago + porReporte
    }

    /**
     * Fecha en que se avisará de un compromiso Pendiente con la anticipación elegida
     * (para mostrarle a la persona "te avisaremos el …"). Null si esa fecha ya pasó.
     */
    fun fechaDelAviso(c: CompromisoEntity, diasAnticipacion: Int, hoy: String = Reloj.hoy()): String? {
        if (c.estado != "Pendiente") return null
        val fecha = Fechas.sumarDias(c.fechaVencimiento, -diasAnticipacion)
        return when {
            fecha >= hoy -> fecha
            c.fechaVencimiento >= hoy -> c.fechaVencimiento // ya pasó la anticipación: queda el aviso del mismo día
            else -> null
        }
    }
}
