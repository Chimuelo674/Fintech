package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.domain.ReglasAvisos
import cl.duocuc.pagoflex.domain.ReglasAvisos.Tipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisosTest {
    private val arriendo = CompromisoEntity("CP-2026-0101", "CP-2026-0101", "UF-01", "EC-08", "Arriendo departamento octubre", 10, 12, 520000.0, "2026-10-10", "Pendiente")
    private val club = CompromisoEntity("CP-2026-0102", "CP-2026-0102", "UF-01", "EC-06", "Cuota social octubre", 10, 12, 15000.0, "2026-10-05", "Vencido", 2000.0)
    private val aporte = CompromisoEntity("CP-2026-0103", "CP-2026-0103", "UF-01", "EC-05", "Aporte mensual octubre", null, null, 10000.0, "2026-10-15", "Pendiente")
    private val credito = CompromisoEntity("CP-2026-0104", "CP-2026-0104", "UF-01", "EC-03", "Crédito de consumo", 7, 24, 86400.0, "2026-09-25", "Pagado")
    private val todos = listOf(arriendo, club, aporte, credito)
    private val nombre: (String) -> String = { it }

    @Test
    fun hu07CA1_tresDiasAntesDelArriendo() {
        val avisos = ReglasAvisos.avisosDelDia(true, 3, todos, nombre, hoy = "2026-10-07")
        val a = avisos.single { it.idCompromiso == "CP-2026-0101" }
        assertEquals(Tipo.PROXIMO, a.tipo)
        assertTrue(a.mensaje.contains("10-10-2026"))
    }

    @Test
    fun hu07CA2_avisosDesactivadosNoAvisa() {
        assertTrue(ReglasAvisos.avisosDelDia(false, 3, todos, nombre, hoy = "2026-10-07").isEmpty())
    }

    @Test
    fun hu07CA3_nuncaEntre22y08() {
        assertFalse(ReglasAvisos.horarioPermitido(23))
        assertFalse(ReglasAvisos.horarioPermitido(22))
        assertFalse(ReglasAvisos.horarioPermitido(7))
        assertTrue(ReglasAvisos.horarioPermitido(8))
        assertTrue(ReglasAvisos.horarioPermitido(21))
    }

    @Test
    fun venceHoyTienePrioridadYEsUnoPorCompromiso() {
        val avisos = ReglasAvisos.avisosDelDia(true, 3, todos, nombre, hoy = "2026-10-10")
        assertEquals(1, avisos.count { it.idCompromiso == "CP-2026-0101" })
        assertEquals(Tipo.VENCE_HOY, avisos.first { it.idCompromiso == "CP-2026-0101" }.tipo)
    }

    @Test
    fun vencidoSoloElPrimerDiaYConTonoPositivo() {
        val dia1 = ReglasAvisos.avisosDelDia(true, 3, listOf(club), nombre, hoy = "2026-10-06")
        assertEquals(Tipo.VENCIDO, dia1.single().tipo)
        assertTrue(dia1.single().mensaje.startsWith("Te falta"))
        assertTrue("incluye recargo", dia1.single().mensaje.contains("17.000"))
        assertTrue(ReglasAvisos.avisosDelDia(true, 3, listOf(club), nombre, hoy = "2026-10-07").isEmpty())
    }

    @Test
    fun noAvisaPagadosAnuladosNiEnRevision() {
        val enRevision = arriendo.copy(estado = "En revisión")
        val anulado = arriendo.copy(idCompromiso = "X", estado = "Anulado")
        val avisos = ReglasAvisos.avisosDelDia(true, 3, listOf(credito, enRevision, anulado), nombre, hoy = "2026-10-07")
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun fechaDelAviso() {
        assertEquals("2026-10-07", ReglasAvisos.fechaDelAviso(arriendo, 3, hoy = "2026-10-06"))
        assertEquals("2026-10-05", ReglasAvisos.fechaDelAviso(arriendo, 5, hoy = "2026-10-05"))
        assertEquals("2026-10-10", ReglasAvisos.fechaDelAviso(arriendo, 5, hoy = "2026-10-06"))
        assertNull(ReglasAvisos.fechaDelAviso(club, 3, hoy = "2026-10-06"))
    }

    @Test
    fun pagoRegistradoHoyPorCualquierCanal() {
        val pago = PagoEntity("CPR-000454", "CPR-000454", "CP-2026-0101", "2026-10-06 12:30", 520000.0, "Portal de pagos")
        val antiguo = PagoEntity("CPR-000451", "CPR-000451", "CP-2026-0104", "2026-09-24 10:42", 86400.0)
        val avisos = ReglasAvisos.avisosDeNovedades(true, listOf(pago, antiguo), todos.associateBy { it.idCompromiso }, emptyList(), nombre, hoy = "2026-10-06")
        val a = avisos.single()
        assertEquals(Tipo.PAGO_REGISTRADO, a.tipo)
        assertEquals("pago|CPR-000454", a.clave)
        assertTrue(a.mensaje.contains("Portal de pagos"))
    }

    @Test
    fun reporteResueltoHoy() {
        val resuelto = ReporteEntity("REP-0012", "CP-2026-0101", "Otro", "x", "2026-10-05", "Resuelto", "AG-01", "2026-10-05", "Listo", "2026-10-06")
        val abierto = ReporteEntity("REP-0013", "CP-2026-0103", "Otro", "x", "2026-10-06", "Abierto")
        val avisos = ReglasAvisos.avisosDeNovedades(true, emptyList(), todos.associateBy { it.idCompromiso }, listOf(resuelto, abierto), nombre, hoy = "2026-10-06")
        assertEquals(Tipo.REPORTE_RESUELTO, avisos.single().tipo)
        assertTrue(avisos.single().mensaje.contains("REP-0012"))
    }

    @Test
    fun novedadesRespetanAvisosDesactivados() {
        val pago = PagoEntity("CPR-000454", "CPR-000454", "CP-2026-0101", "2026-10-06 12:30", 520000.0)
        assertTrue(ReglasAvisos.avisosDeNovedades(false, listOf(pago), todos.associateBy { it.idCompromiso }, emptyList(), nombre, hoy = "2026-10-06").isEmpty())
    }

    @Test
    fun formatoPesosChilenos() {
        assertEquals("$ 520.000", CurrencyFormatter.formatCLP(520000.0))
        assertEquals("$ 0", CurrencyFormatter.formatCLP(0.0))
        assertEquals("$ 1.349.000", CurrencyFormatter.formatCLP(1349000.0))
    }
}
