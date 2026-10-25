package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import cl.duocuc.pagoflex.util.Rut

/**
 * Reglas de negocio de los roles internos (agente y ejecutivo), escritas como funciones puras:
 * no dependen de Android ni de la base de datos, así se pueden probar con pruebas unitarias.
 */
object ReglasRolesInternos {

    // ------------------------------------------------------------------ Reportes (RN-23)

    /** Un reporte con más de 2 días hábiles en estado Abierto se considera atrasado. */
    fun reporteAtrasado(reporte: ReporteEntity, hoy: String = Reloj.hoy()): Boolean =
        reporte.estado == "Abierto" && Fechas.diasHabilesEntre(reporte.fechaCreacion, hoy) > 2

    // ------------------------------------------------------------ Recomendaciones (RN-13)

    val ESTADOS_RECOMENDACION = listOf("Recibida", "Contactada", "En evaluación", "Se unió", "Descartada")

    /**
     * Estados a los que puede pasar una recomendación, sin saltarse etapas:
     * Recibida → Contactada → En evaluación → Se unió. Se puede descartar (con motivo)
     * en cualquier etapa abierta. "Se unió" y "Descartada" son finales.
     */
    fun siguientesEstados(estadoActual: String): List<String> = when (estadoActual) {
        "Recibida" -> listOf("Contactada", "Descartada")
        "Contactada" -> listOf("En evaluación", "Descartada")
        "En evaluación" -> listOf("Se unió", "Descartada")
        else -> emptyList()
    }

    // ------------------------------------------------------------- Anulación (RN-21)

    val MOTIVOS_ANULACION = listOf("Repactación", "Error de registro", "Pagado directamente a la empresa", "Otro")

    fun puedeAnularse(c: CompromisoEntity): Boolean =
        c.estado == "Pendiente" || c.estado == "Vencido" || c.estado == "En revisión"

    /** Devuelve el error a mostrar, o null si la anulación es válida. */
    fun validarAnulacion(c: CompromisoEntity, idEmpresaEjecutivo: String, motivo: String, comentario: String): String? = when {
        c.idEmpresa != idEmpresaEjecutivo -> "Solo puedes anular compromisos de tu empresa."
        c.estado == "Pagado" -> "Un compromiso pagado no se puede anular."
        !puedeAnularse(c) -> "Este compromiso ya no se puede anular (está ${c.estado})."
        motivo !in MOTIVOS_ANULACION -> "Elige un motivo de anulación."
        motivo == "Otro" && comentario.isBlank() -> "Con el motivo \"Otro\" debes escribir un comentario."
        else -> null
    }

    // ---------------------------------------------------------- Registro (RN-17, RN-18, RN-20)

    /**
     * Valida los datos de un compromiso nuevo. [fechaVencimiento] viene en formato interno
     * (yyyy-MM-dd) o null si la persona escribió una fecha inválida.
     * Devuelve el error a mostrar, o null si todo está bien.
     */
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

    /** RN-20: mismo compromiso = misma persona, empresa, concepto y cuota (los anulados no cuentan). */
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

    // ------------------------------------------------------ Resumen de cobranza (RN-24)

    data class ResumenCobranza(
        val total: Int,
        val anulados: Int,
        val pagados: Int,
        val pendientes: Int,
        val vencidos: Int,
        val enRevision: Int,
        val montoRecaudado: Double,
        /** null si no hay compromisos (sin anulados) en el mes. */
        val tasa: Double?
    )

    /**
     * Tasa = pagados ÷ (compromisos con vencimiento en el mes − anulados) × 100, con un decimal.
     * Monto recaudado = suma de lo pagado (incluidos recargos) de esos compromisos.
     * Ej. RN-24: 120 compromisos, 2 anulados, 104 pagados -> 104 ÷ 118 = 88,1 %.
     */
    fun resumenDelMes(
        compromisosEmpresa: List<CompromisoEntity>,
        pagosEmpresa: List<PagoEntity>,
        mes: String // "yyyy-MM"
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
