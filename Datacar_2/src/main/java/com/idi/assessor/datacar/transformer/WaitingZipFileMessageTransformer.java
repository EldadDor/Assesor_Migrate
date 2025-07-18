package com.idi.assessor.datacar.transformer;

import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Component("waitingZipFileMessageTransformer")
public class WaitingZipFileMessageTransformer {

    // Default method for a transformer bean, or add @Transformer to a specific method.
    // The input type will be the output of the header enricher (Message<FileDataWrapper> from the chain definition)
    // The output type depends on what the next component (zipFileProcessingChannel input) expects.
    @Transformer
    public Message<FileDataWrapper> transform(Message<FileDataWrapper> inputMessage) {
        FileDataWrapper fileData = inputMessage.getPayload();
        System.out.println("WaitingZipFileMessageTransformer processing: " + fileData.getName());

        // TODO: Implement transformation logic if needed.
        // Example: if FileDataWrapper's content is null, load it using SambaFileUtil
        // if (fileData.getContent() == null && sambaFileUtil != null) { // Assuming sambaFileUtil is available
        //     byte[] content = sambaFileUtil.getFileContent(fileData.getPath());
        //     fileData.setContent(content);
        // }
        // For now, passing through.
        return inputMessage;
    }
}
