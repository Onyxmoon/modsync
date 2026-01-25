package de.onyxmoon.modsync.provider.cfwidget;

import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ParsedModUrl;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL parser for CurseForge mod URLs.
 * Internal helper class - the provider delegates URL parsing to this class.
 *
 * Supported URL patterns:
 * - https://www.curseforge.com/hytale/mods/example-mod
 * - https://www.curseforge.com/hytale/mods/example-mod/files
 * - https://www.curseforge.com/hytale/mods/example-mod/files/12345
 * - https://www.curseforge.com/hytale/bootstrap/example-plugin
 * - https://www.curseforge.com/hytale/bootstrap/example-plugin/files/12345
 * - https://curseforge.com/hytale/mods/example-mod
 */
class CfWidgetUrlParser {

    private final String source;
    private static final Pattern CF_WIDGET_HOST = Pattern.compile("^(?:api\\.)?cfwidget\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURSEFORGE_URL = Pattern.compile(
            "^https?://(?:www\\.)?curseforge\\.com/hytale/(mods|bootstrap)/([\\w-]+)(?:/files(?:/(\\d+))?)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Creates a new CurseForge URL parser.
     *
     * @param source the source identifier from the provider
     */
    public CfWidgetUrlParser(String source) {

        this.source = source;
    }

    public boolean canParse(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        if (CURSEFORGE_URL.matcher(trimmed).matches()) {
            return true;
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            String path = uri.getPath();
            return host != null && CF_WIDGET_HOST.matcher(host).matches()
                    && path != null && !path.isBlank() && !path.endsWith(".png");
        } catch (Exception e) {
            return false;
        }
    }

    public ParsedModUrl parse(String url) throws InvalidModUrlException {
        if (url == null || url.isBlank()) {
            throw new InvalidModUrlException(url, "URL cannot be null or empty");
        }
        String trimmed = url.trim();
        Matcher cfMatcher = CURSEFORGE_URL.matcher(trimmed);
        if (cfMatcher.matches()) {
            String category = cfMatcher.group(1);
            String slug = cfMatcher.group(2);
            String versionId = cfMatcher.group(3);
            String path = "hytale/" + category + "/" + slug;
            return new ParsedModUrl(source, path, slug, versionId);
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (host == null || !CF_WIDGET_HOST.matcher(host).matches()) {
                throw new InvalidModUrlException(url, "URL does not match CFWidget pattern");
            }
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                throw new InvalidModUrlException(url, "URL does not contain a project path");
            }
            if (path.endsWith(".png")) {
                throw new InvalidModUrlException(url, "Image URLs are not supported");
            }
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            String slug = extractSlug(normalized);
            return new ParsedModUrl(source, normalized, slug, null);
        } catch (InvalidModUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidModUrlException(url, "URL does not match CFWidget pattern");
        }
    }

    private static String extractSlug(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex < 0) {
            return trimmed;
        }
        return trimmed.substring(slashIndex + 1);
    }
}
