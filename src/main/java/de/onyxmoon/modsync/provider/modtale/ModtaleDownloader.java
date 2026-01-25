package de.onyxmoon.modsync.provider.modtale;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModProviderWithDownloadHandler.DownloadResult;

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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modtale download handler for authenticated downloads with X-MODTALE-KEY header.
 * Internal helper class - the provider delegates download to this class.
 */
class ModtaleDownloader {

    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern CONTENT_DISPOSITION_FILENAME = Pattern.compile(
            "filename\\*?=[\"']?(?:UTF-8'')?([^\"';\\s]+)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Downloads a file from Modtale with authentication.
     *
     * @param downloadUrl the URL to download from
     * @param apiKey      the Modtale API key
     * @param targetDir   the directory to download to
     * @return CompletableFuture containing the download result
     */
    CompletableFuture<DownloadResult> download(String downloadUrl, String apiKey, Path targetDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path tempFile = targetDir.resolve(UUID.randomUUID() + ".tmp");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("X-MODTALE-KEY", apiKey)
                        .header("Accept", "*/*")
                        .GET()
                        .build();

                LOGGER.atInfo().log("Downloading from Modtale: %s", downloadUrl);

                HttpResponse<InputStream> response = HTTP_CLIENT.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    throw new IOException("Download failed with status: " + response.statusCode());
                }

                // Extract filename from Content-Disposition header
                String fileName = extractFileName(response);
                LOGGER.atInfo().log("Extracted filename from Content-Disposition: %s", fileName);

                // Save to temp file
                try (InputStream inputStream = response.body()) {
                    Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                long fileSize = Files.size(tempFile);
                if (fileSize == 0) {
                    Files.deleteIfExists(tempFile);
                    throw new IOException("Downloaded file is empty");
                }

                return new DownloadResult(tempFile, fileName, fileSize);

            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Modtale download failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Extracts filename from Content-Disposition header.
     * Supports both standard and RFC 5987 encoded filenames.
     */
    private String extractFileName(HttpResponse<?> response) {
        return response.headers()
                .firstValue("Content-Disposition")
                .map(this::parseContentDisposition)
                .orElse(null);
    }

    private String parseContentDisposition(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }

        Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(header);
        if (matcher.find()) {
            String filename = matcher.group(1);
            // URL decode if needed (for RFC 5987 encoded filenames)
            try {
                return java.net.URLDecoder.decode(filename, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return filename;
            }
        }
        return null;
    }
}
