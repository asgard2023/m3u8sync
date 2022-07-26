package org.ccs.m3u8sync;

import org.ccs.m3u8sync.utils.CommUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class CommUtilsTest {
    @Test
    void asUrlParams() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("roomId", "123");
        paramMap.put("event", "test");
        paramMap.put("tryUrl", "aaaa");
        String url = CommUtils.asUrlParams(paramMap);
        System.out.println(url);
        Assertions.assertEquals("tryUrl=aaaa&event=test&roomId=123", url, "urlParam");
    }

    void replaceBlank(){
        String str="03-Jul-2022 08:55    2348";

    }

    @Test
    void nvl(){
        String v=(String)CommUtils.nvl(null, "123");
        Assertions.assertEquals("123", v, "nvl first null data");
        v=(String)CommUtils.nvl("124", "123");
        Assertions.assertEquals("124", v, "nvl first null data");
    }

    @Test
    void appendUrl(){
        String url="http://www.baidu.com";
        String type="123";
        String urlResult= CommUtils.appendUrl(url, type);
        Assertions.assertEquals("http://www.baidu.com/123", urlResult);

        type="/123";
        urlResult= CommUtils.appendUrl(url, type);
        Assertions.assertEquals("http://www.baidu.com/123", urlResult);

        url="http://www.baidu.com/";
        urlResult= CommUtils.appendUrl(url, type);
        Assertions.assertEquals("http://www.baidu.com/123", urlResult);
    }

    @Test
    void getBaseUrl(){
        String url="http://www.baidu.com/q=123";
        String v=CommUtils.getBaseUrl(url);
        Assertions.assertEquals("http://www.baidu.com", v);
    }

    @Test
    void getRelayUrl(){
        String url="http://175.178.252.112:81/m3u8/live/12344678/index.m3u8";
        String roomId="12344678";
        String relayNginxUrl="http://relay1:80/down";
        url=CommUtils.getRelayUrl(url, roomId, relayNginxUrl);
        System.out.println(url);
        Assertions.assertEquals("http://relay1:80/down/12344678/index.m3u8", url);
    }
}
