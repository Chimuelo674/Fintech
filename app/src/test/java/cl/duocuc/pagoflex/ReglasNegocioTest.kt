package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.seed.HistorialSintetico
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.util.Fechas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasNegocioTest {
    private val hoy = "2026-10-06"

    @Test
    fun diasDeAtraso_RN02() {
        assertEquals(6, Fechas.diasAtraso("2026-09-30", hoy))
        assertEquals(1, Fechas.diasAtraso("2026-10-05", hoy))
        assertEquals(0, Fechas.diasAtraso("2026-10-10", hoy))
    }

    @Test
    fun venceAlFinalDelDia_RN02() {
        assertFalse("el mismo día todavía no está vencido", Fechas.estaVencida("2026-10-06", hoy))
        assertTrue(Fechas.estaVencida("2026-10-05", hoy))
    }

    @Test
    fun formatoEnPantalla_RN17() {
        assertEquals("10-10-2026", Fechas.mostrar("2026-10-10"))
        assertEquals("24-09-2026 10:42", Fechas.mostrar("2026-09-24 10:42"))
        assertEquals("2026-11-30", Fechas.desdePantalla("30-11-2026"))
        assertNull(Fechas.desdePantalla("31-02-2026"))
    }

    private fun comp(id: String, usuario: String, venc: String, estado: String) =
        CompromisoEntity(id, id, usuario, "EC-01", "Prueba $id", null, null, 1000.0, venc, estado)

    private fun pago(idComp: String, usuario: String, fecha: String) =
        PagoEntity("P-$idComp", "P-$idComp", idComp, "$fecha 12:00", 1000.0)

    @Test
    fun ejemploDelAnexo_33de36_es91coma7_AlDia() {
        val explicitos = listOf(
            comp("CP-2026-0101", "UF-01", "2026-10-10", "Pendiente"),
            comp("CP-2026-0102", "UF-01", "2026-10-05", "Vencido"),
            comp("CP-2026-0103", "UF-01", "2026-10-15", "Pendiente"),
            comp("CP-2026-0104", "UF-01", "2026-09-25", "Pagado"),
            comp("CP-2026-0105", "UF-01", "2026-10-25", "Pendiente"),
            comp("CP-2026-0089", "UF-01", "2026-09-10", "Pagado"),
            comp("CP-2026-0090", "UF-01", "2026-09-05", "Pagado"),
            comp("CP-2026-0091", "UF-01", "2026-09-15", "Pagado")
        )
        val pagosExplicitos = listOf(
            pago("CP-2026-0104", "UF-01", "2026-09-24"),
            pago("CP-2026-0089", "UF-01", "2026-09-09"),
            pago("CP-2026-0090", "UF-01", "2026-09-07"),
            pago("CP-2026-0091", "UF-01", "2026-09-15")
        )
        val compromisos = explicitos + HistorialSintetico.compromisos.filter { it.idUsuarioFinal == "UF-01" }
        val idsUf01 = compromisos.map { it.idCompromiso }.toSet()
        val pagos = pagosExplicitos + HistorialSintetico.pagos.filter { it.idCompromiso in idsUf01 }

        val r = PagoFlexRepository.calcularCumplimiento(compromisos, pagos, hoy)

        assertEquals(36, r.considerados)
        assertEquals(33, r.aTiempo)
        assertEquals(91.7, r.porcentaje!!, 0.0001)
        assertEquals("Al día", r.categoria)
    }

    @Test
    fun excluyeAnuladosEnRevisionYNoVencidos() {
        val compromisos = listOf(
            comp("A", "U", "2026-09-01", "Pagado"),
            comp("B", "U", "2026-09-02", "Pagado"),
            comp("C", "U", "2026-09-03", "Pagado"),
            comp("D", "U", "2026-09-04", "Anulado"),
            comp("E", "U", "2026-09-05", "En revisión"),
            comp("F", "U", "2026-10-20", "Pendiente")
        )
        val pagos = listOf(pago("A", "U", "2026-09-01"), pago("B", "U", "2026-09-01"), pago("C", "U", "2026-09-10"))

        val r = PagoFlexRepository.calcularCumplimiento(compromisos, pagos, hoy)

        assertEquals(3, r.considerados)
        assertEquals(2, r.aTiempo)
        assertEquals(66.7, r.porcentaje!!, 0.0001)
        assertEquals("En camino", r.categoria)
    }

    @Test
    fun menosDeTresEsHistorialInsuficiente_HU05_CA2() {
        val compromisos = listOf(
            comp("A", "U", "2026-09-01", "Pagado"),
            comp("B", "U", "2026-09-02", "Pagado")
        )
        val r = PagoFlexRepository.calcularCumplimiento(compromisos, emptyList(), hoy)
        assertNull(r.porcentaje)
        assertEquals("Historial insuficiente", r.categoria)
    }

    @Test
    fun categorias_RN11() {
        fun categoria(aTiempo: Int, total: Int): String {
            val cs = (1..total).map { comp("C$it", "U", "2026-09-%02d".format(it), "Pagado") }
            val ps = (1..aTiempo).map { pago("C$it", "U", "2026-09-01") }
            return PagoFlexRepository.calcularCumplimiento(cs, ps, hoy).categoria
        }
        assertEquals("Ejemplar", categoria(19, 20))
        assertEquals("Al día", categoria(16, 20))
        assertEquals("En camino", categoria(15, 20))
    }
}
