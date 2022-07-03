package org.ccs.m3u8sync.downup.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.ccs.m3u8sync.vo.CallbackVo;

import java.io.Serializable;
import java.util.Date;

/**
 * 下载的DownBean对象
 */
@Data
@AllArgsConstructor
@Builder
@ToString
public class DownBean implements Serializable {
    private String roomId;
    private String url;
    private String title;
    private Date initTime;
    private CallbackVo callback;
    private Integer size;
    private Integer downCount;
    private String error;
    private Integer errorCount=0;
}
