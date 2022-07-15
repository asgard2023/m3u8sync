package org.ccs.m3u8sync.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileInfoVo implements Serializable {
    private String fileName;
    private String fileTime;
    private String fileSize;
}
