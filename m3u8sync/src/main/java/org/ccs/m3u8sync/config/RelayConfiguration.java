package org.ccs.m3u8sync.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "relay")
@Data
public class RelayConfiguration {

    /**
     * 用于级联服务，级联下一节点
     */
    private String nextM3u8Sync;
    /**
     * 本机服务，用于下一节点同步完成回调
     */
    private String localM3u8Sync;
    /**
     * 是否开启下载任务
     */
    private boolean open = false;

    /**
     * 下载完成通知删除原服务的文件
     */
    private boolean deleteOnSuccess=false;
}
