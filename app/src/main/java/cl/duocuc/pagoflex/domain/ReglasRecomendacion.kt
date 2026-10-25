package cl.duocuc.pagoflex.domain

import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.util.Rut

/** Reglas para que un usuario final recomiende una organización (RF-12, RN-12). */
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

    /** "  Jardín  Infantil " -> "jardín infantil" (para detectar la misma organización). */
    fun normalizar(nombre: String): String = nombre.trim().lowercase().replace(Regex("\\s+"), " ")

    /** Recomendación existente del mismo usuario para esa organización, si la hay (HU-08 CA-3). */
    fun buscarDuplicada(existentes: List<RecomendacionEntity>, nombreOrganizacion: String): RecomendacionEntity? =
        existentes.firstOrNull { normalizar(it.nombreOrganizacion) == normalizar(nombreOrganizacion) }

    fun usadasEnElMes(existentes: List<RecomendacionEntity>, mes: String): Int =
        existentes.count { it.fecha.take(7) == mes }

    /**
     * Devuelve el error a mostrar, o null si los datos cumplen RN-12:
     * - obligatorios: nombre de la organización, rubro y comuna;
     * - RUT de la organización opcional, pero con dígito verificador válido si se ingresa (RN-18);
     * - datos de contacto opcionales y SOLO si la persona declara que el contacto autorizó
     *   ser contactado por PagoFlex (HU-08 CA-2); si los ingresa, nombre y un medio de contacto.
     */
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

    /** Etapas visibles para el usuario (RN-13). */
    val ETAPAS = listOf("Recibida", "Contactada", "En evaluación", "Se unió")
}
