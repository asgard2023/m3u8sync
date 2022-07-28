package org.ccs.m3u8sync.downup.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常信息
 *
 * @author chenjh
 */
@Data
@NoArgsConstructor
public class DownErrorInfoVo implements Serializable {
    public DownErrorInfoVo(DownBean bean) {
        this.roomId = bean.getRoomId();
        this.url = bean.getUrl();
        this.syncType = bean.getSyncType();
        this.initTime = bean.getInitTime();
        this.errorMap = new HashMap<>();
        this.tsErrorCounterMap = new ConcurrentHashMap<>();
    }

    private String roomId;
    private String syncType;
    private Date initTime;
    private String url;
    /**
     * 试下载次数，异常次数过多时，任务会被取消，可以再试
     */
    private Integer tryDownCount = 0;
    /**
     * ts文件下载成功的个数
     */
    private Integer successTsNums = 0;
    /**
     * ts文件个数或子文件个数
     */
    private Integer totalTsNums = 0;
    /**
     * ts最大运行时间
     */
    private Integer maxDownTime = 0;
    /**
     * ts下载失败的最大运行时间
     */
    private Integer maxFailTime = 0;


    /**
     * 错误信息
     */
    private Map<String, String> errorMap;
    private ConcurrentHashMap<String, Integer> tsErrorCountMap;
    @Transient
    private ConcurrentHashMap<String, AtomicInteger> tsErrorCounterMap;


    @Transient
    private Map<Integer, String> successLineMap;
    @Transient
    private List<String> successLines;
    @Transient
    private List<String> errUrls;
}
