package de.onyxmoon.modsync.provider.cfwidget.client;

/**
 * Custom exception for CFWidget API errors.
 */
public class CfWidgetApiException extends RuntimeException {
    private final int statusCode;

    public CfWidgetApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CfWidgetApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
