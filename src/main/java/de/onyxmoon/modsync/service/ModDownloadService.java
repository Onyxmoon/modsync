package de.onyxmoon.modsync.service;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.util.FileHashUtils;
import de.onyxmoon.modsync.util.ManifestReader;

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
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Service for downloading and installing mods.
 */
public class ModDownloadService {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final ModSync modSync;
    private final Path modsFolder;
    private final Path earlyPluginsFolder;
    private final HttpClient httpClient;

    public ModDownloadService(ModSync modSync, Path modsFolder, Path earlyPluginsFolder) {
        this.modSync = modSync;
        this.modsFolder = modsFolder;
        this.earlyPluginsFolder = earlyPluginsFolder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Ensure folders exist
        try {
            Files.createDirectories(modsFolder);
            Files.createDirectories(earlyPluginsFolder);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to create plugin folders");
        }
    }

    /**
     * Download and install a mod.
     * Returns the InstalledState which should be added to the ManagedMod by the caller.
     *
     * The download is atomic: the file only appears at the final location after
     * successful validation (manifest readable, hash calculated). If any step fails,
     * the temp file is cleaned up and no orphan files remain.
     */
    public CompletableFuture<InstalledState> downloadAndInstall(
            ManagedMod mod,
            ModVersion version) {

        String downloadUrl = version.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No download URL available for " + mod.getName())
            );
        }

        String fileName = version.getFileName();
        PluginType pluginType = mod.getPluginType();
        Path targetFolder = getTargetFolder(pluginType);
        Path targetPath = targetFolder.resolve(fileName);
        Path tempPath = targetFolder.resolve(fileName + ".tmp");

        LOGGER.atInfo().log("Downloading %s (%s) to %s", mod.getName(), pluginType.getDisplayName(), targetPath);

        return downloadToTemp(downloadUrl, tempPath)
                .thenApply(downloadedTempPath -> {
                    try {
                        // Validate BEFORE moving to final location
                        String hash = FileHashUtils.calculateSha256(downloadedTempPath);
                        long fileSize = Files.size(downloadedTempPath);
                        PluginManifest manifest = ManifestReader.readManifest(downloadedTempPath)
                                .orElse(null);

                        if (manifest == null) {
                            cleanupTempFile(downloadedTempPath);
                            throw new RuntimeException("Failed to read manifest from downloaded file");
                        }

                        var identifier = new PluginIdentifier(manifest.getGroup(), manifest.getName());

                        // All validation passed - now move to final location
                        moveFileWithFallback(downloadedTempPath, targetPath);

                        InstalledState installedState = InstalledState.builder()
                                .identifier(identifier)
                                .installedVersionId(version.getVersionId())
                                .installedVersionNumber(version.getVersionNumber())
                                .filePath(targetPath.toString())
                                .fileName(fileName)
                                .fileSize(fileSize)
                                .fileHash(hash)
                                .installedAt(Instant.now())
                                .lastChecked(Instant.now())
                                .build();

                        LOGGER.atInfo().log("Successfully installed %s (%s) as %s",
                                mod.getName(), version.getVersionNumber(), pluginType.getDisplayName());
                        return installedState;
                    } catch (IOException e) {
                        cleanupTempFile(downloadedTempPath);
                        throw new RuntimeException("Failed to process downloaded file", e);
                    } catch (RuntimeException e) {
                        cleanupTempFile(downloadedTempPath);
                        throw e;
                    }
                });
    }

    /**
     * Get the target folder for the given plugin type.
     */
    private Path getTargetFolder(PluginType pluginType) {
        return pluginType == PluginType.EARLY_PLUGIN ? earlyPluginsFolder : modsFolder;
    }

    /**
     * Downloads a file to a temp location with retry logic.
     * Does NOT move to final location - caller must do that after validation.
     */
    private CompletableFuture<Path> downloadToTemp(String url, Path tempPath) {
        return CompletableFuture.supplyAsync(() -> {
            IOException lastException = null;

            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

                    HttpResponse<InputStream> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofInputStream());

                    if (response.statusCode() != 200) {
                        throw new IOException("Download failed with status: " + response.statusCode());
                    }

                    try (InputStream inputStream = response.body()) {
                        Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Verify download completed
                    if (!Files.exists(tempPath) || Files.size(tempPath) == 0) {
                        throw new IOException("Downloaded file is empty or missing");
                    }

                    return tempPath; // Success
                } catch (IOException e) {
                    lastException = e;
                    LOGGER.atWarning().log("Download attempt %d failed: %s", attempt, e.getMessage());
                    cleanupTempFile(tempPath);
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        try {
                            Thread.sleep(1000L * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Download interrupted", ie);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Download interrupted", e);
                }
            }

            throw new RuntimeException("Download failed after " + MAX_RETRY_ATTEMPTS + " attempts: " +
                    lastException.getMessage(), lastException);
        });
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
                // Target is locked, queue for deletion
                LOGGER.atWarning().log("Target file locked, will be replaced on restart: %s", target);
                modSync.addPendingDeletion(target.toString());
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
                LOGGER.atWarning().log("Could not delete temp file, scheduling for deletion on restart: %s", source);
                modSync.addPendingDeletion(source.toString());
            }
        } catch (IOException e) {
            throw new IOException("Both move and copy failed for " + source + " -> " + target, e);
        }
    }

    /**
     * Safely cleans up a temporary file.
     * If deletion fails, schedules for deletion on next startup.
     */
    private void cleanupTempFile(Path tempPath) {
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException e) {
            LOGGER.atWarning().log("Could not cleanup temp file, scheduling for deletion on restart: %s", tempPath);
            modSync.addPendingDeletion(tempPath.toString());
        }
    }

    /**
     * Delete an installed mod. Unloads if loaded, then deletes or schedules for deletion on next startup.
     * @return true if file was deleted immediately, false if restart is required
     */
    public CompletableFuture<Boolean> deleteMod(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return CompletableFuture.completedFuture(true);
        }

        InstalledState state = mod.getInstalledState().orElseThrow();
        PluginIdentifier identifier = state.getIdentifier();

        return CompletableFuture.supplyAsync(() -> {
            // Unload if currently loaded
            if (identifier != null &&
                    modSync.getPluginManager().getPlugin(identifier) != null &&
                    Objects.requireNonNull(modSync.getPluginManager().getPlugin(identifier)).isEnabled()) {
                if (!modSync.getPluginManager().unload(identifier)) {
                    throw new RuntimeException("Failed to unload mod");
                }
            }

            Path filePath = Path.of(state.getFilePath());
            boolean deletedImmediately = true;

            // Try to delete immediately
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    LOGGER.atInfo().log("Deleted mod file: %s", filePath);
                } catch (IOException e) {
                    // File is locked, schedule for deletion on next startup
                    LOGGER.atWarning().log("File locked, scheduling for deletion on next startup: %s", filePath);
                    modSync.addPendingDeletion(filePath.toString());
                    deletedImmediately = false;
                }
            }

            return deletedImmediately;
        });
    }

    public Path getModsFolder() {
        return modsFolder;
    }

    public Path getEarlyPluginsFolder() {
        return earlyPluginsFolder;
    }
}