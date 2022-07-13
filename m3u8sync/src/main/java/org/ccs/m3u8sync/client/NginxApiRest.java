package org.ccs.m3u8sync.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.exceptions.FailedException;
import org.ccs.m3u8sync.utils.CommUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取nginx的接口
 *
 * @author chenjh
 */
@Service
@Slf4j
public class NginxApiRest {
    @Autowired
    private DownUpConfig downUpConfig;
    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders initPostHeader() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("source", "m3u8sync");
        return requestHeaders;
    }

    public List<String> getM3u8List(String path) {
        String nginxUrl = downUpConfig.getNginxUrl();
        if (StringUtils.isNotBlank(path)) {
            nginxUrl = CommUtils.appendUrl(nginxUrl, path);
        }
        HttpHeaders requestHeaders = initPostHeader();
        Map<String, String> paramMap = new HashMap<>(8);
        HttpEntity<Object> requestEntity = new HttpEntity<>(paramMap, requestHeaders);

        List<String> roomIdList = new ArrayList<>();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(nginxUrl, HttpMethod.GET, requestEntity, String.class);
            String[] lines = exchange.getBody().split("\n");
            for (String line : lines) {
                line = line.trim();
                int idx = line.indexOf("<a href=\"");
                if (idx >= 0) {
                    String linkInfo = line.substring(idx + "<a href=\"".length());
                    String roomId = linkInfo.substring(0, linkInfo.indexOf("/\">"));
                    if ("..".equals(roomId)) {
                        continue;
                    }
                    roomIdList.add(roomId);
                }
            }
        } catch (Exception e) {
            log.error("---getM3u8List--error={}", e.getMessage(), e);
            throw new FailedException(nginxUrl + " " + e.getMessage());
        }
        return roomIdList;
    }
}
