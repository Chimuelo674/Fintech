package cl.duocuc.pagoflex.usuarios.exception;

/** Se responde con 401 Unauthorized cuando el RUT o la clave no son correctos. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) { super(mensaje); }
}
