package org.ccs.m3u8sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 系统参数配置
 *
 * @author chenjh
 */
@Configuration
@ConfigurationProperties(prefix = "m3u8sync")
@Data
public class M3u8SyncConfiguration {
    /**
     * #自定义异常，异常日志类型simple/full
     */
    private String exceptionLogType="simple";
}
