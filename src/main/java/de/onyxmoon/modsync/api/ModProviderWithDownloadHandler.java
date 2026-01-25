package de.onyxmoon.modsync.api;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Optional interface for providers that need custom download logic.
 * <p>
 * If a provider implements this interface, ModDownloadService will use
 * the provider's download method instead of the default HTTP download.
 * This allows for:
 * <ul>
 *   <li>Authenticated downloads (e.g., API key in headers)</li>
 *   <li>Custom headers or request configuration</li>
 *   <li>Extracting filename from Content-Disposition header</li>
 * </ul>
 */
public interface ModProviderWithDownloadHandler extends ModProvider {

    /**
     * Result of a download operation.
     *
     * @param downloadedFile the path to the downloaded temp file
     * @param actualFileName the actual filename (from Content-Disposition or fallback)
     * @param fileSize       the size of the downloaded file in bytes
     */
    record DownloadResult(
            Path downloadedFile,
            String actualFileName,
            long fileSize
    ) {}

    /**
     * Downloads a file from the given URL with provider-specific logic.
     *
     * @param downloadUrl the URL to download from
     * @param apiKey      the API key (may be null if not required)
     * @param targetDir   the directory to download to (file will be created as temp file)
     * @return CompletableFuture containing the download result
     */
    CompletableFuture<DownloadResult> download(
            String downloadUrl,
            String apiKey,
            Path targetDir
    );
}
