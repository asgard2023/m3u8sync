package org.ccs.m3u8sync.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.exceptions.FailedException;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.vo.FileInfoVo;
import org.ccs.m3u8sync.vo.FileListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取nginx的接口
 *
 * @author chenjh
 */
@Service
@Slf4j
public class NginxApiRest {
    @Autowired
    private DownUpConfig downUpConfig;
    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders initPostHeader() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("source", "m3u8sync");
        return requestHeaders;
    }

    public FileListVo getM3u8List(String path) {
        String nginxUrl = downUpConfig.getNginxUrl();
        if(path!=null && !path.endsWith("/")){
            path+="/";
        }
        if (StringUtils.isNotBlank(path)) {
            nginxUrl = CommUtils.appendUrl(nginxUrl, path);
        }
        HttpHeaders requestHeaders = initPostHeader();
        Map<String, String> paramMap = new HashMap<>(8);
        HttpEntity<Object> requestEntity = new HttpEntity<>(paramMap, requestHeaders);

        try {
//            ResponseEntity<String> exchange = restTemplate.exchange(nginxUrl, HttpMethod.GET, requestEntity, String.class);
            ResponseEntity<String> exchange = restTemplate.getForEntity(nginxUrl, String.class);
            log.debug("----getM3u8List--path={} nginxUrl={} statusCode={}", path, nginxUrl, exchange.getStatusCode());
            return readNginxFileInfo(path, exchange.getBody());
        } catch (Exception e) {
            log.error("---getM3u8List--nginxUrl={} error={}", nginxUrl, e.getMessage(), e);
            throw new FailedException(nginxUrl + " " + e.getMessage());
        }
    }

    public FileListVo getFileListBy(String path, List<FileListVo> list) {
        FileListVo fileListVo = getM3u8List(path);
        List<String> folders = fileListVo.getFolders();
        for (String folder : folders) {
            if (StringUtils.isBlank(folder)) {
                continue;
            }
            String folderPath = folder;
            if (path != null) {
                folderPath = CommUtils.appendUrl(path, folder);
            }
            if(list!=null) {
                list.add(getFileListBy(folderPath, list));
            }
        }
        return fileListVo;
    }

    private static final int NGINX_FILE_TIME_LENGTH="03-Jul-2022 08:55".length();

    private FileListVo readNginxFileInfo(String path, String nginxBody) {
        FileListVo fileListVo = new FileListVo();
        fileListVo.setPath(path);
        List<String> folders = new ArrayList<>();
        List<FileInfoVo> files = new ArrayList<>();
        String[] lines = nginxBody.split("\n");

        String hrefStart="<a href=\"";
        int hrefStartLenth=hrefStart.length();

        for (String line : lines) {
            line = line.trim();
            boolean isFolder = line.endsWith("-");
            int idx = line.indexOf(hrefStart);
            if (idx >= 0) {
                if (line.contains("\"../\"")) {
                    continue;
                } else if (isFolder) {
                    String linkInfo = line.substring(idx + hrefStartLenth);
                    String fileName = linkInfo.substring(0, linkInfo.indexOf("/\">"));
                    folders.add(fileName);
                } else {
                    String linkInfo = line.substring(idx + hrefStartLenth);
                    String fileName = linkInfo.substring(0, linkInfo.indexOf("\">"));
                    FileInfoVo file = new FileInfoVo();
                    file.setFileName(fileName);
                    idx = line.indexOf(fileName + "</a>") + (fileName + "</a>").length();
                    String lineEndLink = line.substring(idx);
                    lineEndLink = lineEndLink.trim();
                    file.setFileTime(lineEndLink.substring(0, NGINX_FILE_TIME_LENGTH));
                    file.setFileSize(lineEndLink.substring(NGINX_FILE_TIME_LENGTH).trim());
                    files.add(file);
                }
            }
        }
        fileListVo.setFiles(files);
        fileListVo.setFileCount(files.size());
        fileListVo.setFolders(folders);
        return fileListVo;
    }
}
