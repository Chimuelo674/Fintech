package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.seed.CobranzaSeptiembreSintetica
import cl.duocuc.pagoflex.domain.ReglasRolesInternos as R
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Rut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas del agente PagoFlex y del ejecutivo de empresa cliente (RN-13, RN-18, RN-20, RN-21, RN-23, RN-24). */
class RolesInternosTest {

    private val hoy = "2026-10-06"

    // ---------------- RN-18: RUT

    @Test
    fun rutsDelAnexo9SonValidos() {
        listOf("15.834.207-3", "12.476.318-5", "6.893.541-5", "76.845.213-K", "16.248.317-K", "9.846.217-1")
            .forEach { assertTrue(it, Rut.esValido(it)) }
    }

    @Test
    fun rutConDigitoVerificadorIncorrectoNoEsValido() {
        assertFalse(Rut.esValido("15.834.207-4"))
        assertFalse(Rut.esValido("abc"))
        assertEquals("12.476.318-5", Rut.formatear("124763185"))
        assertTrue(Rut.mismoRut("12476318-5", "12.476.318-5"))
    }

    // ---------------- RN-23: reporte atrasado

    private fun reporte(id: String, fecha: String, estado: String) =
        ReporteEntity(id, "CP", "Otro", null, fecha, estado)

    @Test
    fun diasHabiles() {
        // 30-09-2026 (miércoles) -> 06-10-2026 (martes): jue, vie, lun, mar = 4
        assertEquals(4, Fechas.diasHabilesEntre("2026-09-30", hoy))
        // viernes -> lunes = 1 día hábil
        assertEquals(1, Fechas.diasHabilesEntre("2026-10-02", "2026-10-05"))
    }

    @Test
    fun rep0010EstaAtrasadoYRep0011No() {
        assertTrue(R.reporteAtrasado(reporte("REP-0010", "2026-09-30", "Abierto"), hoy))
        assertFalse("en gestión no cuenta como atrasado", R.reporteAtrasado(reporte("REP-0011", "2026-10-03", "En gestión"), hoy))
        // Abierto hace solo 2 días hábiles (vie 02-10 -> mar 06-10 = lun, mar) -> aún no
        assertFalse(R.reporteAtrasado(reporte("X", "2026-10-02", "Abierto"), hoy))
    }

    // ---------------- RN-13: no saltarse etapas (HU-10 CA-4)

    @Test
    fun recomendacionNoSeSaltaEtapas() {
        assertFalse("Recibida no puede pasar directo a Se unió", "Se unió" in R.siguientesEstados("Recibida"))
        assertEquals(listOf("Contactada", "Descartada"), R.siguientesEstados("Recibida"))
        assertEquals(listOf("En evaluación", "Descartada"), R.siguientesEstados("Contactada"))
        assertEquals(listOf("Se unió", "Descartada"), R.siguientesEstados("En evaluación"))
        assertTrue(R.siguientesEstados("Se unió").isEmpty())
        assertTrue(R.siguientesEstados("Descartada").isEmpty())
    }

    // ---------------- RN-20: registro (HU-11)

    @Test
    fun registroValidoHU11CA1() {
        // Paula registra la cuota 16/36 de Luis por $310.000 con vencimiento 30-11-2026
        assertNull(R.validarDatosCompromiso("12.476.318-5", "Cuota pie departamento", 16, 36, 310000, "2026-11-30", hoy))
    }

    @Test
    fun registroInvalido() {
        assertNotNull("RUT malo", R.validarDatosCompromiso("12.476.318-4", "X", null, null, 1000, "2026-11-30", hoy))
        assertNotNull("monto 0", R.validarDatosCompromiso("12.476.318-5", "X", null, null, 0, "2026-11-30", hoy))
        assertNotNull("fecha pasada", R.validarDatosCompromiso("12.476.318-5", "X", null, null, 1000, "2026-10-05", hoy))
        assertNotNull("fecha inválida", R.validarDatosCompromiso("12.476.318-5", "X", null, null, 1000, null, hoy))
        assertNotNull("cuota > total", R.validarDatosCompromiso("12.476.318-5", "X", 37, 36, 1000, "2026-11-30", hoy))
        assertNotNull("cuota sin total", R.validarDatosCompromiso("12.476.318-5", "X", 3, null, 1000, "2026-11-30", hoy))
        assertNull("hoy mismo sí se permite", R.validarDatosCompromiso("12.476.318-5", "X", null, null, 1000, hoy, hoy))
    }

    private fun comp(id: String, estado: String, venc: String = "2026-09-15", cuota: Int? = 16, monto: Double = 310000.0) =
        CompromisoEntity(id, id, "UF-02", "EC-01", "Cuota pie departamento", cuota, 36, monto, venc, estado)

    @Test
    fun duplicadoHU11CA2() {
        val existentes = listOf(comp("A", "Pendiente"))
        assertTrue(R.esDuplicado(existentes, "UF-02", "EC-01", "cuota pie departamento ", 16))
        assertFalse("otra cuota", R.esDuplicado(existentes, "UF-02", "EC-01", "Cuota pie departamento", 17))
        // FA-12: para "modificar" se anula y se registra de nuevo -> el anulado no bloquea
        assertFalse(R.esDuplicado(listOf(comp("A", "Anulado")), "UF-02", "EC-01", "Cuota pie departamento", 16))
    }

    // ---------------- RN-21: anulación (HU-11 CA-3, CA-4)

    @Test
    fun anulacion() {
        assertNull(R.validarAnulacion(comp("A", "En revisión"), "EC-01", "Pagado directamente a la empresa", ""))
        assertNotNull("pagado nunca", R.validarAnulacion(comp("A", "Pagado"), "EC-01", "Repactación", ""))
        assertNotNull("otra empresa", R.validarAnulacion(comp("A", "Pendiente"), "EC-07", "Repactación", ""))
        assertNotNull("Otro sin comentario", R.validarAnulacion(comp("A", "Vencido"), "EC-01", "Otro", " "))
        assertNull(R.validarAnulacion(comp("A", "Vencido"), "EC-01", "Otro", "Acuerdo con la persona"))
    }

    // ---------------- RN-24: tasa de cobranza

    @Test
    fun ejemploRN24_104de118_es88coma1() {
        val compromisos = (1..120).map { i ->
            val estado = when {
                i <= 2 -> "Anulado"
                i <= 106 -> "Pagado"
                else -> "Vencido"
            }
            comp("C$i", estado, venc = "2026-09-%02d".format(1 + i % 28), cuota = i, monto = 300000.0)
        }
        val pagos = compromisos.filter { it.estado == "Pagado" }.map {
            PagoEntity("P${it.idCompromiso}", "P${it.idCompromiso}", it.idCompromiso, "2026-09-01 10:00", 300000.0)
        }
        val r = R.resumenDelMes(compromisos, pagos, "2026-09")
        assertEquals(120, r.total)
        assertEquals(2, r.anulados)
        assertEquals(104, r.pagados)
        assertEquals(14, r.vencidos)
        assertEquals(88.1, r.tasa!!, 0.0001)
        assertEquals(104 * 300000.0, r.montoRecaudado, 0.001)
        // otro mes: sin compromisos
        assertNull(R.resumenDelMes(compromisos, pagos, "2026-10").tasa)
    }

    @Test
    fun hu12CA1_AltosDelMaipoSeptiembre() {
        // Cartera sintética + el compromiso real de septiembre de Altos del Maipo (CP-2026-0106, vencido)
        val compromisos = CobranzaSeptiembreSintetica.compromisos.filter { it.idEmpresa == "EC-01" } +
            CompromisoEntity("CP-2026-0106", "CP-2026-0106", "UF-02", "EC-01", "Cuota pie departamento", 14, 36, 310000.0, "2026-09-30", "Vencido", 8500.0)
        val ids = compromisos.map { it.idCompromiso }.toSet()
        val pagos = CobranzaSeptiembreSintetica.pagos.filter { it.idCompromiso in ids }

        val r = R.resumenDelMes(compromisos, pagos, "2026-09")

        assertEquals(120, r.total)
        assertEquals(2, r.anulados)
        assertEquals(104, r.pagados)
        assertEquals(14, r.vencidos)
        assertEquals(32_476_000.0, r.montoRecaudado, 0.001)
        assertEquals(88.1, r.tasa!!, 0.0001)
    }

    @Test
    fun personasSinteticasTienenRutValidoYNoAparecenEnLaSesion() {
        CobranzaSeptiembreSintetica.usuarios.forEach {
            assertTrue(it.rut, Rut.esValido(it.rut))
            assertFalse(it.activo)
        }
    }
}
