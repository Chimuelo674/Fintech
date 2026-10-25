package cl.duocuc.pagoflex.ui.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.roundToLong

/**
 * Montos en pesos chilenos con el formato que pide el anexo (RNF-02): "$ 520.000".
 * Signo peso, un espacio, punto como separador de miles y sin decimales (RN-17: montos enteros).
 * Se arma a mano para que el resultado no dependa de cómo cada versión de Android formatea es-CL.
 */
object CurrencyFormatter {

    private val simbolos = DecimalFormatSymbols().apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatCLP(amount: Double): String {
        val entero = amount.roundToLong()
        val texto = DecimalFormat("#,##0", simbolos).format(kotlin.math.abs(entero))
        return if (entero < 0) "-$ $texto" else "$ $texto"
    }
}
