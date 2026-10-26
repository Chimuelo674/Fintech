package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import cl.duocuc.pagoflex.util.Rut

object ReglasRolesInternos {
    fun reporteAtrasado(reporte: ReporteEntity, hoy: String = Reloj.hoy()): Boolean =
        reporte.estado == "Abierto" && Fechas.diasHabilesEntre(reporte.fechaCreacion, hoy) > 2

    val ESTADOS_RECOMENDACION = listOf("Recibida", "Contactada", "En evaluación", "Se unió", "Descartada")

    fun siguientesEstados(estadoActual: String): List<String> = when (estadoActual) {
        "Recibida" -> listOf("Contactada", "Descartada")
        "Contactada" -> listOf("En evaluación", "Descartada")
        "En evaluación" -> listOf("Se unió", "Descartada")
        else -> emptyList()
    }

    val MOTIVOS_ANULACION = listOf("Repactación", "Error de registro", "Pagado directamente a la empresa", "Otro")

    fun puedeAnularse(c: CompromisoEntity): Boolean =
        c.estado == "Pendiente" || c.estado == "Vencido" || c.estado == "En revisión"

    fun validarAnulacion(c: CompromisoEntity, idEmpresaEjecutivo: String, motivo: String, comentario: String): String? = when {
        c.idEmpresa != idEmpresaEjecutivo -> "Solo puedes anular compromisos de tu empresa."
        c.estado == "Pagado" -> "Un compromiso pagado no se puede anular."
        !puedeAnularse(c) -> "Este compromiso ya no se puede anular (está ${c.estado})."
        motivo !in MOTIVOS_ANULACION -> "Elige un motivo de anulación."
        motivo == "Otro" && comentario.isBlank() -> "Con el motivo \"Otro\" debes escribir un comentario."
        else -> null
    }

    fun validarDatosCompromiso(
        rut: String,
        concepto: String,
        cuota: Int?,
        totalCuotas: Int?,
        monto: Long?,
        fechaVencimiento: String?,
        hoy: String = Reloj.hoy()
    ): String? = when {
        rut.isBlank() -> "Ingresa el RUT de la persona."
        !Rut.esValido(rut) -> "El RUT no es válido (revisa el dígito verificador)."
        concepto.isBlank() -> "Ingresa el concepto."
        monto == null || monto <= 0 -> "El monto debe ser un número entero mayor que cero."
        fechaVencimiento == null -> "Ingresa la fecha de vencimiento como DD-MM-AAAA."
        fechaVencimiento < hoy -> "La fecha de vencimiento no puede ser anterior a hoy (${Fechas.mostrar(hoy)})."
        (cuota == null) != (totalCuotas == null) -> "Si el compromiso es en cuotas, indica el número de cuota y el total."
        cuota != null && totalCuotas != null && (cuota < 1 || totalCuotas < 1) -> "La cuota y el total deben ser mayores que cero."
        cuota != null && totalCuotas != null && cuota > totalCuotas -> "La cuota no puede ser mayor que el total de cuotas."
        else -> null
    }

    fun esDuplicado(
        existentes: List<CompromisoEntity>,
        idUsuario: String,
        idEmpresa: String,
        concepto: String,
        cuota: Int?
    ): Boolean = existentes.any {
        it.estado != "Anulado" &&
            it.idUsuarioFinal == idUsuario &&
            it.idEmpresa == idEmpresa &&
            it.concepto.trim().equals(concepto.trim(), ignoreCase = true) &&
            it.numeroCuota == cuota
    }

    data class ResumenCobranza(
        val total: Int,
        val anulados: Int,
        val pagados: Int,
        val pendientes: Int,
        val vencidos: Int,
        val enRevision: Int,
        val montoRecaudado: Double,
        val tasa: Double?
    )

    fun resumenDelMes(
        compromisosEmpresa: List<CompromisoEntity>,
        pagosEmpresa: List<PagoEntity>,
        mes: String
    ): ResumenCobranza {
        val delMes = compromisosEmpresa.filter { it.fechaVencimiento.take(7) == mes }
        val anulados = delMes.count { it.estado == "Anulado" }
        val pagadosList = delMes.filter { it.estado == "Pagado" }
        val idsPagados = pagadosList.map { it.idCompromiso }.toSet()
        val recaudado = pagosEmpresa.filter { it.idCompromiso in idsPagados }.sumOf { it.montoPagado }
        val base = delMes.size - anulados
        val tasa = if (base > 0) Math.round(pagadosList.size * 1000.0 / base) / 10.0 else null
        return ResumenCobranza(
            total = delMes.size,
            anulados = anulados,
            pagados = pagadosList.size,
            pendientes = delMes.count { it.estado == "Pendiente" },
            vencidos = delMes.count { it.estado == "Vencido" },
            enRevision = delMes.count { it.estado == "En revisión" },
            montoRecaudado = recaudado,
            tasa = tasa
        )
    }
}
