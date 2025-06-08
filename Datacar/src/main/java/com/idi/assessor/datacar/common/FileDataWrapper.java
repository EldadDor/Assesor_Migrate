package com.idi.assessor.datacar.common;

import java.util.Date;

public class FileDataWrapper {

    private final String path; // Full path or unique identifier on the Samba share
    private final String name;
    private byte[] content;
    private final long size;
    private final long lastModifiedTimestamp;
    // Add other relevant metadata as needed (e.g., creationTimestamp, owner)

    public FileDataWrapper(String path, String name, byte[] content, long size, long lastModifiedTimestamp) {
        this.path = path;
        this.name = name;
        this.content = content;
        this.size = size;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    // Constructor for when content might be fetched lazily (scanner provides metadata first)
    public FileDataWrapper(String path, String name, long size, long lastModifiedTimestamp) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
        this.content = null; // Content to be loaded later if needed


    }


    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public long getSize() {
        return size;
    }

    public Date getLastModifiedDate() {
        return new Date(lastModifiedTimestamp);
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    @Override
    public String toString() {
        return "FileDataWrapper{"
                + "path='" + path + '\''
                + ", name='" + name + '\''
                + ", size=" + size
                + ", lastModified=" + getLastModifiedDate()
                + ", contentPresent=" + (content != null)
                + '}';
    }
}
