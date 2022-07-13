package org.ccs.m3u8sync;

import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.ccs.m3u8sync.utils.AvClipUtil;
import org.ccs.m3u8sync.vo.M3U8Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

class AvClipUtilTest {
    private static List<M3U8Row> m3U8RowList =null;
    @BeforeAll
    static void init(){
        System.out.println("--init--");
        m3U8RowList = AvClipUtil.readM3U8("http://175.178.252.112:81/m3u8/live/wukong/index.m3u8");
    }
    @Test
    void readM3U8() {
        Assertions.assertTrue(m3U8RowList.size()>0, "m3u8 read");
    }

    @Test
    void getDurationTime(){
        long durationTime = AvClipUtil.getDurationTime(m3U8RowList);
        System.out.println("----durationTime="+durationTime);
        Assertions.assertTrue(durationTime>0, "duration");
    }

    @Test
    void getRemoteSize(){
        Long size=DownLoadUtil.getRemoteSize("http://175.178.252.112:81/m3u8/live/wukong/index.m3u8", 3000);
        Assertions.assertTrue(size>0, "size gt 0");
        System.out.println(size);
        size=DownLoadUtil.getRemoteSize("http://175.178.252.112:81/m3u8/live/12344678/12344678.m3u8", 3000);
        System.out.println(size);
        size=DownLoadUtil.getRemoteSize("http://175.178.252.112:81/m3u8/live/12344678/12344678.m3u8", 3000);
        System.out.println(size);
    }
}
