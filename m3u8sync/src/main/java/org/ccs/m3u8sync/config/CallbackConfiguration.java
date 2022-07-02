package org.ccs.m3u8sync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "callback")
@Data
public class CallbackConfiguration {
    private String baseUrl;
    private String paramUrl;
    private boolean open=false;
}
