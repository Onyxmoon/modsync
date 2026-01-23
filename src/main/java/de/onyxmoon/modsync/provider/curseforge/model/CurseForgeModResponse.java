package de.onyxmoon.modsync.provider.curseforge.model;

import java.time.Instant;
import java.util.List;

/**
 * CurseForge API response models for mod data.
 * These are simplified DTOs that match the CurseForge API structure.
 */
public class CurseForgeModResponse {
    private ModData data;

    public ModData getData() {
        return data;
    }

    public void setData(ModData data) {
        this.data = data;
    }

    public static class ModData {
        private int id;
        private String name;
        private String slug;
        private String summary;
        private List<AuthorData> authors;
        private List<FileData> latestFiles;
        private List<CategoryData> categories;
        private Integer classId;
        private int downloadCount;
        private LinksData links;
        private LogoData logo;
        private Instant dateModified;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<AuthorData> getAuthors() {
            return authors;
        }

        public void setAuthors(List<AuthorData> authors) {
            this.authors = authors;
        }

        public List<FileData> getLatestFiles() {
            return latestFiles;
        }

        public void setLatestFiles(List<FileData> latestFiles) {
            this.latestFiles = latestFiles;
        }

        public List<CategoryData> getCategories() {
            return categories;
        }

        public void setCategories(List<CategoryData> categories) {
            this.categories = categories;
        }

        public Integer getClassId() {
            return classId;
        }

        public void setClassId(Integer classId) {
            this.classId = classId;
        }

        public int getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(int downloadCount) {
            this.downloadCount = downloadCount;
        }

        public LinksData getLinks() {
            return links;
        }

        public void setLinks(LinksData links) {
            this.links = links;
        }

        public LogoData getLogo() {
            return logo;
        }

        public void setLogo(LogoData logo) {
            this.logo = logo;
        }

        public Instant getDateModified() {
            return dateModified;
        }

        public void setDateModified(Instant dateModified) {
            this.dateModified = dateModified;
        }
    }

    public static class AuthorData {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class FileData {
        private int id;
        private String displayName;
        private String fileName;
        private long fileLength;
        private String downloadUrl;
        private List<String> gameVersions;
        private int releaseType;  // CurseForge uses: 1=Release, 2=Beta, 3=Alpha
        private Instant fileDate;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileLength() {
            return fileLength;
        }

        public void setFileLength(long fileLength) {
            this.fileLength = fileLength;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public List<String> getGameVersions() {
            return gameVersions;
        }

        public void setGameVersions(List<String> gameVersions) {
            this.gameVersions = gameVersions;
        }

        public int getReleaseType() {
            return releaseType;
        }

        public void setReleaseType(int releaseType) {
            this.releaseType = releaseType;
        }

        public Instant getFileDate() {
            return fileDate;
        }

        public void setFileDate(Instant fileDate) {
            this.fileDate = fileDate;
        }
    }

    public static class CategoryData {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class LinksData {
        private String websiteUrl;

        public String getWebsiteUrl() {
            return websiteUrl;
        }

        public void setWebsiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
        }
    }

    public static class LogoData {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}