package de.onyxmoon.modsync.api;

/**
 * Exception thrown when a mod URL cannot be parsed.
 */
public class InvalidModUrlException extends Exception {

    private final String url;

    public InvalidModUrlException(String url) {
        super("Invalid mod URL: " + url);
        this.url = url;
    }

    public InvalidModUrlException(String url, String message) {
        super(message + ": " + url);
        this.url = url;
    }

    public InvalidModUrlException(String url, Throwable cause) {
        super("Invalid mod URL: " + url, cause);
        this.url = url;
    }

    /**
     * Get the URL that failed to parse.
     */
    public String getUrl() {
        return url;
    }
}