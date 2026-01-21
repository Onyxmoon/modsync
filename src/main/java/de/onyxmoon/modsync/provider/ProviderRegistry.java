package de.onyxmoon.modsync.provider;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;

import java.util.*;

/**
 * Registry for mod list providers using ServiceLoader.
 */
public class ProviderRegistry {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final Map<ModListSource, ModListProvider> providers;

    public ProviderRegistry() {
        this.providers = new HashMap<>();
        loadProviders();
    }

    private void loadProviders() {
        providers.clear();

        Set<ClassLoader> loaders = new LinkedHashSet<>(List.of(
                Thread.currentThread().getContextClassLoader(),
                ProviderRegistry.class.getClassLoader(),
                ModListProvider.class.getClassLoader()
        ));

        for (ClassLoader cl : loaders) {
            if (cl == null) continue;

            try {
                ServiceLoader<ModListProvider> loader = ServiceLoader.load(ModListProvider.class, cl);

                // stream() zeigt Kandidaten ohne sofort zu instanziieren
                loader.stream().forEach(p -> {
                    LOGGER.atInfo().log("Service candidate via %s: %s", cl, p.type().getName());
                    try {
                        ModListProvider prov = p.get(); // instanziieren
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



    public ModListProvider getProvider(ModListSource source) {
        ModListProvider provider = providers.get(source);
        if (provider == null) {
            throw new IllegalArgumentException("No provider for source: " + source);
        }
        return provider;
    }

    public boolean hasProvider(ModListSource source) {
        return providers.containsKey(source);
    }

    public Collection<ModListSource> getAvailableSources() {
        return providers.keySet();
    }
}