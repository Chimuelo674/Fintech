package cl.duocuc.pagoflex.pagos.controller;

import cl.duocuc.pagoflex.pagos.model.Reporte;
import cl.duocuc.pagoflex.pagos.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CRUD de reportes: /api/reportes */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    /** GET /api/reportes?usuario=UF-01 o ?estado=Abierto (filtros opcionales) */
    @GetMapping
    public List<Reporte> listar(@RequestParam(required = false) String usuario,
                              @RequestParam(required = false) String estado) {
        return service.listar(usuario, estado);
    }

    @GetMapping("/{id}")
    public Reporte obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Reporte> crear(@Valid @RequestBody Reporte reporte) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(reporte));
    }

    /** Carga inicial: la app envía sus datos de prueba cuando la base de datos está vacía. */
    @PostMapping("/lote")
    public Map<String, Integer> crearLote(@RequestBody List<Reporte> lista) {
        return Map.of("insertados", service.crearLote(lista));
    }

    @PutMapping("/{id}")
    public Reporte actualizar(@PathVariable String id, @Valid @RequestBody Reporte reporte) {
        return service.actualizar(id, reporte);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
