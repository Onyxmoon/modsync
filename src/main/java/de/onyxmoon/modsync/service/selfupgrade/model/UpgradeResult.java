package de.onyxmoon.modsync.service.selfupgrade.model;

/**
 * Result of an update operation.
 *
 * @param success         true if the update was successful
 * @param message         description of the result
 * @param restartRequired true if a server restart is needed to complete the update
 */
public record UpgradeResult(
        boolean success,
        String message,
        boolean restartRequired
) {
}