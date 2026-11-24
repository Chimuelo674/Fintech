package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.domain.LinkPago
import cl.duocuc.pagoflex.domain.ReglasAcceso
import cl.duocuc.pagoflex.domain.SimuladorCostos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuncionesBancoTest {

    // ---------- Ingreso con RUT y clave ----------

    @Test
    fun formateaRutMientrasSeEscribe() {
        assertEquals("1", ReglasAcceso.formatearMientrasEscribe("1"))
        assertEquals("15.834.207-3", ReglasAcceso.formatearMientrasEscribe("158342073"))
        assertEquals("16.248.317-K", ReglasAcceso.formatearMientrasEscribe("16248317k"))
        assertEquals("15.834.207-3", ReglasAcceso.formatearMientrasEscribe("15.834.207-3"))
    }

    @Test
    fun escribirRutDigitoPorDigitoNoLoDesordena() {
        // Simula el teclado: cada tecla se agrega al final del texto ya formateado.
        var campo = ""
        for (tecla in "158342073") {
            campo = ReglasAcceso.formatearMientrasEscribe(campo + tecla)
        }
        assertEquals("15.834.207-3", campo)

        // Y al borrar con la tecla de retroceso se va acortando sin mezclarse.
        campo = ReglasAcceso.formatearMientrasEscribe(campo.dropLast(1))
        assertEquals("1.583.420-7", campo)
        campo = ReglasAcceso.formatearMientrasEscribe(campo.dropLast(1))
        assertEquals("158.342-0", campo)
    }

    @Test
    fun laKSoloPuedeIrAlFinal() {
        assertEquals("1-2", ReglasAcceso.formatearMientrasEscribe("K12"))
        assertEquals("1-K", ReglasAcceso.formatearMientrasEscribe("1K"))
    }

    @Test
    fun noAceptaMasDeNueveCaracteres() {
        assertEquals("15.834.207-3", ReglasAcceso.formatearMientrasEscribe("1583420731111"))
    }

    @Test
    fun validaFormularioDeIngreso() {
        assertEquals("Ingresa tu RUT.", ReglasAcceso.validarFormulario("", "1234"))
        assertEquals("El RUT ingresado no es válido.", ReglasAcceso.validarFormulario("15.834.207-4", "1234"))
        assertEquals("Tu clave debe tener 4 dígitos.", ReglasAcceso.validarFormulario("15.834.207-3", "12"))
        assertEquals("Tu clave debe tener 4 dígitos.", ReglasAcceso.validarFormulario("15.834.207-3", "12a4"))
        assertNull(ReglasAcceso.validarFormulario("15.834.207-3", "1234"))
    }

    @Test
    fun enmascaraRutComoEnComprobante() {
        assertEquals("15.***.***-3", ReglasAcceso.enmascararRut("15.834.207-3"))
        assertEquals("6.***.***-5", ReglasAcceso.enmascararRut("68935415"))
    }

    @Test
    fun bloqueaDespuesDeTresIntentosFallidos() {
        val control = ReglasAcceso.ControlIntentos()
        assertEquals(2, control.registrarFallo(0))
        assertEquals(1, control.registrarFallo(1_000))
        assertEquals(0, control.registrarFallo(2_000))
        assertTrue(control.estaBloqueado(2_000))
        assertEquals(30, control.segundosRestantes(2_000))
        assertEquals(1, control.segundosRestantes(31_500))
        assertFalse(control.estaBloqueado(32_000))
    }

    @Test
    fun reiniciarQuitaElBloqueo() {
        val control = ReglasAcceso.ControlIntentos()
        repeat(3) { control.registrarFallo(0) }
        control.reiniciar()
        assertFalse(control.estaBloqueado(1))
        assertEquals(0, control.fallidos)
    }

    @Test
    fun saludoSegunLaHora() {
        assertEquals("Buenos días", ReglasAcceso.saludo(8))
        assertEquals("Buenas tardes", ReglasAcceso.saludo(15))
        assertEquals("Buenas noches", ReglasAcceso.saludo(22))
        assertEquals("Buenas noches", ReglasAcceso.saludo(3))
    }

    // ---------- Simulador de costos ----------

    @Test
    fun simuladorComparaPagoFlexConBanco() {
        val r = SimuladorCostos.calcular(1_000_000.0)
        assertEquals(30_000.0, r.costoPagoFlex, 0.01)
        assertEquals(29_500.0, r.comisionBanco, 0.01)
        assertEquals(5_605.0, r.ivaBanco, 0.01)
        assertEquals(6_500.0, r.mantencionBanco, 0.01)
        assertEquals(41_605.0, r.costoBanco, 0.01)
        assertEquals(11_605.0, r.ahorroMensual, 0.01)
        assertEquals(139_260.0, r.ahorroAnual, 0.01)
        assertTrue(r.pagoFlexConviene)
    }

    @Test
    fun simuladorSinVentasNoCobraNada() {
        val r = SimuladorCostos.calcular(0.0)
        assertEquals(0.0, r.costoBanco, 0.0)
        assertEquals(0.0, r.costoPagoFlex, 0.0)
        assertFalse(r.pagoFlexConviene)
    }

    @Test
    fun simuladorLeeMontosEscritosPorElUsuario() {
        assertEquals(1_500_000.0, SimuladorCostos.leerMonto("$ 1.500.000")!!, 0.0)
        assertNull(SimuladorCostos.leerMonto(""))
        assertNull(SimuladorCostos.leerMonto("abc"))
    }

    // ---------- Links de pago y QR ----------

    @Test
    fun linkDePagoIdaYVuelta() {
        val link = LinkPago.generar("CP-S-EC01-12", 310_000.4)
        assertEquals("https://pagoflex.com.ar/pagar/CP-S-EC01-12?monto=310000", link)
        val datos = LinkPago.leer(link)
        assertNotNull(datos)
        assertEquals("CP-S-EC01-12", datos!!.folio)
        assertEquals(310_000L, datos.monto)
    }

    @Test
    fun linkDePagoCodificaCaracteresEspeciales() {
        val link = LinkPago.generar("FOLIO 1/2", 1000.0)
        assertEquals("FOLIO 1/2", LinkPago.leer(link)!!.folio)
    }

    @Test
    fun rechazaLinksQueNoSonDePagoFlex() {
        assertNull(LinkPago.leer("https://otro.sitio/pagar/X?monto=10"))
        assertNull(LinkPago.leer("https://pagoflex.com.ar/pagar/X"))
        assertNull(LinkPago.leer("https://pagoflex.com.ar/pagar/X?monto=-5"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun noGeneraLinkConMontoCero() {
        LinkPago.generar("X", 0.0)
    }
}
