package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.domain.ReglasRecomendacion
import cl.duocuc.pagoflex.domain.ReglasRecomendacion.Datos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecomendacionTest {
    private val base = Datos(nombreOrganizacion = "Jardín Infantil Los Peques", rubro = "Educación parvularia", comuna = "Maipú")

    @Test
    fun hu08CA1_sinContactoSeRegistra() {
        assertNull(ReglasRecomendacion.validar(base))
    }

    @Test
    fun obligatorios() {
        assertNotNull(ReglasRecomendacion.validar(base.copy(nombreOrganizacion = " ")))
        assertNotNull(ReglasRecomendacion.validar(base.copy(rubro = "")))
        assertNotNull(ReglasRecomendacion.validar(base.copy(comuna = "")))
    }

    @Test
    fun rutOpcionalPeroValido() {
        assertNull(ReglasRecomendacion.validar(base.copy(rutOrganizacion = "65.132.874-8")))
        assertNotNull(ReglasRecomendacion.validar(base.copy(rutOrganizacion = "65.132.874-9")))
    }

    @Test
    fun hu08CA2_contactoSinAutorizacionNoSeEnvia() {
        val conContacto = base.copy(nombreContacto = "Paula Herrera", correoContacto = "ph@jardin.ejemplo")
        assertNotNull(ReglasRecomendacion.validar(conContacto))
        assertNull(ReglasRecomendacion.validar(conContacto.copy(autorizaContacto = true)))
    }

    @Test
    fun contactoAutorizadoNecesitaNombreYUnMedio() {
        assertNotNull(ReglasRecomendacion.validar(base.copy(correoContacto = "x@y.cl", autorizaContacto = true)))
        assertNotNull(ReglasRecomendacion.validar(base.copy(nombreContacto = "Ana", autorizaContacto = true)))
        assertNotNull(ReglasRecomendacion.validar(base.copy(nombreContacto = "Ana", correoContacto = "sin-arroba", autorizaContacto = true)))
    }

    private fun rec(nombre: String, fecha: String) =
        RecomendacionEntity("R-$nombre-$fecha", "UF-01", nombre, fecha = fecha, estado = "Contactada")

    @Test
    fun hu08CA3_duplicadaIgnoraMayusculasYEspacios() {
        val existentes = listOf(rec("Comunidad Edificio Parque Los Olmos", "2026-09-02"))
        val dup = ReglasRecomendacion.buscarDuplicada(existentes, "  comunidad edificio  parque los olmos ")
        assertNotNull(dup)
        assertEquals("Contactada", dup!!.estado)
        assertNull(ReglasRecomendacion.buscarDuplicada(existentes, "Otra organización"))
    }

    @Test
    fun maximoCincoPorMes() {
        val existentes = (1..5).map { rec("Org $it", "2026-10-0$it") } + rec("Vieja", "2026-09-30")
        assertEquals(5, ReglasRecomendacion.usadasEnElMes(existentes, "2026-10"))
        assertEquals(1, ReglasRecomendacion.usadasEnElMes(existentes, "2026-09"))
    }
}
