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
    /**
     * 最大允许重复任务的执行间隔,单位小时
     */
    private Integer timeInterval = 20;
    /**
     * 同时下载m3u8的并发线程数
     */
    private Integer taskNum = 1;
    /**
     * 每个m3u8任务下载并发线程数
     */
    private Integer taskThreadCount = 5;
    /**
     * 最大下载线程数
     */
    private Integer threadMax = 50;
    /**
     * 线程进行url下载的超时时间（文件越大，网络越慢，这个时间越长）
     */
    private Integer threadDownloadTimeout = 30;
    /**
     * 是否开启下载任务
     */
    private boolean open = true;
    /**
     * format用于与nginx-url动态生成m3u8的url
     */
    private String format= "{roomId}/{roomId}.m3u8";
    /**
     * 过期时间，单位小时
     */
    private Integer expireHour;
    /**
     * nginx下载地址
     */
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
        return CommUtils.appendUrl(this.getDownPath(), path);
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
