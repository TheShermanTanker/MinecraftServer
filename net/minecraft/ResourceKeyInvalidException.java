package net.minecraft;

public class ResourceKeyInvalidException extends RuntimeException {
    public ResourceKeyInvalidException(String message) {
        super(message);
    }

    public ResourceKeyInvalidException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
