package de.onyxmoon.modsync.util;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import de.onyxmoon.modsync.ModSync;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Utility class for reading plugin manifests from JAR files.
 */
public final class ManifestReader {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);

    private ManifestReader() {
        // Utility class - prevent instantiation
    }

    /**
     * Reads the plugin manifest from a JAR file.
     *
     * @param jarPath the path to the JAR file
     * @return the manifest, or empty if it cannot be read
     */
    public static Optional<PluginManifest> readManifest(Path jarPath) {
        try {
            URL url = jarPath.toUri().toURL();
            URL resource;
            try (PluginClassLoader pluginClassLoader = new PluginClassLoader(new PluginManager(), false, url)) {
                resource = pluginClassLoader.findResource("manifest.json");
            }
            if (resource == null) {
                LOGGER.at(Level.FINE).log("No manifest.json found in '%s'", jarPath);
                return Optional.empty();
            }

            try (
                    InputStream stream = resource.openStream();
                    InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
            ) {
                char[] buffer = RawJsonReader.READ_BUFFER.get();
                RawJsonReader rawJsonReader = new RawJsonReader(reader, buffer);
                ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
                PluginManifest manifest = PluginManifest.CODEC.decodeJson(rawJsonReader, extraInfo);
                return Optional.ofNullable(manifest);
            }
        } catch (Exception e) {
            LOGGER.atFine().log("Could not read manifest from %s: %s", jarPath.getFileName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Reads the plugin identifier from a JAR file's manifest.
     *
     * @param jarPath the path to the JAR file
     * @return the identifier, or empty if the manifest cannot be read
     */
    public static Optional<PluginIdentifier> readIdentifier(Path jarPath) {
        return readManifest(jarPath)
                .map(manifest -> new PluginIdentifier(manifest.getGroup(), manifest.getName()));
    }
}
