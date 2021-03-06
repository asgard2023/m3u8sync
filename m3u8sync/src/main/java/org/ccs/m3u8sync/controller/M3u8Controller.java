package org.ccs.m3u8sync.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.biz.IM3u8Biz;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * m3u8信息查看
 *
 * @author chenjh
 */
@RestController
@RequestMapping("/m3u8")
@Slf4j
public class M3u8Controller {
    @Autowired
    private IM3u8Biz m3u8Biz;
    @Autowired
    private DownUpConfig downUpConfig;

    @GetMapping("m3u8Info")
    public M3u8FileInfoVo getM3u8Info(@RequestParam("roomId") String roomId,  @RequestParam(value = "format", required = false) String format) {
        log.info("----getM3u8Info--roomId={}", roomId);
        if(StringUtils.isBlank(format)){
            format=downUpConfig.getFormat();
        }
        return this.m3u8Biz.getM3u8Info(roomId, format);
    }

    @GetMapping("fileInfo")
    public M3u8FileInfoVo getFileInfo(@RequestParam("roomId") String roomId,  @RequestParam(value = "format", required = false) String format) {
        log.info("----getFileInfo--roomId={}", roomId);
        if(StringUtils.isBlank(format)){
            format=downUpConfig.getFormat();
        }
        return this.m3u8Biz.getFileInfo(roomId, format);
    }
}
