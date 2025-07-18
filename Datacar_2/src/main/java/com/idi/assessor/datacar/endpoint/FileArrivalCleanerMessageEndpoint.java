package com.idi.assessor.datacar.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.util.SambaFileUtil;

@Component("fileArrivalCleanerMessageEndpoint")
public class FileArrivalCleanerMessageEndpoint {

    private final SambaFileUtil sambaFileUtil;

    @Autowired
    public FileArrivalCleanerMessageEndpoint(SambaFileUtil sambaFileUtil) {
        this.sambaFileUtil = sambaFileUtil;
    }

    // Method from XML: <integration:outbound-channel-adapter ... ref="fileArrivalCleanerMessageEndpoint" method="processMessageArrival"/>
    public void processMessageArrival(Message<FileDataWrapper> message) {
        FileDataWrapper fileData = message.getPayload();
        System.out.println("FileArrivalCleanerMessageEndpoint: Processing arrival for file: " + fileData.getPath());

        // Implement logic, e.g., deleting the original file from Samba
        // boolean deleted = sambaFileUtil.deleteFile(fileData.getPath());
        // if (deleted) {
        //     System.out.println("FileArrivalCleanerMessageEndpoint: Deleted file from Samba: " + fileData.getPath());
        // } else {
        //     System.err.println("FileArrivalCleanerMessageEndpoint: Failed to delete file from Samba: " + fileData.getPath());
        // }
    }
}
