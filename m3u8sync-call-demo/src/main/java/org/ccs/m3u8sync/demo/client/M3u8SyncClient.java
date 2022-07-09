package org.ccs.m3u8sync.demo.client;

import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.demo.config.M3u8AsyncDemoConfiguration;
import org.ccs.m3u8sync.demo.exceptions.ResultCode;
import org.ccs.m3u8sync.demo.exceptions.ResultData;
import org.ccs.m3u8sync.demo.utils.CommUtils;
import org.ccs.m3u8sync.demo.vo.CallbackVo;
import org.ccs.m3u8sync.demo.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;

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


    public ResultData addSync(String roomId, String format, String m3u8Url, CallbackVo callback) {
        HttpHeaders requestHeaders = initPostHeader();
        String apiUrl = CommUtils.appendUrl(m3u8AsyncDemoConfiguration.getApiUrl(), "downup/add");
        apiUrl += "?roomId=" + roomId;
        if (StringUtils.isNotBlank(m3u8Url)) {
            apiUrl += "&m3u8Url=" + URLEncoder.encode(m3u8Url);
        }
        if (StringUtils.isNotBlank(format)) {
            apiUrl += "&format=" + URLEncoder.encode(format);
        }

        String bodyString = JSONUtil.toJsonStr(callback);
        log.info("----addSync--roomId={}, bodyString={}", roomId, bodyString);

        HttpEntity<String> httpEntitys = new HttpEntity<>(bodyString, requestHeaders);
        String resultRemote = null;
        ResultData resultData = null;
        try {
            URI uri = new URI(apiUrl);
            ResponseEntity<String> exchanges = restTemplate.postForEntity(uri, httpEntitys, String.class);
            resultRemote = exchanges.getBody();
            log.info("----addSync--roomId={}, resultRemote={}", roomId, resultRemote);
            resultData = getResultData(resultRemote);
        } catch (Exception e) {
            log.error("----addSync--roomId={}, apiUrl={}", roomId, apiUrl, e);
            return ResultData.error("roomId=" + roomId + ", error=" + e.getMessage());
        }
        return resultData;
    }

    private ResultData getResultData(String jsonBody) {
        JSONObject jsonObject = JSONUtil.parseObj(jsonBody);
        String resultCode = jsonObject.getStr("resultCode");
        String errorMsg = jsonObject.getStr("errorMsg");
        Object data = jsonObject.getObj("data", null);
        if("null".equals(data)|| JSONNull.NULL==data) {
            data=null;
        }
        String errorType = jsonObject.getStr("errorType");
        ResultData resultData=null;
        if (ResultCode.DEFAULT_SUCCESS_CODE.equals(resultCode)) {
            resultData = ResultData.success(data);
        } else {
            resultData = ResultData.error(resultCode, errorMsg);
            resultData.setErrorType(errorType);
        }
        return resultData;
    }

    public M3u8FileInfoVo getM3u8Info(String roomId, String format) {
        HttpHeaders requestHeaders = initPostHeader();
        String url = CommUtils.appendUrl(m3u8AsyncDemoConfiguration.getApiUrl(), "m3u8/m3u8Info");
        url = url + "?roomId=" + roomId + "&format=" + format;


        log.info("----getM3u8Info--roomId={}", roomId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, requestHeaders);
        ResponseEntity<String> exchanges = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        String resultRemote = exchanges.getBody();
        log.info("----getM3u8Info--roomId={}, resultRemote={}", roomId, resultRemote);
        return JSONUtil.parse(resultRemote).toBean(M3u8FileInfoVo.class);
    }
}
