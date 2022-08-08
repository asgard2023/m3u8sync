package org.ccs.m3u8sync.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import org.ccs.m3u8sync.exceptions.FileNormalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    private FileUtils() {

    }

    public static final Logger logger = LoggerFactory.getLogger(FileUtils.class);


    public static Long getFileLength(File file) throws Exception {
        Long length = 0L;
        if (file.exists() && file.isFile()) {
            try (FileInputStream fis = new FileInputStream(file);
                 FileChannel fileChannel = fis.getChannel();) {
                length = fileChannel.size();
            } catch (IOException e) {
                logger.warn("----getFileLength--error={}", e.getMessage());
                length = 0L;
            }
        }
        return length;
    }

    /**
     * 获取m3u8第一个分片
     *
     * @param m3u8Path
     * @return
     */
    public static String getFirstTs(String m3u8Path) {
        RandomAccessFile m3u8PathFile = null;
        try {
            File file = new File(m3u8Path);
            if (!file.exists()) {
                logger.warn("----getFirstTs--m3u8Path={}", m3u8Path);
                return null;
            }
            m3u8PathFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            logger.warn("----getFirstTs--m3u8Path={} not found", m3u8Path);
            return null;
        }
        try {
            FileUtil.readLines(m3u8PathFile, StandardCharsets.UTF_8, new LineHandler() {
                @Override
                public void handle(String line) {
                    if (line.contains(".ts")) {
                        throw new FileNormalException(line);
                    }
                }
            });
            return null;
        } catch (FileNormalException e) {
            return e.getMessage();
        } finally {
            if (m3u8PathFile != null) {
                try {
                    m3u8PathFile.close();
                } catch (IOException e) {
                    logger.warn("----getFirstTs--m3u8Path={} close error={}", m3u8Path, e.getMessage());
                }
            }
        }
    }
}
