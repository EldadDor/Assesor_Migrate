package com.idi.assessor.datacar.integration;

import org.springframework.integration.annotation.MessagingGateway;

// XML: <integration:gateway id="messageRequestGateway" service-interface="com.idi.ifs.datacar.integration.DatacarMessageRequestGateway"/>
// Assuming the package and interface name should be updated for the current project structure.
@MessagingGateway(name = "messageRequestGateway") // Default request channel can be configured here if needed
public interface DatacarMessageRequestGateway {

    // Define methods that will send messages to integration flows
    // For example:
    // void sendData(Object data);
    // Message<?> processAndGetData(Object data); 
    // Add specific methods based on how this gateway is used.
    // The original XML did not specify methods, so this is a basic placeholder.
}
