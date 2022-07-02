package org.ccs.m3u8sync.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@EnableAsync
public class M3u8SyncDemoApplication {
    public static final Logger logger = LoggerFactory.getLogger(M3u8SyncDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(M3u8SyncDemoApplication.class, args);
    }

    //自创建RestTemplate
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);// 设置连接超时，单位毫秒
        requestFactory.setReadTimeout(30000);  //设置读取超时
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        logger.info("RestTemplate initialization complete");
        return restTemplate;
    }

}
