package de.onyxmoon.modsync.storage.migration.lock;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.storage.migration.Migration;
import de.onyxmoon.modsync.storage.model.LockFile;
import de.onyxmoon.modsync.storage.model.ModListFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Main migrator that orchestrates all lock file migrations.
 * Follows the chain of responsibility pattern for sequential migrations.
 */
public class StorageFileMigrator {
    
    private final List<Migration> migrations;
    private final HytaleLogger logger;
    
    /**
     * Creates a new migrator with the given logger.
     * 
     * @param logger Logger for migration messages
     */
    public StorageFileMigrator(HytaleLogger logger) {
        this.logger = logger;
        this.migrations = new ArrayList<>();
        
        // Register all known migrations
        registerMigrations();
    }
    
    /**
     * Registers all available migrations.
     */
    private void registerMigrations() {
        // Migration 1→2: Local version extraction
        migrations.add(new V1toV2Migration(logger));
        
        // Migration 2->3: SemVer normalization
        migrations.add(new V2toV3Migration(logger));
    }
    
    /**
     * Migrates a lock file to the current schema version.
     * 
     * @param lockFile The lock file to migrate
     * @return Migrated lock file with current schema version
     */
    public LockFile migrate(LockFile lockFile) {
        int currentVersion = lockFile.getVersion();
        int targetVersion = getTargetVersion();
        
        if (currentVersion >= targetVersion) {
            logger.atInfo().log("Lock file is already at current version %d", targetVersion);
            return lockFile;
        }

        logger.atInfo().log("Starting lock file migration from version %d to %d",
                currentVersion, targetVersion);

        LockFile migrated = lockFile;
        
        // Apply migrations sequentially
        for (Migration migration : migrations) {
            if (currentVersion < migration.getTargetVersion()) {
                migrated = migration.migrate(migrated);
                currentVersion = migrated.getVersion();
            }
        }

        logger.atInfo().log("Lock file migration complete. Final version: %d",
                migrated.getVersion());
                
        return migrated;
    }

    /**
     * Migrates a ModList file to the current schema version.
     *
     * @param modListFile The ModListfile to migrate
     * @return Migrated ModList file with current schema version
     */
    public ModListFile migrate(ModListFile modListFile) {
        int currentVersion = modListFile.getVersion();
        int targetVersion = getTargetVersion();

        if (currentVersion >= targetVersion) {
            logger.atInfo().log("ModList file is already at current version %d", targetVersion);
            return modListFile;
        }

        logger.atInfo().log("Starting ModList file migration from version %d to %d",
                currentVersion, targetVersion);

        ModListFile migrated = modListFile;

        // Apply migrations sequentially
        for (Migration migration : migrations) {
            if (currentVersion < migration.getTargetVersion()) {
                migrated = migration.migrate(migrated);
                currentVersion = migrated.getVersion();
            }
        }

        logger.atInfo().log("ModList file migration complete. Final version: %d",
                migrated.getVersion());

        return migrated;
    }
    
    /**
     * Gets the target schema version (latest version).
     * 
     * @return Target schema version
     */
    private int getTargetVersion() {
        if (migrations.isEmpty()) {
            return 1; // No migrations available
        }
        return migrations.getLast().getTargetVersion();
    }
}
