package de.onyxmoon.modsync.provider.curseforge.client;

/**
 * Custom exception for CurseForge API errors.
 */
public class CurseForgeApiException extends RuntimeException {
    private final int statusCode;

    public CurseForgeApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CurseForgeApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}