package org.ccs.m3u8sync.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "m3u8async")
@Data
public class M3u8AsyncDemoConfiguration {
    private String apiUrl;
    private String callbackUrl;
    private String callParamUrl;
}
