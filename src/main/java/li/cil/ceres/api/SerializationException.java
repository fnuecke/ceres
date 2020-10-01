package li.cil.ceres.api;

public final class SerializationException extends Exception {
    public SerializationException() {
    }

    public SerializationException(final String message) {
        super(message);
    }

    public SerializationException(final Throwable cause) {
        super(cause);
    }

    public SerializationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
