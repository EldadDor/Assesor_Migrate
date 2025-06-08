package com.idi.assessor.datacar.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.scanner.AbstractRecursiveDirectoryScanner;

/**
 * Custom MessageSource to poll files from a Samba share using a specialized
 * scanner.
 */
public class SambaMessageSource implements MessageSource<FileDataWrapper> {

    private final AbstractRecursiveDirectoryScanner<?, ?, ?> sambaScanner; // Use the abstract type
    private List<FileDataWrapper> availableFiles;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public SambaMessageSource(AbstractRecursiveDirectoryScanner<?, ?, ?> sambaScanner) {
        this.sambaScanner = sambaScanner;
    }

    @Override
    public Message<FileDataWrapper> receive() {
        if (availableFiles == null || currentIndex.get() >= availableFiles.size()) {
            // Time to poll for new files
            this.availableFiles = sambaScanner.scanForFiles(); // Use the new method
            this.currentIndex.set(0);
            if (this.availableFiles == null || this.availableFiles.isEmpty()) {
                return null; // No files found in this poll
            }
        }

        if (currentIndex.get() < availableFiles.size()) {
            FileDataWrapper fileData = availableFiles.get(currentIndex.getAndIncrement());
            // Here you might decide if content should be loaded if it's not already
            // For example, if sambaScanner.scanForFiles() only loads metadata:
            // if (fileData.getContent() == null && sambaScanner.getSambaFileUtil() != null) { // Assuming getter for util
            //     byte[] content = sambaScanner.getSambaFileUtil().getFileContent(fileData.getPath());
            //     fileData.setContent(content);
            // }
            return MessageBuilder.withPayload(fileData).build();
        } else {
            return null; // Should have been caught by the poll above, but as a safeguard
        }
    }
}
