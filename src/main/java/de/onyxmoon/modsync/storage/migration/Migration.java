package de.onyxmoon.modsync.storage.migration;

import de.onyxmoon.modsync.storage.model.LockFile;
import de.onyxmoon.modsync.storage.model.ModListFile;

/**
 * Interface for individual migrations.
 * Each migration implements this interface to provide version-specific migration logic.
 */
public interface Migration {
    
    /**
     * Gets the target version this migration migrates to.
     * 
     * @return Target schema version
     */
    int getTargetVersion();

    /**
     * Migrates a ModList file to the target version.
     *
     * @param modListFile ModList file to migrate (must be at source version)
     * @return Migrated ModList file at target version
     */
    ModListFile migrate(ModListFile modListFile);
    
    /**
     * Migrates a lock file to the target version.
     * 
     * @param lockFile Lock file to migrate (must be at source version)
     * @return Migrated lock file at target version
     */
    LockFile migrate(LockFile lockFile);
}