package org.ccs.m3u8sync;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.config.CallbackConfiguration;
import org.ccs.m3u8sync.config.RelayConfiguration;
import org.ccs.m3u8sync.constants.SyncType;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.ccs.m3u8sync.downup.service.DownUpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.Map;

@SpringBootTest(classes = M3u8SyncApplication.class)
@ActiveProfiles(value = "test")
@Slf4j
public class DownUpServiceTest {
    @Autowired
    private DownUpService downUpService;
    @Autowired
    private RelayConfiguration relayConfiguration;
    @Autowired
    private CallbackConfiguration callbackConfiguration;

    @Test
    void status() {
        Map<String, Object> resultMap = null;
        resultMap = downUpService.status("help");
        System.out.println(resultMap);
        Assertions.assertTrue(resultMap.containsKey("usage"), "has usage");

        resultMap = downUpService.status("all");
        System.out.println(resultMap);
        Assertions.assertTrue(resultMap.containsKey("errors"), "has errors");

        resultMap = downUpService.status("config");
        System.out.println(resultMap);
        Assertions.assertTrue(resultMap.containsKey("callbackConfig"), "has callbackConfig");

        resultMap = downUpService.status("errors");
        System.out.println(resultMap);
        Assertions.assertTrue(resultMap.containsKey("errors"), "has errors");
    }

    @Test
    void doTaskDown() {
        relayConfiguration.setOpen(false);
        callbackConfiguration.setOpen(false);
        DownBean downBean = new DownBean();
        downBean.setRoomId("1025050251");
        downBean.setUrl("http://175.178.252.112:81/m3u8/live/1025050251/1025050251.m3u8");
        downBean.setSyncType(SyncType.M3U8.getType());
        downBean.setInitTime(new Date());
        DownErrorInfoVo downErrorInfoVo = this.downUpService.doTaskDown(downBean);
        System.out.println(JSONUtil.toJsonStr(downErrorInfoVo));
    }
}
