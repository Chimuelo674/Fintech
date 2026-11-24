package cl.duocuc.pagoflex.usuarios.controller;

import cl.duocuc.pagoflex.usuarios.model.Usuario;
import cl.duocuc.pagoflex.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CRUD de usuarios: /api/usuarios */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    /** GET /api/usuarios?rol=USUARIO_FINAL (el filtro por rol es opcional) */
    @GetMapping
    public List<Usuario> listar(@RequestParam(required = false) String rol) {
        return service.listar(rol);
    }

    @GetMapping("/{id}")
    public Usuario obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(usuario));
    }

    /** Carga inicial: la app envía sus usuarios de prueba cuando la base de datos está vacía. */
    @PostMapping("/lote")
    public Map<String, Integer> crearLote(@RequestBody List<Usuario> usuarios) {
        return Map.of("insertados", service.crearLote(usuarios));
    }

    @PutMapping("/{id}")
    public Usuario actualizar(@PathVariable String id, @Valid @RequestBody Usuario usuario) {
        return service.actualizar(id, usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
