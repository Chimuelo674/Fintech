package cl.duocuc.pagoflex.usuarios.controller;

import cl.duocuc.pagoflex.usuarios.service.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** GET /api/salud: la app lo usa para saber si el servidor está disponible. */
@RestController
public class SaludController {

    private final UsuarioService service;

    public SaludController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        return Map.of("servicio", "usuarios-service", "estado", "OK", "usuarios", service.contar());
    }
}
