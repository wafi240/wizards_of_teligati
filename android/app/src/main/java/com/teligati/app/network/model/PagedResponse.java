package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PagedResponse<T> {
    @SerializedName("content")
    private List<T> content;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("totalElements")
    private long totalElements;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("last")
    private boolean last;

    public PagedResponse() {}

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
