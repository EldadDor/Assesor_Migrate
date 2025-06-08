package com.idi.assessor.datacar.util;

import org.springframework.stereotype.Component;

@Component("directoryExpressionSafeCreator")
public class DirectoryExpressionSafeCreator {

    public String getBackupDir() {
        // Implement logic to return backup directory path
        return "/tmp/backup";
    }

    public String getTerminalDir() {
        // Implement logic to return terminal directory path
        return "/tmp/terminal";
    }

    public String getWaitingFilesDir() {
        // Implement logic to return waiting files directory path
        return "/tmp/waiting";
    }

    public String getDuplicatesFilesDir() {
        // Implement logic to return duplicates files directory path
        return "/tmp/duplicates";
    }
}
