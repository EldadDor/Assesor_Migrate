package com.idi.assessor.datacar.handler;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("dataCarFinishedZipMessageHandler")
public class DataCarFinishedZipMessageHandler {

    // Method from XML: <integration:service-activator ... ref="dataCarFinishedZipMessageHandler" method="processFinished"/>
    // Input from completedMessageChannel, no explicit output channel (one-way handler)
    // The input payload type is the output of DataCarCatalogFileHandler (String in the stub)
    public void processFinished(Message<String> inputMessage) {
        String data = inputMessage.getPayload();
        System.out.println("DataCarFinishedZipMessageHandler: Processing finished for: " + data);
        // Implement final processing logic
    }
}
