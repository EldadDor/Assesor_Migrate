package com.idi.assessor.datacar.handler;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

// import java.io.File; // No longer primary type for error payloads
@Component("dataCarErrorFileHandler")
public class DataCarErrorFileHandler {

    // Method for errorFilesChannel: <integration:service-activator ... ref="dataCarErrorFileHandler" method="processError"/>
    // Assuming the payload on errorFilesChannel could be a File or related error information.
    // Adjust parameter type as needed based on what's sent to errorFilesChannel.
    public void processError(Message<?> errorMessage) {
        System.err.println("DataCarErrorFileHandler (processError): Received error with payload: " + errorMessage.getPayload());

        if (errorMessage.getPayload() instanceof FileDataWrapper) {
            FileDataWrapper errorFile = (FileDataWrapper) errorMessage.getPayload();
            System.err.println("Error processing FileDataWrapper: " + errorFile.getPath());
            // Implement logic to handle the erroneous FileDataWrapper (e.g., log, move to an error location via SambaFileUtil)
        } else if (errorMessage.getPayload() instanceof Throwable) {
            Throwable throwable = (Throwable) errorMessage.getPayload();
            System.err.println("Error details (Throwable payload): ");
            throwable.printStackTrace();
        } else {
            System.err.println("Error payload is of unhandled type: " + errorMessage.getPayload().getClass().getName());
        }
        // Add more specific error handling logic
    }

    // Method for errorChannel: <integration:service-activator ... ref="dataCarErrorFileHandler" method="handleMessageError"/>
    // Payloads on the global errorChannel are typically MessagingException
    public void handleMessageError(MessagingException messagingException) {
        System.err.println("DataCarErrorFileHandler (handleMessageError): Handling MessagingException for message: "
                + (messagingException.getFailedMessage() != null ? messagingException.getFailedMessage().toString() : "null (MessagingException itself might be the message)"));
        System.err.println("Exception details: ");
        messagingException.printStackTrace();

        Message<?> failedMessage = messagingException.getFailedMessage();
        if (failedMessage != null && failedMessage.getPayload() instanceof FileDataWrapper) {
            FileDataWrapper errorFile = (FileDataWrapper) failedMessage.getPayload();
            System.err.println("Failed message payload was FileDataWrapper: " + errorFile.getPath());
            // Add specific handling for FileDataWrapper here as well, 
            // e.g., sambaFileUtil.moveToErrorLocation(errorFile.getPath(), errorFile.getContent());
        }
    }
}
