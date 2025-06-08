package com.idi.assessor.datacar.config;

public enum ChannelsNamesEnum {
    BACKUP_FILES("backupFilesChannel"),
    ERROR_FILES("errorFilesChannel"),/*  */
    TERMINAL_FILES("terminalFilesChannel"),
    OUT_WAITING_CATALOG_FILES("waitingFilesOutChannel"),
    IN_WAITING_CATALOG_FILES("inCatalogFilesChannel");

    private String name;

    ChannelsNamesEnum(String filesChannel) {
        name = filesChannel;
    }

    public String getName() {
        return name;
    }

    public static ChannelsNamesEnum getValueFromName(String channelName) {
        final ChannelsNamesEnum[] values = values();
        for (final ChannelsNamesEnum value : values) {
            if (value.getName().equals(channelName)) {
                return value;
            }
        }
        return null;
    }

    public String getChannelDescriptor() {
        return name().split("_")[0];
    }
}
