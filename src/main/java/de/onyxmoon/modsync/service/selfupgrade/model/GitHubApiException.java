package de.onyxmoon.modsync.service.selfupgrade.model;

/**
 * Exception thrown when GitHub API calls fail.
 */
public class GitHubApiException extends RuntimeException {
    private final int statusCode;

    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}