package cl.duocuc.pagoflex.pagos.service;

import cl.duocuc.pagoflex.pagos.exception.ConflictoException;
import cl.duocuc.pagoflex.pagos.exception.NoEncontradoException;
import cl.duocuc.pagoflex.pagos.model.Compromiso;
import cl.duocuc.pagoflex.pagos.model.Pago;
import cl.duocuc.pagoflex.pagos.repository.CompromisoRepository;
import cl.duocuc.pagoflex.pagos.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagoServiceTest {

    private PagoRepository pagoRepo;
    private CompromisoRepository compromisoRepo;
    private PagoService service;

    private Compromiso compromiso(String estado) {
        Compromiso c = new Compromiso();
        c.setIdCompromiso("CP-1");
        c.setFolio("CP-1");
        c.setIdUsuarioFinal("UF-01");
        c.setIdEmpresa("EC-01");
        c.setConcepto("Cuota");
        c.setMonto(10000.0);
        c.setFechaVencimiento("2026-10-10");
        c.setEstado(estado);
        return c;
    }

    private Pago pago() {
        Pago p = new Pago();
        p.setIdPago("CPR-000100");
        p.setNumeroComprobante("CPR-000100");
        p.setIdCompromiso("CP-1");
        p.setFechaHora("2026-10-06 12:00");
        p.setMontoPagado(10000.0);
        return p;
    }

    @BeforeEach
    void setUp() {
        pagoRepo = mock(PagoRepository.class);
        compromisoRepo = mock(CompromisoRepository.class);
        service = new PagoService(pagoRepo, compromisoRepo);
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void pagarDejaElCompromisoComoPagado() {
        when(compromisoRepo.findById("CP-1")).thenReturn(Optional.of(compromiso("Pendiente")));
        Pago guardado = service.crear(pago());

        assertEquals("App", guardado.getCanal());
        ArgumentCaptor<Compromiso> captor = ArgumentCaptor.forClass(Compromiso.class);
        verify(compromisoRepo).save(captor.capture());
        assertEquals("Pagado", captor.getValue().getEstado());
    }

    @Test
    void noSePuedePagarDosVeces() {
        when(compromisoRepo.findById("CP-1")).thenReturn(Optional.of(compromiso("Pendiente")));
        when(pagoRepo.existsByIdCompromiso("CP-1")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.crear(pago()));
        verify(pagoRepo, never()).save(any());
    }

    @Test
    void noSePuedePagarUnCompromisoAnulado() {
        when(compromisoRepo.findById("CP-1")).thenReturn(Optional.of(compromiso("Anulado")));
        assertThrows(ConflictoException.class, () -> service.crear(pago()));
    }

    @Test
    void pagarUnCompromisoInexistenteDa404() {
        when(compromisoRepo.findById("CP-1")).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> service.crear(pago()));
    }

    @Test
    void listarPorUsuarioBuscaLosPagosDeSusCompromisos() {
        when(compromisoRepo.findByIdUsuarioFinalOrderByFechaVencimientoAsc("UF-01"))
                .thenReturn(List.of(compromiso("Pagado")));
        when(pagoRepo.findByIdCompromisoIn(List.of("CP-1"))).thenReturn(List.of(pago()));
        assertEquals(1, service.listar("UF-01").size());
    }

    @Test
    void usuarioSinCompromisosNoTienePagos() {
        when(compromisoRepo.findByIdUsuarioFinalOrderByFechaVencimientoAsc("UF-99")).thenReturn(List.of());
        assertTrue(service.listar("UF-99").isEmpty());
        verify(pagoRepo, never()).findByIdCompromisoIn(any());
    }
}
