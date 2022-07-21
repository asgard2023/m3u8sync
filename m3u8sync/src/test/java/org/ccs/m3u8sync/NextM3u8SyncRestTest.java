package org.ccs.m3u8sync;

import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.client.NextM3u8SyncRest;
import org.ccs.m3u8sync.config.CallbackConfiguration;
import org.ccs.m3u8sync.downup.service.DownUpService;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = M3u8SyncApplication.class)
@ActiveProfiles(value = "test")
@Slf4j
public class NextM3u8SyncRestTest {
    @Autowired
    private NextM3u8SyncRest nextM3u8SyncRest;
    @Autowired
    private CallbackConfiguration callbackConfiguration;

    @Test
    public void addSync() {
        CallbackVo callback = new CallbackVo();
        callback.setBaseUrl(callbackConfiguration.getBaseUrl());
        callback.setParamUrl(callbackConfiguration.getParamUrl());
        ResultData resultData = nextM3u8SyncRest.addSync(DownUpService.CHECK_RELAY, "test", "test", 0, callback);
        System.out.println(resultData.getResultCode() + " " + resultData.getData());
    }
}
