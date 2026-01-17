package de.onyxmoon.modsync.api;

/**
 * Interface for parsing mod URLs from different sources.
 */
public interface ModUrlParser {

    /**
     * Check if this parser can handle the given URL.
     *
     * @param url the URL to check
     * @return true if this parser can parse the URL
     */
    boolean canParse(String url);

    /**
     * Parse the URL and extract mod information.
     *
     * @param url the URL to parse
     * @return parsed mod URL information
     * @throws InvalidModUrlException if the URL cannot be parsed
     */
    ParsedModUrl parse(String url) throws InvalidModUrlException;

    /**
     * Get the source this parser handles.
     *
     * @return the mod list source
     */
    ModListSource getSource();
}