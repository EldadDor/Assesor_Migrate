package com.idi.assessor.datacar.source;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.scanner.AbstractRecursiveDirectoryScanner;
import com.idi.assessor.datacar.util.SambaFileUtil;

/**
 * A custom MessageSource implementation that uses our AbstractRecursiveDirectoryScanner
 * to scan for files on a Samba share and create Messages with FileDataWrapper payloads.
 */
public class SambaMessageSource implements MessageSource<FileDataWrapper> {

    private static final Logger log = LoggerFactory.getLogger(SambaMessageSource.class);
    
    private final AbstractRecursiveDirectoryScanner<?, ?, ?> scanner;
    private final SambaFileUtil sambaFileUtil;
    private final int queueSize;
    private boolean loadContentImmediately = false;

    /**
     * Creates a SambaMessageSource with the provided scanner.
     * 
     * @param scanner The scanner to use for finding files on Samba shares
     * @param sambaFileUtil Utility for interacting with Samba shares
     * @param queueSize Maximum number of files to process in one polling cycle
     */
    public SambaMessageSource(AbstractRecursiveDirectoryScanner<?, ?, ?> scanner, 
                             SambaFileUtil sambaFileUtil,
                             int queueSize) {
        Assert.notNull(scanner, "Scanner must not be null");
        Assert.notNull(sambaFileUtil, "SambaFileUtil must not be null");
        
        this.scanner = scanner;
        this.sambaFileUtil = sambaFileUtil;
        this.queueSize = queueSize > 0 ? queueSize : 100; // Default to 100 if invalid value provided
    }

    /**
     * Set whether to load file content immediately when creating the FileDataWrapper.
     * If true, file content is loaded when the file is first discovered.
     * If false, only metadata is loaded, and content can be loaded later when needed.
     * 
     * @param loadContentImmediately True to load content immediately, false to load on demand
     */
    public void setLoadContentImmediately(boolean loadContentImmediately) {
        this.loadContentImmediately = loadContentImmediately;
    }

    @Override
    public Message<FileDataWrapper> receive() {
        try {
            List<FileDataWrapper> files = scanner.scanForFiles();
            
            if (files == null || files.isEmpty()) {
                log.debug("No files found by scanner at path: {}", scanner.sambaDirectoryPath);
                return null;
            }

            // Limit the number of files processed in one go
            Collection<FileDataWrapper> limitedFiles = 
                files.size() > queueSize ? files.subList(0, queueSize) : files;
            
            // Process the first file in the list
            // (In a production implementation, you might want to sort by timestamp or other criteria)
            FileDataWrapper file = limitedFiles.iterator().next();
            
            if (file == null) {
                return null;
            }
            
            log.debug("Processing file: {}", file.getPath());
            
            // If configured to load content immediately, load it now
            if (loadContentImmediately && file.getContent() == null) {
                try {
                    byte[] content = sambaFileUtil.readFileAsByteArray(file.getPath());
                    file.setContent(content);
                    log.debug("Loaded content for file: {}, size: {} bytes", file.getPath(), 
                            content != null ? content.length : 0);
                } catch (Exception e) {
                    log.error("Failed to load content for file: " + file.getPath(), e);
                    // Depending on your error handling strategy, you might want to:
                    // 1. Return null (skipping this file)
                    // 2. Proceed with file.getContent() == null (the processor will need to handle this)
                    // 3. Throw a runtime exception
                    return null;
                }
            }
            
            // Create a message with the FileDataWrapper as payload
            // Add any headers that might be needed downstream
            Message<FileDataWrapper> message = MessageBuilder.withPayload(file)
                    .setHeader("file_path", file.getPath())
                    .setHeader("file_name", file.getName())
                    .setHeader("file_timestamp", file.getLastModifiedTimestamp())
                    .build();
            
            // "Claim" the file to prevent it from being processed again
            // This depends on your AbstractRecursiveDirectoryScanner implementation
            boolean claimed = scanner.tryClaim(file);
            
            if (!claimed) {
                log.warn("Failed to claim file: {}, it might be processed again", file.getPath());
                // Depending on your strategy, you might want to return null here
            }
            
            return message;
        } catch (Exception e) {
            log.error("Error receiving message from Samba scanner", e);
            return null;
        }
    }
}