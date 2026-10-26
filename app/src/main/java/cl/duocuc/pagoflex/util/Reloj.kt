package cl.duocuc.pagoflex.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Reloj {
    const val FECHA_REFERENCIA = "2026-10-06"

    fun hoy(): String = FECHA_REFERENCIA

    fun ahora(): String {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return "$FECHA_REFERENCIA $hora"
    }
}

object Fechas {
    private val UTC = TimeZone.getTimeZone("UTC")

    private fun parse(fecha: String): Calendar? = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC
            isLenient = false
        }
        Calendar.getInstance(UTC).apply { time = sdf.parse(fecha.take(10))!! }
    } catch (e: Exception) {
        null
    }

    fun esValida(fecha: String): Boolean = fecha.length == 10 && parse(fecha) != null

    fun diasEntre(desde: String, hasta: String): Int {
        val a = parse(desde) ?: return 0
        val b = parse(hasta) ?: return 0
        return ((b.timeInMillis - a.timeInMillis) / 86_400_000L).toInt()
    }

    fun sumarMeses(fecha: String, meses: Int): String {
        val c = parse(fecha) ?: return fecha
        c.add(Calendar.MONTH, meses)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun diasAtraso(fechaVencimiento: String, hoy: String = Reloj.hoy()): Int =
        diasEntre(fechaVencimiento, hoy).coerceAtLeast(0)

    fun estaVencida(fechaVencimiento: String, hoy: String = Reloj.hoy()): Boolean =
        fechaVencimiento.take(10) < hoy

    fun diasHabilesEntre(desde: String, hasta: String): Int {
        val d = parse(desde) ?: return 0
        val h = parse(hasta) ?: return 0
        var cuenta = 0
        while (d.before(h)) {
            d.add(Calendar.DAY_OF_MONTH, 1)
            val dia = d.get(Calendar.DAY_OF_WEEK)
            if (dia != Calendar.SATURDAY && dia != Calendar.SUNDAY) cuenta++
        }
        return cuenta
    }

    fun sumarDias(fecha: String, dias: Int): String {
        val c = parse(fecha) ?: return fecha
        c.add(Calendar.DAY_OF_MONTH, dias)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun sumarMesesAMes(mes: String, n: Int): String = sumarMeses("$mes-01", n).take(7)

    fun nombreMes(mes: String): String {
        val nombres = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val m = mes.drop(5).take(2).toIntOrNull() ?: return mes
        return "${nombres[m - 1]} ${mes.take(4)}"
    }

    fun mostrar(fecha: String?): String {
        if (fecha.isNullOrBlank() || fecha.length < 10) return fecha ?: ""
        val (a, m, d) = fecha.take(10).split("-").takeIf { it.size == 3 } ?: return fecha
        return "$d-$m-$a" + fecha.drop(10)
    }

    fun desdePantalla(fecha: String): String? {
        val partes = fecha.trim().split("-", "/")
        if (partes.size != 3) return null
        val (d, m, a) = partes
        val interna = "${a.padStart(4, '0')}-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        return interna.takeIf { esValida(it) }
    }
}
