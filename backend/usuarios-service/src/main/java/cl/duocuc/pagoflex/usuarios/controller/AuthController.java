package cl.duocuc.pagoflex.usuarios.controller;

import cl.duocuc.pagoflex.usuarios.model.Usuario;
import cl.duocuc.pagoflex.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

/** Inicio de sesión con RUT y clave: POST /api/auth/login */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String rut, @NotBlank String clave) { }

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    /** 200 con el usuario si los datos son correctos; 401 con {"mensaje": "..."} si no. */
    @PostMapping("/login")
    public Usuario login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.rut(), request.clave());
    }
}
