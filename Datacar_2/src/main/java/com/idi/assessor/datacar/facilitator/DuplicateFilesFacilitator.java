package com.idi.assessor.datacar.facilitator;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("duplicateFilesFacilitator")
public class DuplicateFilesFacilitator {

    // In XML: fixed-delay="1200000"
    // This translates to 1200000 ms = 20 minutes
    @Scheduled(fixedDelay = 1200000)
    public void facilitateDuplicateFilesCleanup() {
        System.out.println("Facilitating duplicate files cleanup...");
        // Implement cleanup logic here
    }
}
