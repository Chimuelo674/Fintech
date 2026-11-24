package cl.duocuc.pagoflex.usuarios.service;

import cl.duocuc.pagoflex.usuarios.exception.ConflictoException;
import cl.duocuc.pagoflex.usuarios.exception.CredencialesInvalidasException;
import cl.duocuc.pagoflex.usuarios.exception.NoEncontradoException;
import cl.duocuc.pagoflex.usuarios.model.Usuario;
import cl.duocuc.pagoflex.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    /** Clave con la que se crean las cuentas en esta versión de prueba. */
    public static final String CLAVE_INICIAL = "1234";

    private final UsuarioRepository repo;

    public UsuarioService(UsuarioRepository repo) {
        this.repo = repo;
    }

    public List<Usuario> listar(String rol) {
        return (rol == null || rol.isBlank()) ? repo.findAll() : repo.findByIdRol(rol);
    }

    public Usuario obtener(String id) {
        return repo.findById(id).orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id + "."));
    }

    public Optional<Usuario> buscarPorRut(String rut) {
        String buscado = Seguridad.limpiarRut(rut);
        return repo.findAll().stream()
                .filter(u -> Seguridad.limpiarRut(u.getRut()).equals(buscado))
                .findFirst();
    }

    @Transactional
    public Usuario crear(Usuario usuario) {
        if (repo.existsById(usuario.getIdUsuario())) {
            throw new ConflictoException("Ya existe un usuario con el id " + usuario.getIdUsuario() + ".");
        }
        if (buscarPorRut(usuario.getRut()).isPresent()) {
            throw new ConflictoException("Ya existe un usuario con el RUT " + usuario.getRut() + ".");
        }
        if (repo.existsByCorreoIgnoreCase(usuario.getCorreo())) {
            throw new ConflictoException("El correo " + usuario.getCorreo() + " ya está registrado.");
        }
        usuario.setClaveHash(Seguridad.hashClave(CLAVE_INICIAL));
        return repo.save(usuario);
    }

    /** Carga inicial desde la app: guarda solo los que no existen y devuelve cuántos agregó. */
    @Transactional
    public int crearLote(List<Usuario> usuarios) {
        int insertados = 0;
        for (Usuario u : usuarios) {
            if (u.getIdUsuario() == null || repo.existsById(u.getIdUsuario())) continue;
            u.setClaveHash(Seguridad.hashClave(CLAVE_INICIAL));
            repo.save(u);
            insertados++;
        }
        return insertados;
    }

    @Transactional
    public Usuario actualizar(String id, Usuario datos) {
        Usuario actual = obtener(id);
        datos.setIdUsuario(id);
        datos.setClaveHash(actual.getClaveHash()); // la clave no se cambia por este endpoint
        return repo.save(datos);
    }

    @Transactional
    public void eliminar(String id) {
        if (!repo.existsById(id)) {
            throw new NoEncontradoException("No existe el usuario " + id + ".");
        }
        repo.deleteById(id);
    }

    /** Valida RUT y clave. Usa el mismo mensaje si el RUT no existe o la clave está mal. */
    public Usuario login(String rut, String clave) {
        Usuario usuario = buscarPorRut(rut)
                .orElseThrow(() -> new CredencialesInvalidasException("RUT o clave incorrectos."));
        String esperado = usuario.getClaveHash() != null
                ? usuario.getClaveHash()
                : Seguridad.hashClave(CLAVE_INICIAL);
        if (clave == null || !esperado.equals(Seguridad.hashClave(clave))) {
            throw new CredencialesInvalidasException("RUT o clave incorrectos.");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException("Tu cuenta está desactivada. Contacta a soporte.");
        }
        return usuario;
    }

    public long contar() {
        return repo.count();
    }
}
