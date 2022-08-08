package org.ccs.m3u8sync.utils;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.exceptions.BaseException;
import org.ccs.m3u8sync.exceptions.FailedException;
import org.ccs.m3u8sync.vo.M3U8Row;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AvClipUtil {
    private AvClipUtil() {

    }

    static Logger logger = LoggerFactory.getLogger(AvClipUtil.class);
    public static final String M3U8_ENDPOINT = "#EXT-X-ENDLIST";

    /**
     * 读取url的内容
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static List<M3U8Row> readM3U8(String url) {
        Long time = System.currentTimeMillis();
        List<String> contents = getContents(url);
        if (CollectionUtils.isEmpty(contents)) {
            contents = getContents(url);//如果第一次失败，再调一次
            logger.warn("----readM3U8--second--url={} time={}", url, System.currentTimeMillis() - time);
        }
        if (CollectionUtils.isEmpty(contents)) {
            throw new FailedException("M3U8文件格式不正确，编辑失败！");
        }
        return toM3U8Rows(contents);
    }

    private static List<String> getContents(String url) {
        List<String> contents = new ArrayList<>();
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            URL urlRead = new URL(url);
            isr = new InputStreamReader(urlRead.openConnection().getInputStream());
            br = new BufferedReader(isr);
            String realLineStr = null;
            while ((realLineStr = br.readLine()) != null) {
                contents.add(realLineStr);
            }
        } catch (Exception e) {
            logger.error("----readM3U8--url={} error={}", url, e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error("----readM3U8--url={} error={}", url, e.getMessage());
            }
            try {
                if (isr != null) {
                    isr.close();
                }
            } catch (IOException e) {
                logger.error("----readM3U8--url={} error={}", url, e.getMessage());
            }
        }
        return contents;
    }

    /**
     * 构造我们熟悉的m3u8结构方便判断操作
     *
     * @param contents
     * @return
     */
    private static List<M3U8Row> toM3U8Rows(List<String> contents) {
        List<M3U8Row> list = new ArrayList<>();
        int i = 0;
        BigDecimal timeNode = new BigDecimal(0).setScale(3, RoundingMode.HALF_UP);
        for (String content : contents) {
            M3U8Row row = new M3U8Row();
            row.setRowIndex(i);
            row.setContent(content);
            if (content.indexOf("#EXTINF") != -1) {
                row.setExtinf(true);
                Double tsTime = Double.parseDouble(content.replaceFirst("#EXTINF:", "").replace(", no desc", "").replaceFirst(",", ""));
                row.setTsTimes(toMillisec(tsTime));
                row.setStartTimeNode(toMillisec(timeNode.doubleValue()));
                row.setEndTimeNode(toMillisec(timeNode.add(BigDecimal.valueOf(tsTime).setScale(3, RoundingMode.HALF_UP)).doubleValue()));
                timeNode = timeNode.add(BigDecimal.valueOf(tsTime).setScale(3, RoundingMode.HALF_UP));
            } else {
                row.setExtinf(false);
            }
            list.add(row);
            i++;
        }
        return list;
    }

    private static long toMillisec(double sec) {
        return (long) (sec * 1000);
    }


    public static long getDurationTime(List<M3U8Row> m3u8Rows) {
        long durationTime = 0L;
        for (M3U8Row m3u8Row : m3u8Rows) {
            if (m3u8Row.isExtinf()) {
                durationTime += m3u8Row.getTsTimes();
            }
        }
        return durationTime;
    }

    /**
     * 返回时长(s)
     *
     * @param file
     * @return
     */
    public static long getDurationTime(File file) {
        List<String> contents = null;
        try {
            contents = FileUtil.readLines(file, StandardCharsets.UTF_8);
            if (CollectionUtils.isEmpty(contents)) {
                return 0L;
            }
        } catch (Exception e) {
            logger.warn("----getDurationTime--path={}", file.getAbsolutePath(), e);
            return 0L;
        }
        List<M3U8Row> m3u8Rows = toM3U8Rows(contents);
        return getDurationTime(m3u8Rows) / 1000;
    }

    public static M3u8FileInfoVo getFileInfo(File file, M3u8FileInfoVo data) throws BaseException {
        M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
        String info = null;
        String path = null;
        try {
            path = file.getPath();
            info = "----getFileInfo path=" + path;
            if (file.isFile()) {
                fileInfoVo.setFilePath(file.getAbsolutePath());
                file = file.getParentFile();
                File[] files = file.listFiles();
                //排除.tmp文件,.开头的文件
                List<File> fileList = Arrays.stream(files).filter(f -> !(f.getName().endsWith(".tmp") || f.getName().startsWith("."))).collect(Collectors.toList());
                Integer fileCount = fileList.size();
                fileInfoVo.setFileCount(fileCount);
                if (data != null && !StringUtils.equals("" + data.getFileCount(), "" + fileCount)) {
                    info += ",fileCount:" + fileCount + "/" + data.getFileCount();
                    logger.warn(info);
                    throw new FailedException(info);
                }

                Long length = 0L;
                for (File f : fileList) {
                    length += FileUtils.getFileLength(f);
                }
                fileInfoVo.setFileLength(length);
                if (data != null && !StringUtils.equals("" + data.getFileLength(), "" + length)) {
                    info += ",fileLength:" + length + "/" + data.getFileLength();
                    logger.warn(info);
                    throw new FailedException(info);
                }
            } else {
                info += " not file";
                logger.warn(info);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            logger.error("----getFileInfo path={}", path, e);
            throw new FailedException("getFileInfo:" + e.getMessage());
        }
        return fileInfoVo;
    }


    public static M3u8FileInfoVo getM3u8FileInfoAll(File file, M3u8FileInfoVo data) {
        M3u8FileInfoVo fileInfoVo = getFileInfo(file, data);
        List<String> contents = null;
        try {
            contents = FileUtil.readLines(file, StandardCharsets.UTF_8);
            if (CollectionUtils.isEmpty(contents)) {
                return null;
            }
        } catch (Exception e) {
            logger.warn("----getDurationTime--path={}", file.getAbsolutePath(), e);
            throw new FailedException("readLine fail:" + e.getMessage());
        }
        List<M3U8Row> m3u8Rows = toM3U8Rows(contents);
        fileInfoVo.setDurationTime(getDurationTime(m3u8Rows));
        if (data != null && !StringUtils.equals("" + data.getDurationTime(), "" + fileInfoVo.getDurationTime())) {
            String info = "durationTime:" + fileInfoVo.getDurationTime() + "/" + data.getDurationTime();
            logger.warn("----getM3u8FileInfo {}", info);
            throw new FailedException(info);
        }
        return fileInfoVo;
    }

    private static long getRowsTsFileLength(List<M3U8Row> m3u8Rows, String path, File[] files) {
        long fileLength = 0L;
        try {
            for (M3U8Row row : m3u8Rows) {
                if (row.getContent().endsWith(".ts")) {
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        if (f.getPath().contains(row.getContent())) {
                            fileLength += FileUtils.getFileLength(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("----getRowsTsFileLength path={} error={}", path, e.getMessage());
        }
        return fileLength;
    }


    /**
     * @param m3u8Rows
     * @param endMillisec
     * @param isStart     true开头，false结束
     * @return
     */
    private static List<String> getM3u8Content(List<M3U8Row> m3u8Rows, long endMillisec, boolean isStart) {
        List<String> resultContents = new ArrayList<>();

        boolean isEnd = !isStart;
        if (isEnd) {
            Collections.reverse(m3u8Rows);
        }

        long timeMillisec = 0;

        int rowIndex = 0;
        int count = 0;
        boolean isBreak = false;
        for (M3U8Row m3u8Row : m3u8Rows) {
            resultContents.add(m3u8Row.getContent());
            count++;
            if (rowIndex != 0 && count < rowIndex + 3) {
                isBreak = true;
                break;
            }
            if (m3u8Row.isExtinf()) {
                timeMillisec += m3u8Row.getTsTimes();
                if (timeMillisec > endMillisec) {
                    rowIndex = m3u8Row.getRowIndex();
                }
            }

        }

        if (isEnd) {
            if (isBreak) {
                if (count < m3u8Rows.size()) {
                    //多补一行，把#EXTINF也加进来
                    resultContents.add(m3u8Rows.get(count).getContent());
                }
                //反序加入m3u8开头报文信息，以便于翻转后变成正常的
                resultContents.add("#EXT-X-DISCONTINUITY");
                resultContents.add("#EXT-X-TARGETDURATION:8");
                resultContents.add("#EXT-X-MEDIA-SEQUENCE:0");
                resultContents.add("#EXT-X-VERSION:3");
                resultContents.add("#EXTM3U");
            }
            Collections.reverse(resultContents);
        }
        String lastLine = resultContents.get(resultContents.size() - 1);
        if (!M3U8_ENDPOINT.equals(lastLine)) {
            resultContents.add(M3U8_ENDPOINT);
        }
        return resultContents;
    }


    public static String getShortFileName(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf("."));
        String path = fileName.substring(0, fileName.lastIndexOf("/"));
        String fName = fileName.substring(fileName.lastIndexOf("/"));
        if (fName.indexOf(".") > 0) {
            fName = fName.substring(0, fName.lastIndexOf("."));
        }
        fileName = path + CommUtils.getStringLimit(fName, 25) + ext;
        return fileName;
    }
}
