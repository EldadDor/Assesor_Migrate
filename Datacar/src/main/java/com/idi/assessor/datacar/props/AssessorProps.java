package com.idi.assessor.datacar.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "assessor")
public class AssessorProps {

    private String inFilesDir;
    private String waitingFilesInDir;

    public String getInFilesDir() {
        return inFilesDir;
    }

    public void setInFilesDir(String inFilesDir) {
        this.inFilesDir = inFilesDir;
    }

    public String getWaitingFilesInDir() {
        return waitingFilesInDir;
    }

    public void setWaitingFilesInDir(String waitingFilesInDir) {
        this.waitingFilesInDir = waitingFilesInDir;
    }
}
