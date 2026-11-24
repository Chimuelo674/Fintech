package cl.duocuc.pagoflex.usuarios.controller;

import cl.duocuc.pagoflex.usuarios.model.Empresa;
import cl.duocuc.pagoflex.usuarios.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** CRUD de empresas: /api/empresas */
@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Empresa> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Empresa obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Empresa> crear(@Valid @RequestBody Empresa empresa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(empresa));
    }

    @PostMapping("/lote")
    public Map<String, Integer> crearLote(@RequestBody List<Empresa> empresas) {
        return Map.of("insertados", service.crearLote(empresas));
    }

    @PutMapping("/{id}")
    public Empresa actualizar(@PathVariable String id, @Valid @RequestBody Empresa empresa) {
        return service.actualizar(id, empresa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
