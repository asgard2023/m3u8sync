package org.ccs.m3u8sync.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"org.ccs.opendfl.core", "org.ccs.opendfl.mysql", "org.ccs.m3u8sync"})
@EnableConfigurationProperties
@EnableScheduling
@EnableAsync
public class M3u8SyncSecurityApplication {
    public static final Logger logger = LoggerFactory.getLogger(M3u8SyncSecurityApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(M3u8SyncSecurityApplication.class, args);
    }

}
