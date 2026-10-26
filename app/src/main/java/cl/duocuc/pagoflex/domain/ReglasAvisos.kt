package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj

object ReglasAvisos {
    val OPCIONES_ANTICIPACION = listOf(1, 3, 5)
    const val HORA_INICIO = 8
    const val HORA_FIN = 22

    enum class Tipo { VENCE_HOY, PROXIMO, VENCIDO, PAGO_REGISTRADO, REPORTE_RESUELTO }

    data class Aviso(
        val idCompromiso: String,
        val tipo: Tipo,
        val titulo: String,
        val mensaje: String,
        val clave: String = idCompromiso
    )

    fun horarioPermitido(hora: Int): Boolean = hora in HORA_INICIO until HORA_FIN

    private fun describir(c: CompromisoEntity): String =
        c.concepto + (c.numeroCuota?.let { " (cuota $it/${c.totalCuotas})" } ?: "")

    fun avisosDelDia(
        avisosActivos: Boolean,
        diasAnticipacion: Int,
        compromisos: List<CompromisoEntity>,
        nombreEmpresa: (String) -> String,
        hoy: String = Reloj.hoy()
    ): List<Aviso> {
        if (!avisosActivos) return emptyList()
        return compromisos.mapNotNull { c ->
            val empresa = nombreEmpresa(c.idEmpresa)
            val monto = CurrencyFormatter.formatCLP(c.monto)
            when {
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

    fun fechaDelAviso(c: CompromisoEntity, diasAnticipacion: Int, hoy: String = Reloj.hoy()): String? {
        if (c.estado != "Pendiente") return null
        val fecha = Fechas.sumarDias(c.fechaVencimiento, -diasAnticipacion)
        return when {
            fecha >= hoy -> fecha
            c.fechaVencimiento >= hoy -> c.fechaVencimiento
            else -> null
        }
    }
}
