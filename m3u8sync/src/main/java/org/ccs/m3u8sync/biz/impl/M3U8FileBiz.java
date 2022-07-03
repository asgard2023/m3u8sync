package org.ccs.m3u8sync.biz.impl;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.biz.IM3u8Biz;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.utils.AvClipUtil;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.utils.FileUtils;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

/**
 * m3u8文件处理
 *
 * @author chenjh
 */
@Service
@Slf4j
public class M3U8FileBiz implements IM3u8Biz {
    @Autowired
    private DownUpConfig downUpConfig;


    @Override
    public Date getRoomModifyTime(String roomId) {
        String roomPath = downUpConfig.getRoomM3u8Path(roomId);
        File file = new File(roomPath);
        if (file.exists()) {
            Long lastModified = file.lastModified();
            return new Date(lastModified);
        }
        return null;
    }


    @Override
    public M3u8FileInfoVo getM3u8Info(String roomId, String format) {
        String roomPath = downUpConfig.getRoomM3u8Path(roomId, format);
        File file = new File(roomPath);
        //如果m3u8不存在，则目录中查找第一个m3u8
//        if (!file.exists()) {
//            file = findFirstM3u8(file);
//        }
        if (!file.exists()) {
            log.warn("-----getM3u8Info--roomPath={} unexist", roomPath);
            M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
            fileInfoVo.setFilePath(roomPath);
            fileInfoVo.setFileLength(0L);
            fileInfoVo.setFileCount(0);
            fileInfoVo.setDurationTime(0L);
            return fileInfoVo;
        }

        return AvClipUtil.getM3u8FileInfoAll(file, null);
    }

    private File findFirstM3u8(File file) {
        file = file.getParentFile();
        File[] files = file.listFiles();
        for (File f : files) {
            if (!(f.getName().endsWith(".m3u8"))) {
               file =f;
               break;
            }
        }
        return file;
    }

    @Override
    public M3u8FileInfoVo getFileInfo(String roomId, String format) {
        String roomPath = downUpConfig.getRoomIdFilePath(roomId);
        File file = new File(roomPath);
        File[] files = file.listFiles();
        int count = 0;
        Long length = 0L;
        if (files != null) {
            for (File f : files) {
                if (!(f.getName().startsWith(".") || f.getName().endsWith(".tmp"))) {
                    count++;
                    try {
                        length += FileUtils.getFileLength(f);
                    } catch (Exception e) {
                        log.warn("----getFileInfo--name={} error={}", f.getPath(), e.getMessage());
                    }
                }
            }
        } else {
            log.warn("-----getFileInfo--roomPath={} is null", roomPath);
        }
        M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
        fileInfoVo.setFilePath(file.getPath());
        fileInfoVo.setFileCount(count);
        fileInfoVo.setFileLength(length);
        return fileInfoVo;
    }


    @Override
    public boolean existFirstTryTs(String roomId) {
        String roomPath = downUpConfig.getRoomM3u8Path(roomId);
        String firstTs = FileUtils.getFirstTs(roomPath);
        String firstTsPath = CommUtils.appendUrl(roomPath, firstTs);
        return FileUtil.exist(firstTsPath);
    }

}
