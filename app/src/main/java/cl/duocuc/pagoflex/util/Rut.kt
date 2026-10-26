package cl.duocuc.pagoflex.util

object Rut {
    fun limpiar(rut: String): String = rut.replace(".", "").replace("-", "").replace(" ", "").uppercase()

    fun dvCalculado(cuerpo: String): Char {
        var suma = 0
        var factor = 2
        for (digito in cuerpo.reversed()) {
            suma += (digito - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val r = 11 - suma % 11) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + r
        }
    }

    fun esValido(rut: String): Boolean {
        val limpio = limpiar(rut)
        if (limpio.length < 2) return false
        val cuerpo = limpio.dropLast(1)
        if (cuerpo.isEmpty() || !cuerpo.all { it.isDigit() }) return false
        return dvCalculado(cuerpo) == limpio.last()
    }

    fun formatear(rut: String): String {
        val limpio = limpiar(rut)
        if (limpio.length < 2) return rut
        val cuerpo = limpio.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
        return "$cuerpo-${limpio.last()}"
    }

    fun mismoRut(a: String, b: String): Boolean = limpiar(a) == limpiar(b)
}
