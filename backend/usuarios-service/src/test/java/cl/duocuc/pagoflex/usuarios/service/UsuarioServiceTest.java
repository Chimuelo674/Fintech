package cl.duocuc.pagoflex.usuarios.service;

import cl.duocuc.pagoflex.usuarios.exception.ConflictoException;
import cl.duocuc.pagoflex.usuarios.exception.CredencialesInvalidasException;
import cl.duocuc.pagoflex.usuarios.model.Usuario;
import cl.duocuc.pagoflex.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository repo;
    private UsuarioService service;

    private Usuario camila() {
        Usuario u = new Usuario();
        u.setIdUsuario("UF-01");
        u.setIdRol("USUARIO_FINAL");
        u.setRut("15.834.207-3");
        u.setNombres("Camila Andrea");
        u.setApellidos("Rojas Peña");
        u.setCorreo("crojas@correo.ejemplo");
        u.setActivo(true);
        u.setClaveHash(Seguridad.hashClave("1234"));
        return u;
    }

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioRepository.class);
        service = new UsuarioService(repo);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void loginConRutSinPuntosYClaveCorrecta() {
        when(repo.findAll()).thenReturn(List.of(camila()));
        Usuario u = service.login("158342073", "1234");
        assertEquals("UF-01", u.getIdUsuario());
    }

    @Test
    void loginConClaveIncorrectaLanza401() {
        when(repo.findAll()).thenReturn(List.of(camila()));
        assertThrows(CredencialesInvalidasException.class, () -> service.login("15.834.207-3", "9999"));
    }

    @Test
    void loginConRutInexistenteLanza401() {
        when(repo.findAll()).thenReturn(List.of(camila()));
        assertThrows(CredencialesInvalidasException.class, () -> service.login("11.111.111-1", "1234"));
    }

    @Test
    void loginDeCuentaDesactivadaLanza401() {
        Usuario inactivo = camila();
        inactivo.setActivo(false);
        when(repo.findAll()).thenReturn(List.of(inactivo));
        CredencialesInvalidasException e = assertThrows(CredencialesInvalidasException.class,
                () -> service.login("15.834.207-3", "1234"));
        assertTrue(e.getMessage().contains("desactivada"));
    }

    @Test
    void crearAsignaClaveInicialHasheada() {
        when(repo.existsById("UF-01")).thenReturn(false);
        when(repo.findAll()).thenReturn(List.of());
        when(repo.existsByCorreoIgnoreCase(any())).thenReturn(false);
        Usuario nuevo = camila();
        nuevo.setClaveHash(null);
        Usuario guardado = service.crear(nuevo);
        assertEquals(Seguridad.hashClave("1234"), guardado.getClaveHash());
        assertNotEquals("1234", guardado.getClaveHash());
    }

    @Test
    void crearConRutRepetidoLanzaConflicto() {
        when(repo.existsById(any())).thenReturn(false);
        when(repo.findAll()).thenReturn(List.of(camila()));
        Usuario otro = camila();
        otro.setIdUsuario("UF-99");
        assertThrows(ConflictoException.class, () -> service.crear(otro));
    }

    @Test
    void loteSoloInsertaLosQueNoExisten() {
        Usuario a = camila();
        Usuario b = camila();
        b.setIdUsuario("UF-02");
        when(repo.existsById("UF-01")).thenReturn(true);
        when(repo.existsById("UF-02")).thenReturn(false);
        assertEquals(1, service.crearLote(List.of(a, b)));
        verify(repo, times(1)).save(any(Usuario.class));
    }
}
