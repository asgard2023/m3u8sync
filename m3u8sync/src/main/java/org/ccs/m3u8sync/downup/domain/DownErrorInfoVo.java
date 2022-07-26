package org.ccs.m3u8sync.downup.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownErrorInfoVo implements Serializable {
    private Date initTime;
    private Integer tryDownCount;
    private Integer successTsNums;
    private Integer totalTsNums;
    private String uploadVodInfo;


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
