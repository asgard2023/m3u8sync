package org.ccs.m3u8sync;

import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.client.NginxApiRest;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = M3u8SyncApplication.class)
@ActiveProfiles(value = "test")
@Slf4j
public class NginxApiRestTest {
    @Autowired
    private NginxApiRest nginxApiRest;
    @Autowired
    private DownUpConfig downUpConfig;

    @Test
    public void getM3u8List() {
        List<String> roomIdList = nginxApiRest.getM3u8List(null);
        List<String> okList = new ArrayList<>();
        String format = downUpConfig.getFormat();
        for (String roomId : roomIdList) {
            String m3u8Url = downUpConfig.getNginxUrl(roomId, format);
            Long length = DownLoadUtil.getRemoteSize(m3u8Url, 3000);
            if (length == null || length == 0) {
                log.warn("----add--roomId={} m3u8Url={} size={} get fail", roomId, m3u8Url, length);
                continue;
            }
            okList.add(roomId);
        }
        System.out.println(roomIdList);
        System.out.println(okList);
        Assertions.assertTrue(okList.size()>0, "okList gt 0");
    }
}