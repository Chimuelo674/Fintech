package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.util.Rut

object ReglasRecomendacion {
    const val MAXIMO_POR_MES = 5

    data class Datos(
        val nombreOrganizacion: String,
        val rubro: String,
        val comuna: String,
        val rutOrganizacion: String = "",
        val nombreContacto: String = "",
        val correoContacto: String = "",
        val telefonoContacto: String = "",
        val autorizaContacto: Boolean = false
    ) {
        val tieneDatosDeContacto: Boolean
            get() = nombreContacto.isNotBlank() || correoContacto.isNotBlank() || telefonoContacto.isNotBlank()
    }

    fun normalizar(nombre: String): String = nombre.trim().lowercase().replace(Regex("\\s+"), " ")

    fun buscarDuplicada(existentes: List<RecomendacionEntity>, nombreOrganizacion: String): RecomendacionEntity? =
        existentes.firstOrNull { normalizar(it.nombreOrganizacion) == normalizar(nombreOrganizacion) }

    fun usadasEnElMes(existentes: List<RecomendacionEntity>, mes: String): Int =
        existentes.count { it.fecha.take(7) == mes }

    fun validar(d: Datos): String? = when {
        d.nombreOrganizacion.isBlank() -> "Ingresa el nombre de la organización."
        d.rubro.isBlank() -> "Indica el rubro (por ejemplo, jardín infantil o club deportivo)."
        d.comuna.isBlank() -> "Indica la comuna."
        d.rutOrganizacion.isNotBlank() && !Rut.esValido(d.rutOrganizacion) -> "El RUT de la organización no es válido."
        d.tieneDatosDeContacto && !d.autorizaContacto ->
            "Para enviar datos de contacto, confirma que esa persona autorizó que PagoFlex la contacte. Si no, deja el contacto vacío."
        d.tieneDatosDeContacto && d.nombreContacto.isBlank() -> "Indica el nombre de la persona de contacto."
        d.tieneDatosDeContacto && d.correoContacto.isBlank() && d.telefonoContacto.isBlank() ->
            "Indica un correo o un teléfono de contacto."
        d.correoContacto.isNotBlank() && !d.correoContacto.contains("@") -> "El correo de contacto no es válido."
        else -> null
    }

    val ETAPAS = listOf("Recibida", "Contactada", "En evaluación", "Se unió")
}
