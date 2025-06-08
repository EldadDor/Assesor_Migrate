package com.idi.assessor.datacar.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.util.SambaFileUtil;
// Assuming payload is a FileDataWrapper
// import java.io.File; // No longer used

@Component("zipMessageServiceHandler")
public class ZipMessageServiceHandler {

    private SambaFileUtil sambaFileUtil; // Optional: if content needs to be fetched here

    @Autowired(required = false) // Make optional if content is always pre-loaded
    public void setSambaFileUtil(SambaFileUtil sambaFileUtil) {
        this.sambaFileUtil = sambaFileUtil;
    }

    // Method from XML: <integration:service-activator ... ref="zipMessageServiceHandler" method="processZipFile"/>
    // Input comes from zipFileProcessingChannel, output goes to buildingCatalogingDataChannel
    public Message<?> processZipFile(Message<FileDataWrapper> inputMessage) { // Expect FileDataWrapper
        FileDataWrapper fileData = inputMessage.getPayload();
        System.out.println("ZipMessageServiceHandler: Processing zip file: " + fileData.getName());

        // Ensure content is loaded if not already
        byte[] content = fileData.getContent();
        if (content == null) {
            if (sambaFileUtil != null) {
                System.out.println("ZipMessageServiceHandler: Lazily loading content for " + fileData.getPath());
                content = sambaFileUtil.getFileContent(fileData.getPath());
                // It might be good to update the FileDataWrapper if it's mutable and shared, 
                // but here we are creating a new message payload.
            } else {
                System.err.println("ZipMessageServiceHandler: SambaFileUtil not available to load content for " + fileData.getPath());
                // Handle error - throw exception or return error message
                return org.springframework.integration.support.MessageBuilder
                        .withPayload("Error: Content not available for " + fileData.getName())
                        .copyHeaders(inputMessage.getHeaders())
                        .build();
            }
        }

        // TODO: Implement actual zip file processing logic using the 'content' byte[]
        // For example, decompress, inspect entries, etc.
        // The return value will be sent to 'buildingCatalogingDataChannel'
        String resultPayload = "Processed_Zip_Data_From_" + fileData.getName();
        return org.springframework.integration.support.MessageBuilder.withPayload(resultPayload)
                .copyHeaders(inputMessage.getHeaders())
                .build();
    }
}
