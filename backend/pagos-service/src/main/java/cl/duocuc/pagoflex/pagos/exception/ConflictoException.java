package cl.duocuc.pagoflex.pagos.exception;

/** Se responde con 409 Conflict (por ejemplo, un registro duplicado). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) { super(mensaje); }
}
