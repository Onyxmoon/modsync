package de.onyxmoon.modsync.provider.curseforge;

import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ModUrlParser;
import de.onyxmoon.modsync.api.ParsedModUrl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL parser for CurseForge mod URLs.
 *
 * Supported URL patterns:
 * - https://www.curseforge.com/hytale/mods/example-mod
 * - https://www.curseforge.com/hytale/mods/example-mod/files
 * - https://www.curseforge.com/hytale/mods/example-mod/files/12345
 * - https://curseforge.com/hytale/mods/example-mod
 */
public class CurseForgeUrlParser implements ModUrlParser {

    // Pattern to match CurseForge Hytale mod URLs
    // Groups: (1) slug, (2) optional file ID
    private static final Pattern CURSEFORGE_URL_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?curseforge\\.com/hytale/mods/([\\w-]+)(?:/files(?:/(\\d+))?)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean canParse(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return CURSEFORGE_URL_PATTERN.matcher(url.trim()).matches();
    }

    @Override
    public ParsedModUrl parse(String url) throws InvalidModUrlException {
        if (url == null || url.isBlank()) {
            throw new InvalidModUrlException(url, "URL cannot be null or empty");
        }

        Matcher matcher = CURSEFORGE_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new InvalidModUrlException(url, "URL does not match CurseForge pattern");
        }

        String slug = matcher.group(1);
        String versionId = matcher.group(2); // May be null

        return new ParsedModUrl(
                ModListSource.CURSEFORGE,
                null,  // modId will be resolved via API using slug
                slug,
                versionId
        );
    }

    @Override
    public ModListSource getSource() {
        return ModListSource.CURSEFORGE;
    }
}