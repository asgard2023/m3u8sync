package org.ccs.m3u8sync.demo.client;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.demo.config.M3u8AsyncDemoConfiguration;
import org.ccs.m3u8sync.demo.utils.CommUtils;
import org.ccs.m3u8sync.demo.vo.CallbackVo;
import org.ccs.m3u8sync.demo.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class M3u8SyncClient {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private M3u8AsyncDemoConfiguration m3u8AsyncDemoConfiguration;

    private HttpHeaders initPostHeader() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("source", "m3u8sync");
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        return requestHeaders;
    }


    public String addSync(@RequestParam("roomId") String roomId, @RequestParam("format") String format, @RequestParam("url") String url, CallbackVo callback) {
        HttpHeaders requestHeaders = initPostHeader();
        String apiUrl = CommUtils.appendUrl(m3u8AsyncDemoConfiguration.getApiUrl(), "downup/add");
        apiUrl = apiUrl + "?roomId=" + roomId+"&url="+url+"&format="+format;

        String bodyString = JSONUtil.toJsonStr(callback);
        log.info("----addSync--roomId={}, bodyString={}", roomId, bodyString);

        HttpEntity<String> httpEntitys = new HttpEntity<>(bodyString, requestHeaders);
        ResponseEntity<String> exchanges = restTemplate.postForEntity(apiUrl, httpEntitys, String.class);
        String resultRemote = exchanges.getBody();
        log.info("----addSync--roomId={}, resultRemote={}", roomId, resultRemote);
        return resultRemote;
    }

    public M3u8FileInfoVo getM3u8Info(@RequestParam("roomId") String roomId) {
        HttpHeaders requestHeaders = initPostHeader();
        String url = CommUtils.appendUrl(m3u8AsyncDemoConfiguration.getApiUrl(), "m3u8/m3u8Info");
        url = url + "?roomId=" + roomId;


        log.info("----getM3u8Info--roomId={}", roomId);
        ResponseEntity<String> exchanges = restTemplate.getForEntity(url, String.class);
        String resultRemote = exchanges.getBody();
        log.info("----getM3u8Info--roomId={}, resultRemote={}", roomId, resultRemote);
        M3u8FileInfoVo fileInfo = JSONUtil.parse(resultRemote).toBean(M3u8FileInfoVo.class);
        return fileInfo;
    }
}
