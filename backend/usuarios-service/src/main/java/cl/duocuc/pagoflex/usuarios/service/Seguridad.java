package cl.duocuc.pagoflex.usuarios.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Utilidades para comparar RUT y guardar claves sin dejarlas en texto plano. */
public final class Seguridad {

    private Seguridad() { }

    /** "15.834.207-3" y "158342073" se consideran el mismo RUT. */
    public static String limpiarRut(String rut) {
        if (rut == null) return "";
        return rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
    }

    /** Hash SHA-256 en hexadecimal (64 caracteres). */
    public static String hashClave(String clave) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
