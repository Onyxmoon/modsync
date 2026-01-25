package de.onyxmoon.modsync.provider.modtale;

import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ParsedModUrl;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL parser for Modtale project URLs.
 * Internal helper class - the provider delegates URL parsing to this class.
 */
class ModtaleUrlParser {

    private final String source;

    /**
     * Creates a new Modtale URL parser.
     *
     * @param source the source identifier from the provider
     */
    public ModtaleUrlParser(String source) {
        this.source = source;
    }

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern MODTALE_HOST = Pattern.compile("^(?:www\\.)?modtale\\.net$", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_HOST = Pattern.compile("^api\\.modtale\\.net$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATH_WITH_UUID = Pattern.compile(
            "/projects?/([0-9a-fA-F-]{36})(?:/.*)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PATH_WITH_SLUG_UUID = Pattern.compile(
            "/mod/([a-z0-9-]+)-([0-9a-fA-F-]{36})(?:/.*)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PATH_WITH_SLUG = Pattern.compile(
            "/mod/([a-z0-9-]+)(?:/.*)?",
            Pattern.CASE_INSENSITIVE
    );

    public boolean canParse(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        if (UUID_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) {
                return false;
            }
            if (!MODTALE_HOST.matcher(host).matches() && !API_HOST.matcher(host).matches()) {
                return false;
            }
            return PATH_WITH_UUID.matcher(path).find()
                    || PATH_WITH_SLUG_UUID.matcher(path).find()
                    || PATH_WITH_SLUG.matcher(path).find()
                    || extractSlug(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public ParsedModUrl parse(String url) throws InvalidModUrlException {
        if (url == null || url.isBlank()) {
            throw new InvalidModUrlException(url, "URL cannot be null or empty");
        }
        String trimmed = url.trim();
        if (UUID_PATTERN.matcher(trimmed).matches()) {
            return new ParsedModUrl(source, trimmed, null, null);
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null ||
                    (!MODTALE_HOST.matcher(host).matches() && !API_HOST.matcher(host).matches())) {
                throw new InvalidModUrlException(url, "URL does not match Modtale host");
            }
            Matcher matcher = PATH_WITH_UUID.matcher(path);
            if (matcher.find()) {
                String id = matcher.group(1);
                return new ParsedModUrl(source, id, null, null);
            }
            Matcher slugMatcher = PATH_WITH_SLUG_UUID.matcher(path);
            if (slugMatcher.find()) {
                String slug = slugMatcher.group(1);
                String id = slugMatcher.group(2);
                return new ParsedModUrl(source, id, slug, null);
            }
            Matcher slugOnlyMatcher = PATH_WITH_SLUG.matcher(path);
            if (slugOnlyMatcher.find()) {
                String slug = slugOnlyMatcher.group(1);
                return new ParsedModUrl(source, null, slug, null);
            }
            String slug = extractSlug(path);
            if (slug == null) {
                throw new InvalidModUrlException(url, "URL does not contain a project identifier");
            }
            return new ParsedModUrl(source, null, slug, null);
        } catch (InvalidModUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidModUrlException(url, "URL does not match Modtale pattern");
        }
    }

    private String extractSlug(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == trimmed.length() - 1) {
            return null;
        }
        String candidate = trimmed.substring(slashIndex + 1);
        if (candidate.isBlank() || candidate.equalsIgnoreCase("projects") || candidate.equalsIgnoreCase("project")) {
            return null;
        }
        if (candidate.equalsIgnoreCase("mod") || candidate.equalsIgnoreCase("mods")) {
            return null;
        }
        if (UUID_PATTERN.matcher(candidate).matches()) {
            return null;
        }
        return candidate;
    }
}
