package cl.duocuc.pagoflex.ui.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.roundToLong

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
