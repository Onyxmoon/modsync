package de.onyxmoon.modsync.provider.modtale.client;

/**
 * Custom exception for Modtale API errors.
 */
public class ModtaleApiException extends RuntimeException {
    private final int statusCode;

    public ModtaleApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ModtaleApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
