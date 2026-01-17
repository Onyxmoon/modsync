package de.onyxmoon.modsync.provider.curseforge.model;

import java.util.List;

/**
 * CurseForge API search response wrapper.
 */
public class CurseForgeSearchResponse {
    private List<CurseForgeModResponse.ModData> data;
    private PaginationData pagination;

    public List<CurseForgeModResponse.ModData> getData() {
        return data;
    }

    public void setData(List<CurseForgeModResponse.ModData> data) {
        this.data = data;
    }

    public PaginationData getPagination() {
        return pagination;
    }

    public void setPagination(PaginationData pagination) {
        this.pagination = pagination;
    }

    public static class PaginationData {
        private int index;
        private int pageSize;
        private int resultCount;
        private int totalCount;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getResultCount() {
            return resultCount;
        }

        public void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
    }
}