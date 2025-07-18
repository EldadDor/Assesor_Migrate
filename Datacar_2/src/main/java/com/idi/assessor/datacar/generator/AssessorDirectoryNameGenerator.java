package com.idi.assessor.datacar.generator;

import org.springframework.integration.file.FileNameGenerator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Component("assessorDirectoryNameGenerator") // Bean name from XML
public class AssessorDirectoryNameGenerator implements FileNameGenerator {

    /**
     * Generates a file name for the outbound message. The payload is expected
     * to be a FileDataWrapper.
     *
     * @param message The message containing the FileDataWrapper payload.
     * @return The generated file name.
     */
    @Override
    public String generateFileName(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof FileDataWrapper) {
            FileDataWrapper fileData = (FileDataWrapper) payload;
            // Basic implementation: returns the original name from FileDataWrapper.
            // You might want to add more sophisticated logic here, 
            // e.g., adding timestamps, prefixes, or modifying based on headers.
            return fileData.getName();
        } else {
            // Fallback or error handling if payload is not FileDataWrapper
            // This might depend on how your flows are structured.
            // For now, returning a default name or could throw an exception.
            return "unknown_file_" + System.currentTimeMillis();
        }
    }
}
