package com.idi.assessor.datacar.handler;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("dataCarCatalogFileHandler")
// Equivalent to <aop:scoped-proxy/>. Consider if prototype is the correct scope 
// (e.g., if it holds state per invocation) or if another scope like request/session is more appropriate
// depending on the broader application context if this were a web application.
// For typical SI handlers, prototype ensures a new instance if statefulness is a concern per message.
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DataCarCatalogFileHandler {

    // Method from XML: <integration:service-activator ... ref="dataCarCatalogFileHandler" method="buildCatalogZipFileContents"/>
    // Input from buildingCatalogingDataChannel, output to completedMessageChannel
    // The input payload type is the output of ZipMessageServiceHandler (String in the stub)
    public Message<?> buildCatalogZipFileContents(Message<String> inputMessage) {
        String data = inputMessage.getPayload();
        System.out.println("DataCarCatalogFileHandler: Building catalog for: " + data);
        // Implement catalog building logic
        String catalogResult = "Cataloged_" + data;
        return org.springframework.integration.support.MessageBuilder.withPayload(catalogResult)
                .copyHeaders(inputMessage.getHeaders())
                .build();
    }
}
