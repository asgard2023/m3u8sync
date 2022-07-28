package org.ccs.m3u8sync.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.client.NginxApiRest;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.constants.SyncType;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.ccs.m3u8sync.downup.service.DownUpService;
import org.ccs.m3u8sync.exceptions.ParamErrorException;
import org.ccs.m3u8sync.exceptions.ParamNullException;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.ccs.m3u8sync.vo.FileListVo;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.*;

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
    @GetMapping("retryTask")
    public ResultData retryTask(@RequestParam(value = "roomId", required = false) String roomId) {
        log.info("开始执行指定房间的重传任务,roomId={}", roomId);
        if (CharSequenceUtil.isBlank(roomId)) {
            throw new ParamNullException("roomId不能为空");
        }
        downUpService.doDownBeanM3u8(roomId);
        return ResultData.success();
    }

    /**
     * 添加m3u8下载任务
     *
     * @param roomId         m3u8房间id
     * @param format         url中roomId组成的格式
     * @param m3u8Url        直接m3u8Url，不用拼接生成
     * @param ifRelayCallDel 用于表示上个接点是中继且希望下载完成后，回调callbackDel接口，以删除上个节点的文件
     * @param callback       回调接口
     * @return
     */
    @PostMapping("addAsync")
    public ResultData addAsync(@RequestParam("roomId") String roomId
            , @RequestParam(value = "format", required = false, defaultValue = "{roomId}/{roomId}.m3u8") String format
            , @RequestParam(value = "m3u8Url", required = false) String m3u8Url
            , @RequestParam(value = "lastM3u8Url", required = false) String lastM3u8Url
            , @RequestParam(value = "ifRelayCallDel", required = false, defaultValue = "0") Integer ifRelayCallDel
            , @RequestBody CallbackVo callback) {

        //用于快束检测中继模模式下一节点是否正常（报告给上一节点）
        if (DownUpService.CHECK_RELAY.equals(roomId) && "test".equals(m3u8Url)) {
            log.info("----add--roomId={} check ok", roomId);
            return ResultData.success("ok");
        }
        if (CharSequenceUtil.isBlank(roomId)) {
            log.warn("----add--roomId={} isBlank", roomId);
            throw new ParamNullException("roomId不能为空");
        }
        if (StringUtils.isNotBlank(format)) {
            format = URLDecoder.decode(format);
        }
        if (StringUtils.isNotBlank(lastM3u8Url)) {
            lastM3u8Url = URLDecoder.decode(lastM3u8Url);
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
        downUpService.checkCallback(DownUpService.CHECK_CALLBACK, callback);
        //中继模式：检查下一节点是否正常
        downUpService.checkRelay(DownUpService.CHECK_RELAY);

        Long length = DownLoadUtil.getRemoteSize(m3u8Url, 3000);
        if (length == null || length == 0) {
            log.warn("----add--roomId={} m3u8Url={} size={} get fail", roomId, m3u8Url, length);
            throw new ParamErrorException(m3u8Url + ":get fail");
        }

        log.info("----add--roomId={} format={} m3u8Url={}", roomId, format, m3u8Url);
        DownBean bean = new DownBean(roomId, m3u8Url, new Date(), callback);
        bean.setSyncType(SyncType.M3U8.getType());
        bean.setIfRelayCallDel(ifRelayCallDel);
        bean.setFormat(format);
        bean.setLastM3u8Url(lastM3u8Url);
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
            , @RequestParam(value = "syncType", required = false) String syncType
            , @RequestBody CallbackVo callback) {

        SyncType syncTypeObj = SyncType.M3U8;
        if (StringUtils.isNotBlank(syncType)) {
            syncTypeObj = SyncType.parse(syncType);
            if (syncTypeObj == null) {
                throw new ParamErrorException("syncType:" + syncType + ",invalid");
            }
        }
        //检查一下回调接口是否正常（如果成功只检查一次）
        downUpService.checkCallback(DownUpService.CHECK_CALLBACK, callback);
        //中继模式：检查下一节点是否正常
        downUpService.checkRelay(DownUpService.CHECK_RELAY);
        Map<String, Object> resultMap = new TreeMap<>();
        List<String> errorInfos = new ArrayList<>();
        if (syncTypeObj == SyncType.M3U8) {
            FileListVo fileListVo = nginxApiRest.getM3u8List(path);
            List<String> roomIdList = fileListVo.getFolders();
            HttpRequest.closeCookie();
            if (StringUtils.isBlank(format)) {
                format = downUpConfig.getFormat();
            }
            List<String> okList = new ArrayList<>();
            for (String roomId : roomIdList) {
                String m3u8Url = downUpConfig.getNginxUrl(roomId, format);
                //如果是m3u8检查一下文件大小，以确定m3u8Url是否有效
                Long length = DownLoadUtil.getRemoteSize(m3u8Url, 3000);
                if (length == null || length == 0) {
                    log.warn("----addNginxList--roomId={} m3u8Url={} size={} get fail", roomId, m3u8Url, length);
                    continue;
                }
                try {
                    DownBean bean = new DownBean(roomId, m3u8Url, new Date(), callback);
                    bean.setSyncType(syncTypeObj.getType());
                    bean.setPath(path);
                    bean.setFormat(format);
                    downUpService.addTask(roomId, bean);
                    okList.add(roomId);
                } catch (Exception e) {
                    log.warn("----addNginxList--roomId={} m3u8Url={} err={}", roomId, m3u8Url, e.getMessage());
                    errorInfos.add(m3u8Url + " " + e.getMessage());
                }
            }
            resultMap.put("roomIdList", roomIdList);
            resultMap.put("sizeRoom", roomIdList.size());
            resultMap.put("sizeOk", okList.size());
            resultMap.put("okList", okList);
            resultMap.put("errorInfos", errorInfos);
        } else {
            addAllNginxFolderForSync(path, callback, syncTypeObj, resultMap, errorInfos);
        }
        return ResultData.success(resultMap);
    }

    /**
     * 按path递归找出所有可同步的文件夹加到任务中
     *
     * @param path
     * @param callback
     * @param syncTypeObj
     * @param resultMap
     * @param errorInfos
     */
    private void addAllNginxFolderForSync(String path, CallbackVo callback, SyncType syncTypeObj, Map<String, Object> resultMap, List<String> errorInfos) {
        List<FileListVo> list = new ArrayList<>();
        //递归查出所有的可同频的目录（可文件夹不管）
        nginxApiRest.getFileListBy(path, list);
        int roomIdCount = 0;
        for (FileListVo fileListVo : list) {
            String roomId = fileListVo.getPath();
            if (fileListVo.getFileCount() > 0) {
                try {
                    DownBean bean = new DownBean(roomId, null, new Date(), callback);
                    bean.setSyncType(syncTypeObj.getType());
                    bean.setPath(fileListVo.getPath());
                    roomIdCount++;
                    downUpService.addTask(roomId, bean);
                } catch (Exception e) {
                    log.warn("----addNginxList--roomId={} m3u8Url={} err={}", roomId, null, e.getMessage());
                    errorInfos.add(roomId + " " + e.getMessage());
                }
            }
        }
        resultMap.put("sizeOk", roomIdCount);
        resultMap.put("errorInfos", errorInfos);
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
        int count = downUpService.recover();
        return ResultData.success(count);
    }

    /**
     * 用于上传成功后回调的接口
     *
     * @param roomId
     * @param fileInfo
     * @return
     */
    @PostMapping("callbackDel/{roomId}")
    public String callbackDel(@PathVariable(value = "roomId") String roomId, @RequestParam(value = "successDel", required = false) String successDel, @RequestBody M3u8FileInfoVo fileInfo) {
        //用于快速验证回调接口
        if (StringUtils.equals("checkCallbackDel", roomId) && StringUtils.equals("test", fileInfo.getFilePath())) {
            log.info("----callbackDel--roomId={} check ok", roomId);
            return "ok";
        }
        log.info("----callbackDel--roomId={} successDel={} fileInfo={}", roomId, successDel, JSONUtil.toJsonStr(fileInfo));
        if ("true".equals(successDel)) {
            downUpService.deleteDown(roomId, fileInfo);
        }
        return "ok";
    }

    /**
     * 用于移除失败的任务，不直接停止当前任务，不会将异常任务加回正常队列
     *
     * @return ResultData
     */
    @GetMapping("remove")
    public ResultData remove(@RequestParam("roomId") String roomId) {
        log.info("----remove={}", roomId);
        downUpService.remove(roomId);
        return ResultData.success();
    }
}
