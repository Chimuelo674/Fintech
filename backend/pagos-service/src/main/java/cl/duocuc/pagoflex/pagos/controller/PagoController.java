package cl.duocuc.pagoflex.pagos.controller;

import cl.duocuc.pagoflex.pagos.model.Pago;
import cl.duocuc.pagoflex.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CRUD de pagos: /api/pagos */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    /** GET /api/pagos?usuario=UF-01 (filtro opcional). POST marca el compromiso como Pagado. */
    @GetMapping
    public List<Pago> listar(@RequestParam(required = false) String usuario) {
        return service.listar(usuario);
    }

    @GetMapping("/{id}")
    public Pago obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Pago> crear(@Valid @RequestBody Pago pago) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(pago));
    }

    /** Carga inicial: la app envía sus datos de prueba cuando la base de datos está vacía. */
    @PostMapping("/lote")
    public Map<String, Integer> crearLote(@RequestBody List<Pago> lista) {
        return Map.of("insertados", service.crearLote(lista));
    }

    @PutMapping("/{id}")
    public Pago actualizar(@PathVariable String id, @Valid @RequestBody Pago pago) {
        return service.actualizar(id, pago);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
