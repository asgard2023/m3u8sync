package org.ccs.m3u8sync.downup.down;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.List;

/**
 * 下载结果对象
 */
@Data
@AllArgsConstructor
public class DownResult {
    File m3u8File;
    Integer tsNums;
    List<String> tss;
}
