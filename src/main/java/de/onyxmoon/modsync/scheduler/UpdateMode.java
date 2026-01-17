package de.onyxmoon.modsync.scheduler;

/**
 * Enum representing different update modes for mod list synchronization.
 */
public enum UpdateMode {
    /**
     * Manual updates only (via command)
     */
    MANUAL,

    /**
     * Automatic periodic updates at scheduled intervals
     */
    PERIODIC,

    /**
     * Update on server startup
     */
    STARTUP,

    /**
     * Both periodic and startup updates
     */
    BOTH
}