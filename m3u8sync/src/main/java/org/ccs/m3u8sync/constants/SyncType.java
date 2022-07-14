package org.ccs.m3u8sync.constants;

public enum SyncType {
    M3U8("m3u8", "m3u8同步"),
    FILE("file", "file同步(nginx)");


    private String type;
    private String desc;

    SyncType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static SyncType parse(String syncType) {
        if (syncType == null) {
            return null;
        }
        SyncType[] types = SyncType.values();
        for (SyncType type : types) {
            if (type.type.equals(syncType)) {
                return type;
            }
        }
        return null;
    }

}
