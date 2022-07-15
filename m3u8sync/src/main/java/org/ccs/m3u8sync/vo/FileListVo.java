package org.ccs.m3u8sync.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FileListVo implements Serializable {
    private String path;
    private List<String> folders;
    private List<FileInfoVo> files;
    private int fileCount;
}
