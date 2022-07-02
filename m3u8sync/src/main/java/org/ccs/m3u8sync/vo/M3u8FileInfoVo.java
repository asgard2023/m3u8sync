package org.ccs.m3u8sync.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class M3u8FileInfoVo implements Serializable {
    private String filePath;
    private Long durationTime;
    private Long fileLength=0L;
    private Integer fileCount=0;
}
