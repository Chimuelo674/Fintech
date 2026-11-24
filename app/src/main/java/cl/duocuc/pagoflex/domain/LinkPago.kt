package cl.duocuc.pagoflex.domain

import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.math.roundToLong

/**
 * Links de pago y códigos QR para que otra persona pague un compromiso
 * (por ejemplo, un familiar), como los links de pago por WhatsApp que ofrece PagoFlex.
 *
 * El QR contiene el mismo link, así que escanearlo abre el pago.
 * La dirección es de demostración: esta versión no tiene un servidor que la atienda.
 */
object LinkPago {
    const val BASE = "https://pagoflex.com.ar/pagar/"

    data class Datos(val folio: String, val monto: Long)

    fun generar(folio: String, monto: Double): String {
        require(folio.isNotBlank()) { "El compromiso no tiene folio." }
        require(monto > 0) { "El monto debe ser mayor a cero." }
        val folioUrl = URLEncoder.encode(folio, "UTF-8")
        return "$BASE$folioUrl?monto=${monto.roundToLong()}"
    }

    /** Lee un link de pago generado por [generar]. Devuelve null si no es un link de PagoFlex válido. */
    fun leer(link: String): Datos? {
        if (!link.startsWith(BASE)) return null
        val resto = link.removePrefix(BASE)
        val folioUrl = resto.substringBefore("?")
        val monto = resto.substringAfter("monto=", "").substringBefore("&").toLongOrNull()
        if (folioUrl.isEmpty() || monto == null || monto <= 0) return null
        return Datos(URLDecoder.decode(folioUrl, "UTF-8"), monto)
    }

    fun mensajeParaCompartir(empresa: String, concepto: String, montoTexto: String, link: String): String =
        "Hola, ¿me ayudas a pagar mi $concepto de $empresa por $montoTexto? " +
            "Puedes hacerlo con PagoFlex en este link: $link"
}
