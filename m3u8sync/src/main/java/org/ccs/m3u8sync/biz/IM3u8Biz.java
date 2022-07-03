package org.ccs.m3u8sync.biz;


import org.ccs.m3u8sync.vo.M3u8FileInfoVo;

import java.util.Date;

/**
 * m3u8文件处理
 *
 * @author chenjh
 */
public interface IM3u8Biz {
    public Date getRoomModifyTime(String roomId);

    public M3u8FileInfoVo getFileInfo(String roomId, String format);

    public M3u8FileInfoVo getM3u8Info(String roomId, String format);

    public boolean existFirstTryTs(String roomId);
}
