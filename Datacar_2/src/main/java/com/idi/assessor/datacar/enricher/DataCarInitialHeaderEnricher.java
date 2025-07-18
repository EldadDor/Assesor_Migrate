package com.idi.assessor.datacar.enricher;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Component("dataCarInitialHeaderEnricher")
public class DataCarInitialHeaderEnricher {

    // Method name from XML: <integration:header name="file-name" ref="dataCarInitialHeaderEnricher" method="enrichHeader"/>
    public String enrichHeader(Message<Object> message) {
        FileDataWrapper fileData = (FileDataWrapper) message.getPayload();
        if (fileData != null) {
            return fileData.getName();
        }
        return "unknown-file-data";
    }
}
