package org.ccs.m3u8sync.constants;

public enum SyncType {
    /**
     * m3u8模式，读取m3u8文件内所有的ts，同步m3u8及所有ts文件（不在m3u8内的文件不管）
     */
    M3U8("m3u8", "m3u8同步"),
    /**
     * 在nginx开启文件列表显示时，可同步nginx目录指定目录递归所有子目录的所有文件，
     */
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
