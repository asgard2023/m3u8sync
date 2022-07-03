package org.ccs.m3u8sync.config;

import lombok.Data;
import org.ccs.m3u8sync.utils.CommUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 下载配置
 */
@Component
@ConfigurationProperties(prefix = "downup")
@Data
public class DownUpConfig {
    private Integer timeInterval = 20;
    private Integer taskNum = 1;
    private Integer taskThreadCount = 5;
    private Integer threadMax = 50;
    private boolean open = true;
    private String format= "{roomId}/{roomId}.m3u8";
    /**
     * 过期时间，单位小时
     */
    private Integer expireHour;

    private String nginxUrl;
    private String downPath = "/data/down/";

    public String getNginxUrl(String roomId, String format) {
        String path=format.replace("{roomId}", roomId).replace("{roomId}", roomId);
        return CommUtils.appendUrl(nginxUrl, path);
    }

    /**
     * 获取回放本地目录
     *
     * @param roomId
     * @return
     */
    public String getRoomIdFilePath(String roomId) {
        return CommUtils.appendUrl(downPath, roomId);
    }
    /**
     * 获取本地m3u8文件
     *
     * @param roomId
     * @return
     */
    public String getRoomM3u8Path(String roomId, String format) {
        String path=format.replace("{roomId}", roomId).replace("{roomId}", roomId);
        return CommUtils.appendUrl(getRoomIdFilePath(roomId), path);
    }

    /**
     * 获取本地m3u8文件
     *
     * @param roomId
     * @return
     */
    public String getRoomM3u8Path(String roomId) {
        return getRoomM3u8Path(roomId, format);
    }

}
