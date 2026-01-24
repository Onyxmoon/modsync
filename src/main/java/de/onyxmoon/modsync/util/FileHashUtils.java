package de.onyxmoon.modsync.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for calculating file hashes.
 */
public final class FileHashUtils {

    private FileHashUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculates the SHA-256 hash of a file.
     *
     * @param filePath the path to the file
     * @return the hash in format "sha256:hexstring"
     * @throws IOException if the file cannot be read
     */
    public static String calculateSha256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            return "sha256:" + HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
