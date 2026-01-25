package de.onyxmoon.modsync.provider;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModProvider;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for mod list providers using ServiceLoader.
 */
public class ProviderRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);

    /**
     * Thread-safe map of providers. Uses ConcurrentHashMap to allow
     * safe concurrent access during provider lookups.
     * Key is the source identifier (lowercase), e.g., "curseforge", "modtale".
     */
    private final Map<String, ModProvider> providers;

    public ProviderRegistry() {
        this.providers = new ConcurrentHashMap<>();
        loadProviders();
    }

    private void loadProviders() {
        providers.clear();

        Set<ClassLoader> loaders = new LinkedHashSet<>(List.of(
                Thread.currentThread().getContextClassLoader(),
                ProviderRegistry.class.getClassLoader(),
                ModProvider.class.getClassLoader()
        ));

        for (ClassLoader cl : loaders) {
            if (cl == null) continue;

            try {
                ServiceLoader<ModProvider> loader = ServiceLoader.load(ModProvider.class, cl);

                // stream() zeigt Kandidaten ohne sofort zu instanziieren
                loader.stream().forEach(p -> {
                    LOGGER.atInfo().log("Service candidate via %s: %s", cl, p.type().getName());
                    try {
                        ModProvider prov = p.get(); // instanziieren
                        providers.put(prov.getSource(), prov);
                        LOGGER.atInfo().log("Registered provider via %s: %s", cl, prov.getClass().getName());
                    } catch (Throwable t) {
                        LOGGER.atSevere().withCause(t).log("Failed to instantiate provider %s via %s", p.type().getName(), cl);
                    }
                });
            } catch (Throwable t) {
                LOGGER.atSevere().withCause(t).log("ServiceLoader failed via %s", cl);
            }
        }

        if (providers.isEmpty()) {
            LOGGER.atWarning().log("No mod list providers found!");
        }
    }



    /**
     * Gets a provider by its source identifier.
     *
     * @param source the source identifier (e.g., "curseforge", "modtale")
     * @return the provider
     * @throws IllegalArgumentException if no provider exists for the source
     */
    public ModProvider getProvider(String source) {
        String normalized = source != null ? source.toLowerCase() : null;
        ModProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new IllegalArgumentException("No provider for source: " + source);
        }
        return provider;
    }

    /**
     * Checks if a provider exists for the given source.
     *
     * @param source the source identifier
     * @return true if a provider exists
     */
    public boolean hasProvider(String source) {
        String normalized = source != null ? source.toLowerCase() : null;
        return providers.containsKey(normalized);
    }

    public Collection<ModProvider> getProviders() {
        return providers.values();
    }

    /**
     * Gets the display name for a source identifier.
     * Falls back to the source string if no provider is found.
     *
     * @param source the source identifier (e.g., "curseforge")
     * @return the display name (e.g., "CurseForge")
     */
    public String getDisplayName(String source) {
        if (source == null) {
            return "Unknown";
        }
        ModProvider provider = providers.get(source.toLowerCase());
        return provider != null ? provider.getDisplayName() : source;
    }
}
