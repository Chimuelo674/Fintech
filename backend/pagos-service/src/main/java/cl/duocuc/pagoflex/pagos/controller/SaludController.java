package cl.duocuc.pagoflex.pagos.controller;

import cl.duocuc.pagoflex.pagos.repository.CompromisoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** GET /api/salud: la app lo usa para saber si el servidor está disponible. */
@RestController
public class SaludController {

    private final CompromisoRepository compromisos;

    public SaludController(CompromisoRepository compromisos) {
        this.compromisos = compromisos;
    }

    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        return Map.of("servicio", "pagos-service", "estado", "OK", "compromisos", compromisos.count());
    }
}
