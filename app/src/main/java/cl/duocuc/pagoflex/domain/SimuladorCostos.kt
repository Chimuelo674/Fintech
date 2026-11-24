package cl.duocuc.pagoflex.domain

/**
 * Simulador de costos, basado en la herramienta de pagoflex.com.ar:
 * PagoFlex cobra un 3% fijo sin cargos ocultos, mientras que un banco
 * tradicional suma comisión + IVA sobre la comisión + mantención mensual.
 *
 * Los valores del banco son referenciales y se pueden ajustar.
 */
object SimuladorCostos {

    data class Parametros(
        val tasaPagoFlex: Double = 0.03,
        val tasaBanco: Double = 0.0295,
        val iva: Double = 0.19,
        val mantencionMensualBanco: Double = 6_500.0
    )

    data class Resultado(
        val ventasMensuales: Double,
        val costoPagoFlex: Double,
        val comisionBanco: Double,
        val ivaBanco: Double,
        val mantencionBanco: Double,
        val costoBanco: Double,
        val ahorroMensual: Double,
        val ahorroAnual: Double,
        val tasaEfectivaBanco: Double
    ) {
        val pagoFlexConviene: Boolean get() = ahorroMensual > 0
    }

    const val VENTAS_MAXIMAS = 1_000_000_000.0

    fun calcular(ventasMensuales: Double, p: Parametros = Parametros()): Resultado {
        require(ventasMensuales >= 0) { "Las ventas no pueden ser negativas." }
        val ventas = ventasMensuales.coerceAtMost(VENTAS_MAXIMAS)

        val costoPagoFlex = ventas * p.tasaPagoFlex
        val comision = ventas * p.tasaBanco
        val iva = comision * p.iva
        // Sin ventas no se cobra mantención en la comparación (no hay cuenta que mantener).
        val mantencion = if (ventas > 0) p.mantencionMensualBanco else 0.0
        val costoBanco = comision + iva + mantencion
        val ahorro = costoBanco - costoPagoFlex

        return Resultado(
            ventasMensuales = ventas,
            costoPagoFlex = costoPagoFlex,
            comisionBanco = comision,
            ivaBanco = iva,
            mantencionBanco = mantencion,
            costoBanco = costoBanco,
            ahorroMensual = ahorro,
            ahorroAnual = ahorro * 12,
            tasaEfectivaBanco = if (ventas > 0) costoBanco / ventas else 0.0
        )
    }

    /** Convierte lo que escribe el usuario ("1.500.000", "$ 2000") a número. */
    fun leerMonto(texto: String): Double? {
        val digitos = texto.filter { it.isDigit() }
        if (digitos.isEmpty()) return null
        return digitos.take(10).toDouble()
    }
}
