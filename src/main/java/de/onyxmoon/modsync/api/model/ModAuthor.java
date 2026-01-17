package de.onyxmoon.modsync.api.model;

import java.util.Objects;

/**
 * Immutable model representing a mod author.
 */
public final class ModAuthor {
    private final String name;
    private final String url;

    public ModAuthor(String name, String url) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModAuthor modAuthor = (ModAuthor) o;
        return Objects.equals(name, modAuthor.name) &&
               Objects.equals(url, modAuthor.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    @Override
    public String toString() {
        return "ModAuthor{" +
               "name='" + name + '\'' +
               ", url='" + url + '\'' +
               '}';
    }
}