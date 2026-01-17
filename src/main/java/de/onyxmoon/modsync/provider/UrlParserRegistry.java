package de.onyxmoon.modsync.provider;

import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ModUrlParser;
import de.onyxmoon.modsync.provider.curseforge.CurseForgeUrlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry for URL parsers.
 */
public class UrlParserRegistry {

    private final List<ModUrlParser> parsers;

    public UrlParserRegistry() {
        this.parsers = new ArrayList<>();
        registerBuiltInParsers();
    }

    private void registerBuiltInParsers() {
        // Register CurseForge parser
        parsers.add(new CurseForgeUrlParser());

        // Future: Add Modrinth parser here
        // parsers.add(new ModrinthUrlParser());
    }

    /**
     * Find a parser that can handle the given URL.
     *
     * @param url the URL to parse
     * @return the parser if found
     */
    public Optional<ModUrlParser> findParser(String url) {
        return parsers.stream()
                .filter(parser -> parser.canParse(url))
                .findFirst();
    }

    /**
     * Get a parser for a specific source.
     *
     * @param source the mod source
     * @return the parser if found
     */
    public Optional<ModUrlParser> getParser(ModListSource source) {
        return parsers.stream()
                .filter(parser -> parser.getSource() == source)
                .findFirst();
    }

    /**
     * Check if any parser can handle the given URL.
     *
     * @param url the URL to check
     * @return true if a parser exists for this URL
     */
    public boolean canParse(String url) {
        return findParser(url).isPresent();
    }

    /**
     * Register a custom parser.
     *
     * @param parser the parser to register
     */
    public void registerParser(ModUrlParser parser) {
        parsers.add(parser);
    }

    /**
     * Get all registered parsers.
     *
     * @return list of all parsers
     */
    public List<ModUrlParser> getParsers() {
        return List.copyOf(parsers);
    }
}