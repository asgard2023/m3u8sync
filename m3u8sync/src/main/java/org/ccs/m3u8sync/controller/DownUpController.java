package org.ccs.m3u8sync.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.client.NginxApiRest;
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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @Autowired
    private NginxApiRest nginxApiRest;

    /**
     * 重新执行指定房间任务 上传原画对应的试看,并更新CMS平台
     * 示例:
     *
     * @param roomId m3u8房间id
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

    /**
     * 添加m3u8下载任务
     *
     * @param roomId   m3u8房间id
     * @param format   url中roomId组成的格式
     * @param m3u8Url  直接m3u8Url，不用拼接生成
     * @param callback 回调接口
     * @return
     */
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
        } else {
            if (m3u8Url.startsWith("http%3A%2F%2F") || m3u8Url.startsWith("https%3A%2F%2F")) {
                m3u8Url = URLDecoder.decode(m3u8Url);
            }
        }

        //检查一下回调接口是否正常（如果成功只检查一次）
        downUpService.checkCallback("checkCallback", callback);

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

    /**
     * nginx开启文件列表显示功能，此接口读取目录列表中的所有m3u8进行处理
     *
     * @param format   url中roomId组成的格式
     * @param callback 回调接口相关
     * @return
     * @author chenjh
     */
    @PostMapping("addNginxList")
    public ResultData addNginxList(@RequestParam(value = "format", required = false, defaultValue = "{roomId}/{roomId}.m3u8") String format
            , @RequestParam(value = "path", required = false) String path
            , @RequestBody CallbackVo callback) {
        //检查一下回调接口是否正常（如果成功只检查一次）
        downUpService.checkCallback("checkCallback", callback);

        List<String> roomIdList = nginxApiRest.getM3u8List(path);
        HttpRequest.closeCookie();
        if (StringUtils.isBlank(format)) {
            format = downUpConfig.getFormat();
        }
        List<String> okList = new ArrayList<>();
        for (String roomId : roomIdList) {
            String m3u8Url = downUpConfig.getNginxUrl(roomId, format);
            Long length = DownLoadUtil.getRemoteSize(m3u8Url, 3000);
            if (length == null || length == 0) {
                log.warn("----add--roomId={} m3u8Url={} size={} get fail", roomId, m3u8Url, length);
                continue;
            }
            try {
                DownBean bean = new DownBean(roomId, m3u8Url, roomId, new Date(), callback, 0, 0, null, 0);
                downUpService.addTask(roomId, bean);
                okList.add(roomId);
            } catch (Exception e) {
                log.warn("----add--roomId={} m3u8Url={} err={}", roomId, m3u8Url, e.getMessage());
            }
        }
        return ResultData.success(okList);
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
