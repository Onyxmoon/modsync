package de.onyxmoon.modsync.service.selfupgrade.model;

/**
 * Status of an update check.
 */
public enum UpgradeStatus {
    /**
     * The current version is up to date.
     */
    UP_TO_DATE,

    /**
     * A newer version is available.
     */
    UPGRADE_AVAILABLE,

    /**
     * An error occurred during the check.
     */
    ERROR
}