package cl.duocuc.pagoflex.pagos.controller;

import cl.duocuc.pagoflex.pagos.model.Compromiso;
import cl.duocuc.pagoflex.pagos.service.CompromisoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CRUD de compromisos: /api/compromisos */
@RestController
@RequestMapping("/api/compromisos")
public class CompromisoController {

    private final CompromisoService service;

    public CompromisoController(CompromisoService service) {
        this.service = service;
    }

    /** GET /api/compromisos?usuario=UF-01 o ?empresa=EC-01 (filtros opcionales) */
    @GetMapping
    public List<Compromiso> listar(@RequestParam(required = false) String usuario,
                                   @RequestParam(required = false) String empresa) {
        return service.listar(usuario, empresa);
    }

    @GetMapping("/{id}")
    public Compromiso obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Compromiso> crear(@Valid @RequestBody Compromiso compromiso) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(compromiso));
    }

    /** Carga inicial: la app envía sus datos de prueba cuando la base de datos está vacía. */
    @PostMapping("/lote")
    public Map<String, Integer> crearLote(@RequestBody List<Compromiso> lista) {
        return Map.of("insertados", service.crearLote(lista));
    }

    @PutMapping("/{id}")
    public Compromiso actualizar(@PathVariable String id, @Valid @RequestBody Compromiso compromiso) {
        return service.actualizar(id, compromiso);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
