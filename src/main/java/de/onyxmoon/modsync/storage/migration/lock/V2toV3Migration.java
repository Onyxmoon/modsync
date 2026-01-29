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
 * Migration from version 2 to 3: Normalize installed version numbers to SemVer when possible.
 */
public class V2toV3Migration implements Migration {

    private final HytaleLogger logger;

    public V2toV3Migration(HytaleLogger logger) {
        this.logger = logger;
    }

    @Override
    public int getTargetVersion() {
        return 3;
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
        logger.atInfo().log("Running migration 2->3: SemVer normalization");

        Map<String, LockFile.LockedInstallation> migratedInstallations = new HashMap<>();
        int updatedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (Map.Entry<String, LockFile.LockedInstallation> entry : lockFile.getInstallations().entrySet()) {
            LockFile.LockedInstallation installation = entry.getValue();
            try {
                String currentVersion = installation.getInstalledVersionNumber();
                if (VersionExtractor.parseSemver(currentVersion).isPresent()) {
                    skippedCount++;
                    migratedInstallations.put(entry.getKey(), installation);
                    continue;
                }

                Path modFile = Paths.get(installation.getFilePath());
                if (!Files.exists(modFile)) {
                    skippedCount++;
                    migratedInstallations.put(entry.getKey(), installation);
                    continue;
                }

                String fileName = installation.getFileName();
                if (fileName == null || fileName.isBlank()) {
                    fileName = modFile.getFileName().toString();
                }
                String replacementVersion = ManifestReader.readVersion(modFile)
                        .map(Object::toString)
                        .orElse(VersionExtractor.baseFilename(fileName));
                if (replacementVersion == null || replacementVersion.isBlank() ||
                        replacementVersion.equals(currentVersion)) {
                    skippedCount++;
                    migratedInstallations.put(entry.getKey(), installation);
                    continue;
                }

                updatedCount++;
                migratedInstallations.put(entry.getKey(), new LockFile.LockedInstallation(
                        installation.getIdentifier(),
                        installation.getInstalledVersionId(),
                        replacementVersion,
                        installation.getFilePath(),
                        installation.getFileName(),
                        installation.getFileSize(),
                        installation.getFileHash(),
                        installation.getInstalledAt(),
                        installation.getLastChecked()
                ));
            } catch (Exception e) {
                errorCount++;
                logger.atWarning().log("Failed to normalize installation: %s - %s",
                        installation.getFileName(), e.getMessage());
                migratedInstallations.put(entry.getKey(), installation);
            }
        }

        logger.atInfo().log("Migration 2->3 complete. Updated: %d, Skipped: %d, Errors: %d",
                updatedCount, skippedCount, errorCount);

        return new LockFile(getTargetVersion(), Instant.now(), migratedInstallations);
    }
}
