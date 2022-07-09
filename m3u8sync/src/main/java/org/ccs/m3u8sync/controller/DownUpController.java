package org.ccs.m3u8sync.controller;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.ccs.m3u8sync.downup.service.DownUpService;
import org.ccs.m3u8sync.exceptions.ParamErrorException;
import org.ccs.m3u8sync.exceptions.ParamNullException;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 下载服务处理
 */
@RestController
@RequestMapping("/downup")
@Slf4j
public class DownUpController {
    @Autowired
    private DownUpService downUpService;
    @Autowired
    private DownUpConfig downUpConfig;

    /**
     * 重新执行指定房间任务 上传原画对应的试看,并更新CMS平台
     * 示例:
     *
     * @param roomId
     * @return ResultData
     */
    @GetMapping("one")
    public ResultData one(@RequestParam("roomId") String roomId) {
        log.info("开始执行指定房间的重传任务,roomId={}", roomId);
        if (CharSequenceUtil.isBlank(roomId)) {
            throw new ParamNullException("roomId不能为空");
        }
        downUpService.doOneBean(roomId);
        return ResultData.success();
    }

    @PostMapping("add")
    public ResultData add(@RequestParam("roomId") String roomId
            , @RequestParam(value = "format", required = false, defaultValue = "{roomId}/{roomId}.m3u8") String format
            , @RequestParam(value = "m3u8Url", required = false) String m3u8Url
            , @RequestBody CallbackVo callback) {

        if (CharSequenceUtil.isBlank(roomId)) {
            log.warn("----add--roomId={} isBlank", roomId);
            throw new ParamNullException("roomId不能为空");
        }
        if (StringUtils.isBlank(m3u8Url)) {
            m3u8Url = downUpConfig.getNginxUrl(roomId, format);
        }
        if (StringUtils.isBlank(m3u8Url) || "null".equals(m3u8Url)) {
            log.warn("----add--roomId={} m3u8Url={} invalid", roomId, m3u8Url);
            throw new ParamNullException("m3u8Url不能为空");
        }


        Long length = DownLoadUtil.getRemoteSize(m3u8Url, 3000);
        if (length == null || length == 0) {
            log.warn("----add--roomId={} m3u8Url={} size={} get fail", roomId, m3u8Url, length);
            throw new ParamErrorException(m3u8Url + ":get fail");
        }

        log.info("----add--roomId={} format={} m3u8Url={}", roomId, format, m3u8Url);
        DownBean bean = new DownBean(roomId, m3u8Url, roomId, new Date(), callback, 0, 0, null, 0);
        downUpService.addTask(roomId, bean);
        return ResultData.success();
    }

    @GetMapping("status")
    public ResultData status(@RequestParam(value = "type", required = false) String type) {
        return ResultData.success(downUpService.status(type));
    }

    /**
     * 执行所有失败的任务,会从失败队列中迁移到正常队列的执行头部
     *
     * @return ResultData
     */
    @GetMapping("recover")
    public ResultData recover() {
        downUpService.recover();
        return ResultData.success();
    }

}
