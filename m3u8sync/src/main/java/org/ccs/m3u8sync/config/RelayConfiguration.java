package org.ccs.m3u8sync.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "relay")
@Data
public class RelayConfiguration {
    /**
     * 是否开启中继模式
     */
    private boolean open = false;

    private String code;

    private String relayNiginx="http://localhost:9200";

    /**
     * 用于级联服务，级联下一节点
     */
    private String nextM3u8Sync;
    /**
     * 本机服务，用于下一节点同步完成回调
     */
    private String localM3u8Sync;


    /**
     * 用于中继开始时，通知下节点下载完成，回调callbackDel删除本节点的文件
     */
    private boolean deleteOnSuccess = false;
}
