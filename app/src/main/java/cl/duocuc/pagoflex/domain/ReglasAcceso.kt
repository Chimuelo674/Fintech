package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.util.Rut

/**
 * Reglas del ingreso estilo banco: RUT + clave de 4 dígitos, con bloqueo
 * temporal después de varios intentos fallidos.
 *
 * Nota: en esta versión de prueba todas las cuentas usan la misma clave
 * ([CLAVE_DEMO]). Una app real validaría la clave contra el servidor y
 * nunca la guardaría en el teléfono.
 */
object ReglasAcceso {
    const val LARGO_CLAVE = 4
    const val MAX_INTENTOS = 3
    const val SEGUNDOS_BLOQUEO = 30
    const val CLAVE_DEMO = "1234"

    /** Largo máximo de un RUT sin puntos ni guion (8 dígitos + DV). */
    private const val LARGO_MAX_RUT = 9

    /** Formatea el RUT mientras el usuario escribe: "158342073" -> "15.834.207-3". */
    fun formatearMientrasEscribe(entrada: String): String {
        val limpio = entrada.uppercase()
            .filter { it.isDigit() || it == 'K' }
            .let { texto ->
                // La K solo puede ir al final, como dígito verificador.
                val sinK = texto.dropLast(1).filter { it.isDigit() }
                if (texto.isEmpty()) "" else sinK + texto.last()
            }
            .take(LARGO_MAX_RUT)
        if (limpio.length < 2) return limpio
        return Rut.formatear(limpio)
    }

    /** Devuelve el mensaje de error del formulario, o null si se puede intentar ingresar. */
    fun validarFormulario(rut: String, clave: String): String? = when {
        rut.isBlank() -> "Ingresa tu RUT."
        !Rut.esValido(rut) -> "El RUT ingresado no es válido."
        clave.length != LARGO_CLAVE || !clave.all { it.isDigit() } ->
            "Tu clave debe tener $LARGO_CLAVE dígitos."
        else -> null
    }

    fun claveCorrecta(clave: String): Boolean = clave == CLAVE_DEMO

    /** Oculta el RUT como en los comprobantes bancarios: "15.834.207-3" -> "15.***.***-3". */
    fun enmascararRut(rut: String): String {
        val formateado = Rut.formatear(rut)
        val partes = formateado.split("-")
        if (partes.size != 2) return rut
        val bloques = partes[0].split(".")
        val oculto = listOf(bloques.first()) + bloques.drop(1).map { "*".repeat(it.length) }
        return oculto.joinToString(".") + "-" + partes[1]
    }

    /**
     * Lleva la cuenta de intentos fallidos. El tiempo se pasa como parámetro
     * (en milisegundos) para poder probarlo sin esperar.
     */
    class ControlIntentos(
        private val maxIntentos: Int = MAX_INTENTOS,
        private val segundosBloqueo: Int = SEGUNDOS_BLOQUEO
    ) {
        var fallidos: Int = 0
            private set
        private var bloqueadoHasta: Long = 0L

        fun estaBloqueado(ahoraMs: Long): Boolean = ahoraMs < bloqueadoHasta

        fun segundosRestantes(ahoraMs: Long): Int =
            if (!estaBloqueado(ahoraMs)) 0 else (((bloqueadoHasta - ahoraMs) + 999) / 1000).toInt()

        /** Registra un intento fallido y devuelve cuántos intentos quedan (0 = bloqueado). */
        fun registrarFallo(ahoraMs: Long): Int {
            fallidos++
            if (fallidos >= maxIntentos) {
                bloqueadoHasta = ahoraMs + segundosBloqueo * 1000L
                fallidos = 0
                return 0
            }
            return maxIntentos - fallidos
        }

        fun reiniciar() {
            fallidos = 0
            bloqueadoHasta = 0L
        }
    }

    /** Saludo según la hora del día, como en las apps de banco. */
    fun saludo(hora: Int): String = when (hora) {
        in 6..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
