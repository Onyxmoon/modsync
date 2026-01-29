package de.onyxmoon.modsync.storage.migration.lock;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.storage.migration.Migration;
import de.onyxmoon.modsync.storage.model.LockFile;
import de.onyxmoon.modsync.storage.model.ModListFile;
import de.onyxmoon.modsync.util.ManifestReader;
import de.onyxmoon.modsync.util.VersionExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Migration from version 1 to 2: Extracts local versions from mod files.
 * This ensures installedVersionNumber reflects the actual local file version.
 */
public class V1toV2Migration implements Migration {
    
    private final HytaleLogger logger;
    
    /**
     * Creates a new migration with the given logger.
     * 
     * @param logger Logger for migration messages
     */
    public V1toV2Migration(HytaleLogger logger) {
        this.logger = logger;
    }
    
    @Override
    public int getTargetVersion() {
        return 2;
    }

    @Override
    public ModListFile migrate(ModListFile modListFile) {
        return new ModListFile(getTargetVersion(),
                modListFile.getName(),
                modListFile.getCreatedAt(),
                Instant.now(),
                modListFile.getMods());
    }

    @Override
    public LockFile migrate(LockFile lockFile) {
        logger.atInfo().log("Running migration 1→2: Local version extraction");

        Map<String, LockFile.LockedInstallation> migratedInstallations = new HashMap<>();
        int updatedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (Map.Entry<String, LockFile.LockedInstallation> entry : lockFile.getInstallations().entrySet()) {
            LockFile.LockedInstallation installation = entry.getValue();
            
            try {
                LockFile.LockedInstallation migrated = migrateInstallation(installation);
                
                if (migrated != installation) {
                    updatedCount++;
                } else {
                    skippedCount++;
                }
                
                migratedInstallations.put(entry.getKey(), migrated);
            } catch (Exception e) {
                errorCount++;
                logger.atWarning().log("Failed to migrate installation: %s - %s",
                        installation.getFileName(), e.getMessage());
                // Keep original installation on error
                migratedInstallations.put(entry.getKey(), installation);
            }
        }

        logger.atInfo().log("Migration 1→2 complete. Updated: %d, Skipped: %d, Errors: %d",
                updatedCount, skippedCount, errorCount);

        return new LockFile(getTargetVersion(), Instant.now(), migratedInstallations);
    }
    
    /**
     * Migrates a single installation from version 1 to 2.
     * Extracts local version from file if needed.
     * 
     * @param installation Installation to migrate
     * @return Migrated installation, or original if no migration needed
     */
    private LockFile.LockedInstallation migrateInstallation(LockFile.LockedInstallation installation) {
        // Skip if already has a proper version
        String currentVersion = installation.getInstalledVersionNumber();
        if (shouldSkipMigration(currentVersion)) {
            logger.atFine().log("Skipping migration for %s (version: %s)",
                    installation.getFileName(), currentVersion);
            return installation;
        }

        // Extract version from file
        Path modFile = Paths.get(installation.getFilePath());
        if (!Files.exists(modFile)) {
            logger.atFine().log("Mod file not found for migration: %s", installation.getFileName());
            return installation;
        }

        String fileName = installation.getFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = modFile.getFileName().toString();
        }
        String localVersion = ManifestReader.readVersion(modFile)
                .map(Object::toString)
                .orElse(VersionExtractor.baseFilename(fileName));
        if (localVersion == null || localVersion.isBlank()) {
            logger.atFine().log("No version found in file: %s", installation.getFileName());
            return installation;
        }

        logger.atFine().log("Migrating %s: %s → %s",
                installation.getFileName(), currentVersion, localVersion);

        // Create migrated installation with local version
        return new LockFile.LockedInstallation(
                installation.getIdentifier(),
                installation.getInstalledVersionId(),  // Keep provider ID for updates
                localVersion,                          // Use local version for display
                installation.getFilePath(),
                installation.getFileName(),
                installation.getFileSize(),
                installation.getFileHash(),
                installation.getInstalledAt(),
                installation.getLastChecked()
        );
    }
    
    /**
     * Determines if an installation should be skipped during migration.
     * 
     * @param version Current version number
     * @return true if migration should be skipped
     */
    private boolean shouldSkipMigration(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        return VersionExtractor.parseSemver(version).isPresent();
    }
}
