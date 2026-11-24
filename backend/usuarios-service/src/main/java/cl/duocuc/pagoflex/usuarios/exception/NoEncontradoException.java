package cl.duocuc.pagoflex.usuarios.exception;

/** Se responde con 404 Not Found. */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String mensaje) { super(mensaje); }
}
