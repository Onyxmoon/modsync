package de.onyxmoon.modsync.service;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.service.selfupgrade.GitHubClient;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeCheckResult;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeResult;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeStatus;
import de.onyxmoon.modsync.service.selfupgrade.model.GitHubAsset;
import de.onyxmoon.modsync.service.selfupgrade.model.GitHubRelease;
import de.onyxmoon.modsync.service.selfupgrade.model.SemanticVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing plugin self-upgrades.
 */
public class SelfUpgradeService {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private static final int CONNECT_TIMEOUT_SECONDS = 30;

    private final ModSync plugin;
    private final GitHubClient gitHubClient;
    private final HttpClient downloadClient;

    public SelfUpgradeService(ModSync plugin) {
        this.plugin = plugin;
        this.gitHubClient = new GitHubClient();
        this.downloadClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Gets the current installed version.
     *
     * @return the current SemanticVersion
     */
    public SemanticVersion getCurrentVersion() {
        return SemanticVersion.parse(BuildInfo.VERSION);
    }

    /**
     * Checks for available upgrades.
     *
     * @return CompletableFuture containing the check result
     */
    public CompletableFuture<UpgradeCheckResult> checkForUpgrade() {
        return gitHubClient.getLatestRelease()
                .thenApply(release -> {
                    SemanticVersion currentVersion = getCurrentVersion();
                    SemanticVersion latestVersion = SemanticVersion.tryParse(release.getVersion());

                    if (latestVersion == null) {
                        return new UpgradeCheckResult(
                                UpgradeStatus.ERROR,
                                currentVersion,
                                null,
                                null,
                                "Invalid version format in release: " + release.getTagName()
                        );
                    }

                    // Skip prereleases unless configured to include them
                    if (release.isPrerelease() &&
                            !plugin.getConfigStorage().getConfig().isIncludePrereleases()) {
                        return new UpgradeCheckResult(
                                UpgradeStatus.UP_TO_DATE,
                                currentVersion,
                                latestVersion,
                                release,
                                "Latest release is a prerelease (skipped)"
                        );
                    }

                    if (latestVersion.isNewerThan(currentVersion)) {
                        return new UpgradeCheckResult(
                                UpgradeStatus.UPGRADE_AVAILABLE,
                                currentVersion,
                                latestVersion,
                                release,
                                null
                        );
                    }

                    return new UpgradeCheckResult(
                            UpgradeStatus.UP_TO_DATE,
                            currentVersion,
                            latestVersion,
                            release,
                            null
                    );
                })
                .exceptionally(ex -> new UpgradeCheckResult(
                        UpgradeStatus.ERROR,
                        getCurrentVersion(),
                        null,
                        null,
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
                ));
    }

    /**
     * Downloads and installs the upgrade.
     * Old files are queued for deletion on next restart.
     *
     * @param release the GitHub release to install
     * @return CompletableFuture containing the upgrade result
     */
    public CompletableFuture<UpgradeResult> performUpgrade(GitHubRelease release) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path modsFolder = plugin.getDownloadService().getModsFolder();
                Path earlyPluginsFolder = plugin.getDownloadService().getEarlyPluginsFolder();

                Path currentPluginPath = findCurrentPluginPath(modsFolder);
                Path currentBootstrapPath = findCurrentBootstrapPath(earlyPluginsFolder);

                GitHubAsset mainJarAsset = release.findMainJar();
                GitHubAsset bootstrapAsset = release.findBootstrapJar();

                if (mainJarAsset == null) {
                    return new UpgradeResult(false, "Main plugin JAR not found in release assets", false);
                }

                boolean restartRequired = false;
                StringBuilder resultMessage = new StringBuilder();

                // Calculate target path BEFORE downloading to check if upgrade is needed
                Path newMainJarPath = modsFolder.resolve(mainJarAsset.getName());

                // Download new main plugin (only if different from current)
                if (currentPluginPath != null && currentPluginPath.equals(newMainJarPath)) {
                    resultMessage.append("Main plugin already at target version. ");
                } else {
                    Path newMainJar = downloadAsset(mainJarAsset, modsFolder);
                    resultMessage.append("Downloaded ").append(mainJarAsset.getName()).append(". ");

                    // Queue old main plugin for deletion
                    if (currentPluginPath != null && Files.exists(currentPluginPath)) {
                        queueForDeletion(currentPluginPath);
                    }
                    restartRequired = true;
                }

                // Bootstrap: Cannot auto-update due to file locking on Windows
                // (both old and new would be loaded, and old cannot delete itself)
                // Only show hint if bootstrap IS installed and an update is available
                if (bootstrapAsset != null && currentBootstrapPath != null) {
                    Path newBootstrapPath = earlyPluginsFolder.resolve(bootstrapAsset.getName());
                    if (!currentBootstrapPath.equals(newBootstrapPath)) {
                        resultMessage.append("Bootstrap update available (")
                                .append(currentBootstrapPath.getFileName())
                                .append(" -> ").append(bootstrapAsset.getName())
                                .append(") - manual update required. ");
                    }
                }

                resultMessage.append("Version ").append(release.getVersion()).append(" installed.");

                return new UpgradeResult(true, resultMessage.toString(), restartRequired);

            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to perform self-upgrade");
                return new UpgradeResult(false, "Upgrade failed: " + e.getMessage(), false);
            }
        });
    }

    /**
     * Finds the path to the currently running plugin JAR.
     */
    private Path findCurrentPluginPath(Path modsFolder) {
        try {
            return Files.list(modsFolder)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("modsync-") &&
                                !name.contains("bootstrap") &&
                                name.endsWith(".jar") &&
                                !name.contains("sources") &&
                                !name.contains("javadoc");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not scan mods folder: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Finds the path to the currently installed bootstrap JAR.
     */
    private Path findCurrentBootstrapPath(Path earlyPluginsFolder) {
        if (!Files.exists(earlyPluginsFolder)) {
            return null;
        }
        try {
            return Files.list(earlyPluginsFolder)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.contains("modsync-bootstrap") &&
                                name.endsWith(".jar") &&
                                !name.contains("sources") &&
                                !name.contains("javadoc");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not scan earlyplugins folder: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Downloads an asset to the target folder with resilient file operations.
     * Includes retry logic and fallback mechanisms for locked files.
     */
    private Path downloadAsset(GitHubAsset asset, Path targetFolder) throws IOException, InterruptedException {
        String url = asset.getBrowserDownloadUrl();
        Path targetPath = targetFolder.resolve(asset.getName());
        Path tempPath = targetFolder.resolve(asset.getName() + ".tmp");

        LOGGER.atInfo().log("Downloading %s (%d bytes)", asset.getName(), asset.getSize());

        // Ensure target folder exists
        Files.createDirectories(targetFolder);

        // Download with retry logic
        IOException lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "ModSync-Plugin")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = downloadClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    throw new IOException("Download failed with status: " + response.statusCode());
                }

                try (InputStream is = response.body()) {
                    Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // Verify download completed
                if (!Files.exists(tempPath) || Files.size(tempPath) == 0) {
                    throw new IOException("Downloaded file is empty or missing");
                }

                break; // Download successful
            } catch (IOException e) {
                lastException = e;
                LOGGER.atWarning().log("Download attempt %d failed: %s", attempt, e.getMessage());
                cleanupTempFile(tempPath);
                if (attempt < 3) {
                    Thread.sleep(1000L * attempt); // Exponential backoff
                }
            }
        }

        if (lastException != null && (!Files.exists(tempPath) || Files.size(tempPath) == 0)) {
            throw new IOException("Download failed after 3 attempts: " + lastException.getMessage(), lastException);
        }

        // Move file with fallback to copy+delete
        try {
            moveFileWithFallback(tempPath, targetPath);
        } catch (IOException e) {
            cleanupTempFile(tempPath);
            throw new IOException("Failed to move downloaded file: " + e.getMessage(), e);
        }

        LOGGER.atInfo().log("Downloaded %s successfully", asset.getName());
        return targetPath;
    }

    /**
     * Moves a file with fallback to copy+delete if atomic move fails.
     */
    private void moveFileWithFallback(Path source, Path target) throws IOException {
        // First, try to delete existing target if it exists
        if (Files.exists(target)) {
            try {
                Files.delete(target);
            } catch (IOException e) {
                // Target is locked, queue for deletion and use alternative name
                LOGGER.atWarning().log("Target file locked, will be replaced on restart: %s", target);
                queueForDeletion(target);
                // Continue with move - it will create alongside the old file
            }
        }

        // Try atomic move first
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (IOException e) {
            LOGGER.atWarning().log("Atomic move failed, trying copy+delete: %s", e.getMessage());
        }

        // Fallback: copy then delete source
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.delete(source);
            } catch (IOException e) {
                LOGGER.atWarning().log("Could not delete temp file: %s", source);
                // Not critical, temp file will be orphaned but won't break anything
            }
        } catch (IOException e) {
            throw new IOException("Both move and copy failed for " + source + " -> " + target, e);
        }
    }

    /**
     * Safely cleans up a temporary file.
     */
    private void cleanupTempFile(Path tempPath) {
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not cleanup temp file %s: %s", tempPath, e.getMessage());
        }
    }

    /**
     * Queues a file for deletion on next server restart.
     */
    private void queueForDeletion(Path filePath) {
        try {
            // Try to delete immediately first
            Files.delete(filePath);
            LOGGER.atInfo().log("Deleted old file: %s", filePath);
        } catch (IOException e) {
            // File is locked, queue for deletion
            LOGGER.atInfo().log("File locked, queuing for deletion: %s", filePath);
            plugin.addPendingDeletion(filePath.toString());
        }
    }

    /**
     * Gets the GitHub client for direct access if needed.
     */
    public GitHubClient getGitHubClient() {
        return gitHubClient;
    }
}