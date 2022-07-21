package org.ccs.m3u8sync.client;

import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.config.RelayConfiguration;
import org.ccs.m3u8sync.exceptions.ResultCode;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;

/**
 * 如是中继模式时，用于调用下一个服务
 */
@Service
@Slf4j
public class NextM3u8SyncRest {
    @Autowired
    private RelayConfiguration relayConfiguration;
    @Autowired
    private RestTemplate restTemplate;

    @Value("${server.port}")
    private int port;

    private HttpHeaders initPostHeader() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("source", "m3u8sync");
        return requestHeaders;
    }

    /**
     * 通知下一节点，并回调通知本节点
     *
     * @param roomId
     * @param format
     * @param m3u8Url
     * @return
     */
    public ResultData addSync(String roomId, String format, String m3u8Url, Integer ifRelayCallDel, CallbackVo callback) {
        HttpHeaders requestHeaders = initPostHeader();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        String nextM3u8SyncUrl = relayConfiguration.getNextM3u8Sync();
        String apiUrl = CommUtils.appendUrl(nextM3u8SyncUrl, "downup/add");
        apiUrl += "?roomId=" + roomId;
        apiUrl += "&ifRelayCallDel=" + ifRelayCallDel;
        if (StringUtils.isNotBlank(m3u8Url)) {
            apiUrl += "&m3u8Url=" + URLEncoder.encode(m3u8Url);
        }
        if (StringUtils.isNotBlank(format)) {
            apiUrl += "&format=" + URLEncoder.encode(format);
        }

        String bodyString = JSONUtil.toJsonStr(callback);
        log.info("----addSync--roomId={}, bodyString={}", roomId, bodyString);
        HttpEntity<String> httpEntitys = new HttpEntity<>(bodyString, requestHeaders);
        ResponseEntity<String> exchanges = restTemplate.postForEntity(apiUrl, httpEntitys, String.class);
        String resultRemote = exchanges.getBody();
        log.info("----addSync--roomId={}, resultRemote={}", roomId, resultRemote);
        return getResultData(resultRemote);
    }

    /**
     * 通知下一节点，并回调通知本节点
     *
     * @param roomId
     * @param successDel
     * @param fileInfo
     * @return
     */
    public ResultData callbackDel(String roomId, String successDel, M3u8FileInfoVo fileInfo) {
        HttpHeaders requestHeaders = initPostHeader();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        String nextM3u8SyncUrl = relayConfiguration.getNextM3u8Sync();
        String apiUrl = CommUtils.appendUrl(nextM3u8SyncUrl, "downup/callbackDel");
        if (StringUtils.isNotBlank(successDel)) {
            apiUrl += "&successDel=" + URLEncoder.encode(successDel);
        }

        String bodyString = JSONUtil.toJsonStr(fileInfo);
        log.info("----callbackDel--roomId={}, bodyString={}", roomId, bodyString);
        HttpEntity<String> httpEntitys = new HttpEntity<>(bodyString, requestHeaders);
        ResponseEntity<String> exchanges = restTemplate.postForEntity(apiUrl, httpEntitys, String.class);
        String resultRemote = exchanges.getBody();
        log.info("----callbackDel--roomId={}, resultRemote={}", roomId, resultRemote);
        return getResultData(resultRemote);
    }


    private ResultData getResultData(String jsonBody) {
        JSONObject jsonObject = JSONUtil.parseObj(jsonBody);
        String resultCode = jsonObject.getStr("resultCode");
        String errorMsg = jsonObject.getStr("errorMsg");
        Object data = jsonObject.getObj("data", null);
        if ("null".equals(data) || JSONNull.NULL == data) {
            data = null;
        }
        String errorType = jsonObject.getStr("errorType");
        ResultData resultData = null;
        if (ResultCode.DEFAULT_SUCCESS.getCode().equals(resultCode)) {
            resultData = ResultData.success(data);
        } else {
            resultData = ResultData.error(resultCode, errorMsg);
            resultData.setErrorType(errorType);
        }
        return resultData;
    }
}
