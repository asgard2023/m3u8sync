package org.ccs.m3u8sync.controller;

import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.ccs.m3u8sync.downup.service.DownErrorService;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 下载服务处理
 *
 * @author chenjh
 */
@RestController
@RequestMapping("/downErrorInfo")
@Slf4j
public class DownErrorInfoController {
    @Autowired
    private DownErrorService downErrorService;

    /**
     * 用于显示任务的异常信息信息
     *
     * @return ResultData
     */
    @GetMapping("downErrorInfo")
    public ResultData getDownErrorInfo(@RequestParam("roomId") String roomId) {
        DownErrorInfoVo downErrorInfo = downErrorService.getDownErrorLocalCache(roomId);
        if (downErrorInfo == null) {
            downErrorInfo = downErrorService.getDownError(roomId);
        }
        return ResultData.success(downErrorInfo);
    }

    /**
     * 所有本机在用处理的任务
     * @return
     */
    @GetMapping("errorInfos")
    public ResultData errorInfos() {
        return ResultData.success(downErrorService.errorInfos());
    }


}
