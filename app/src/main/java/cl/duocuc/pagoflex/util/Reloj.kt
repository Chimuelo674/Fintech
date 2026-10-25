package cl.duocuc.pagoflex.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Fecha de referencia del prototipo.
 *
 * Los datos de ejemplo del Anexo 9 están armados para el 06-10-2026 ("Fecha de referencia
 * de los datos"). Si la app usara la fecha real del teléfono, los estados, días de atraso y
 * el indicador no coincidirían con los anexos. Por eso "hoy" es siempre esta fecha;
 * la hora sí es la real (se usa en la fecha y hora del comprobante).
 *
 * Para probar otro día basta con cambiar FECHA_REFERENCIA.
 */
object Reloj {

    const val FECHA_REFERENCIA = "2026-10-06" // formato interno yyyy-MM-dd

    /** "Hoy" en formato interno yyyy-MM-dd. */
    fun hoy(): String = FECHA_REFERENCIA

    /** Fecha de referencia con la hora real del teléfono: "yyyy-MM-dd HH:mm". */
    fun ahora(): String {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return "$FECHA_REFERENCIA $hora"
    }
}

/**
 * Utilidades de fecha sin java.time (minSdk 24 no lo trae sin desugaring).
 * Internamente las fechas se guardan como texto "yyyy-MM-dd", que además se ordena bien
 * como texto. En pantalla se muestran como DD-MM-AAAA (RN-17).
 */
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

    /** Días desde [desde] hasta [hasta] (positivo si hasta es posterior). */
    fun diasEntre(desde: String, hasta: String): Int {
        val a = parse(desde) ?: return 0
        val b = parse(hasta) ?: return 0
        return ((b.timeInMillis - a.timeInMillis) / 86_400_000L).toInt()
    }

    /** Suma meses a una fecha yyyy-MM-dd. */
    fun sumarMeses(fecha: String, meses: Int): String {
        val c = parse(fecha) ?: return fecha
        c.add(Calendar.MONTH, meses)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** RN-02: días de atraso = fecha actual − fecha de vencimiento (0 si aún no vence). */
    fun diasAtraso(fechaVencimiento: String, hoy: String = Reloj.hoy()): Int =
        diasEntre(fechaVencimiento, hoy).coerceAtLeast(0)

    /** RN-02: el compromiso vence a las 23:59 del día; desde el día siguiente está vencido. */
    fun estaVencida(fechaVencimiento: String, hoy: String = Reloj.hoy()): Boolean =
        fechaVencimiento.take(10) < hoy

    /**
     * Días hábiles (lunes a viernes) transcurridos desde [desde] hasta [hasta], sin contar
     * el día inicial. No considera feriados. Ej.: 30-09-2026 (mié) -> 06-10-2026 (mar) = 4.
     */
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

    /** Suma (o resta, si es negativo) días a una fecha yyyy-MM-dd. */
    fun sumarDias(fecha: String, dias: Int): String {
        val c = parse(fecha) ?: return fecha
        c.add(Calendar.DAY_OF_MONTH, dias)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** "2026-10" -> "2026-11" (n = 1) o "2026-09" (n = -1). */
    fun sumarMesesAMes(mes: String, n: Int): String = sumarMeses("$mes-01", n).take(7)

    /** "2026-10" -> "Octubre 2026". */
    fun nombreMes(mes: String): String {
        val nombres = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val m = mes.drop(5).take(2).toIntOrNull() ?: return mes
        return "${nombres[m - 1]} ${mes.take(4)}"
    }

    /** "2026-10-10" -> "10-10-2026"; "2026-10-10 13:05" -> "10-10-2026 13:05" (RN-17). */
    fun mostrar(fecha: String?): String {
        if (fecha.isNullOrBlank() || fecha.length < 10) return fecha ?: ""
        val (a, m, d) = fecha.take(10).split("-").takeIf { it.size == 3 } ?: return fecha
        return "$d-$m-$a" + fecha.drop(10)
    }

    /** "10-10-2026" -> "2026-10-10" (para lo que escribe la persona en un formulario). */
    fun desdePantalla(fecha: String): String? {
        val partes = fecha.trim().split("-", "/")
        if (partes.size != 3) return null
        val (d, m, a) = partes
        val interna = "${a.padStart(4, '0')}-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        return interna.takeIf { esValida(it) }
    }
}
