package org.ccs.m3u8sync.demo.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.demo.client.M3u8SyncClient;
import org.ccs.m3u8sync.demo.config.M3u8AsyncDemoConfiguration;
import org.ccs.m3u8sync.demo.vo.CallbackVo;
import org.ccs.m3u8sync.demo.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 下载服务处理
 */
@RestController
@RequestMapping("/m3u8Sync")
@Slf4j
public class M3u8SyncController {
    @Autowired
    private M3u8SyncClient m3u8SyncClient;
    @Autowired
    private M3u8AsyncDemoConfiguration m3u8AsyncDemoConfiguration;

    @PostMapping("callback/{roomId}")
    public String callback(@PathVariable(value = "roomId") String roomId, @RequestBody M3u8FileInfoVo fileInfo) {
        log.info("----callback--roomId={} fileInfo={}", roomId, JSONUtil.toJsonStr(fileInfo));
        return "ok";
    }

    @GetMapping("addSync")
    public String addSync(@RequestParam(value = "roomId") String roomId
            , @RequestParam(value = "format", required = false) String format
            , @RequestParam(value = "m3u8Url", required = false) String m3u8Url) {
        CallbackVo callbackVo = new CallbackVo();
        callbackVo.setBaseUrl(m3u8AsyncDemoConfiguration.getCallbackUrl());
        callbackVo.setParamUrl(m3u8AsyncDemoConfiguration.getCallParamUrl());
        return m3u8SyncClient.addSync(roomId, format, m3u8Url, callbackVo);
    }

    @GetMapping("m3u8Info")
    public Object getM3u8Info(@RequestParam(name = "roomId") String roomId
            , @RequestParam(value = "format", required = false) String format) {
        return m3u8SyncClient.getM3u8Info(roomId, format);
    }
}
