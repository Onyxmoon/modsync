package de.onyxmoon.modsync.service.selfupgrade.model;

/**
 * Result of an update check operation.
 *
 * @param status         the status of the check
 * @param currentVersion the currently installed version
 * @param latestVersion  the latest available version (may be null on error)
 * @param release        the GitHub release info (may be null on error)
 * @param message        additional message (e.g., error details)
 */
public record UpgradeCheckResult(
        UpgradeStatus status,
        SemanticVersion currentVersion,
        SemanticVersion latestVersion,
        GitHubRelease release,
        String message
) {
    /**
     * Returns true if an update is available.
     */
    public boolean hasUpdate() {
        return status == UpgradeStatus.UPGRADE_AVAILABLE;
    }
}