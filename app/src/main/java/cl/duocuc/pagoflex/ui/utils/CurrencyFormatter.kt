package cl.duocuc.pagoflex.ui.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun formatCLP(amount: Double): String {
        val localeCL = Locale.Builder().setLanguage("es").setRegion("CL").build()
        val formatter = NumberFormat.getCurrencyInstance(localeCL)
        formatter.maximumFractionDigits = 0
        return formatter.format(amount).replace("CLP", "").trim()
    }
}
