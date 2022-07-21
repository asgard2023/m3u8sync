package org.ccs.m3u8sync.downup.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.ccs.m3u8sync.vo.CallbackVo;

import java.io.Serializable;
import java.util.Date;

/**
 * 下载的DownBean对象
 */
@Data
@NoArgsConstructor
@ToString
public class DownBean implements Serializable {
    public DownBean(String roomId, String url, Date initTime, CallbackVo callback) {
        this.roomId = roomId;
        this.url = url;
        this.initTime = initTime;
        this.callback = callback;
    }

    private String roomId;
    private String url;
    private String path;
    private Date initTime;
    private CallbackVo callback;
    private Integer size;
    private Integer downCount;
    private String error;
    private Integer errorCount = 0;
    /**
     * 同步类型(m3u8/file)
     */
    private String syncType;
    private Integer ifRelayCallDel=0;
}
