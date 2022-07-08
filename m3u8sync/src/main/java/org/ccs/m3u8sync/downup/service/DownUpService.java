package org.ccs.m3u8sync.downup.service;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ccs.m3u8sync.config.CallbackConfiguration;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.ccs.m3u8sync.downup.down.DownResult;
import org.ccs.m3u8sync.downup.queue.DownQueue;
import org.ccs.m3u8sync.exceptions.FailedException;
import org.ccs.m3u8sync.exceptions.FileUnexistException;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 下载上传服务
 */
@Service
@Slf4j
public class DownUpService {
    @Autowired
    private DownQueue queue;
    @Autowired
    private DownUpConfig downUpConfig;
    @Autowired
    private CallbackConfiguration callbackConfiguration;
    //下载成功数
    private static AtomicInteger downSuccessCounter = new AtomicInteger();
    //下载失败数
    private static AtomicInteger downFailCounter = new AtomicInteger();
    //回调失败数
    private static AtomicInteger callbackFailCounter = new AtomicInteger();
    private static Map<String, DownBean> errorMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (checkDownupNotOpen()) {
            return;
        }
        log.info("目前迁移任务数量{}个,异常队列数量{}个", queue.size(), queue.errSize());
        //自动恢复失败的任务
        if (queue.errSize() > 0) {
            queue.moveErr();
        }
        //检测queue结构中的hash和pop的数量是否一致,不一致就进行从hash中重新灌入到pop中
        queue.syncBreak();
        DownLoadUtil.setTaskTheadCount(downUpConfig.getTaskThreadCount(), downUpConfig.getThreadMax());
        doTask();
    }


    /**
     * 每小时自动把失败的任务加回队珍，以便于重新执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduleTask() {
        if (checkDownupNotOpen()) {
            return;
        }
        log.info("启动定时任务,目前迁移任务数量{}个,异常队列数量{}个", queue.size(), queue.errSize());
        //自动恢复失败的任务
        if (queue.errSize() > 0) {
            queue.moveErr();
        }
    }

    private boolean checkDownupNotOpen() {
        if (!downUpConfig.isOpen()) {
            log.warn("未开启下载上传服务,请检查配置");
            return true;
        }
        return false;
    }

    public synchronized void addTask(String roomId, DownBean bean) {
        if (checkDownupNotOpen()) {
            return;
        }
        DownBean oldBean = queue.get(roomId);
        if (oldBean != null) {
            //检测存入任务的时间,按照小时计
            Date initTime = oldBean.getInitTime();
            Date now = new Date();
            long interval = DateUtil.between(initTime, now, DateUnit.HOUR, false);
            int timeInterval = downUpConfig.getTimeInterval();
            if (interval <= timeInterval) {
                log.warn("roomId={},首次加入下载队列的时间为{},未超过配置的时间间隔{}h,本次提交被忽略", roomId, initTime, timeInterval);
                throw new FailedException("首次加入下载队列的时间为"+initTime+",未超过配置的时间间隔"+timeInterval+"h,本次提交被忽略");
            }
        }
        //单台使用synchronized来防止并发,集群需要变更为redis锁roomId防止并发写入队列
        queue.put(bean);
    }


    /**
     * 下载任务处理
     */
    private void doTask() {
        Integer taskNum = downUpConfig.getTaskNum();
        log.info("----doTask--taskNum={}", taskNum);
        for (int i = 0; i < taskNum; i++) {
            ThreadUtil.execute(() -> {
                //线程优先休眠,让容器启动完毕
                ThreadUtil.sleep(5000);
                while (true) {
                    //阻塞式拉取下载对象
                    DownBean bean = queue.getOne();
                    if (null == bean) {
                        ThreadUtil.sleep(10 * 1000L);
                        continue;
                    }
                    Long curTime = System.currentTimeMillis();
                    Long timeHour = (curTime - bean.getInitTime().getTime()) / 3600000;
                    String roomId = bean.getRoomId();
                    //如果任务超过过期时间，则删除任务，不执行
                    if (timeHour > downUpConfig.getExpireHour()) {
                        queue.delete(roomId);
                        log.warn("----doTask--roomId={} expireHour={} expired, remove task", roomId, timeHour);
                        continue;
                    }
                    doOneBean(bean);
                    log.info("---doTask--roomID={}的下载任务已结束", bean.getRoomId());
                }
            });
        }
    }

    /**
     * 恢复的做法是, 将异常队列的任务,塞在正常队列的尾部, 以使其与正常产生任务交叉执行.
     * 正常业务依然享有优先执行权
     */
    public void recover() {
        if (checkDownupNotOpen()) {
            return;
        }
        long errSize = queue.errSize();
        log.info("开始迁移异常下载队列,队列总容量为{}", errSize);
        queue.moveErr();
    }

    public void doOneBean(String roomId) {
        if (checkDownupNotOpen()) {
            return;
        }
        DownBean bean = queue.get(roomId);
        if (null == bean) {
            log.warn("任务队列中未查询到指定roomId={}的资源", roomId);
            return;
        }
        doOneBean(bean);
    }

    private void doOneBean(DownBean bean) {
        log.info("从队列获取一个下载任务{} {},队列剩余个数{},异常数量堆积{}个", bean.getRoomId(), bean.getUrl(), queue.size(), queue.errSize());
        //先下载原画,然后上传原画成功后,再下载试看的M3u8,再上传试看的资源到阿里
        String roomId = bean.getRoomId();
        String url = bean.getUrl();
        if(StringUtils.isBlank(url)||"null".equals(url)){
            queue.delete(roomId);
            log.warn("----doOneBean--roomId={} url={} is null remove task", roomId, url);
            return;
        }
        String m3u8Path = downUpConfig.getRoomIdFilePath(roomId);
        String fileName = url.substring(url.lastIndexOf("/"));
        m3u8Path = CommUtils.appendUrl(m3u8Path, fileName);
        File destFile = FileUtil.file(m3u8Path);
        //不重新设置M3u8,保持与原文件一致
        DownResult result = null;
        try {
            result = DownLoadUtil.downloadM3u8(url, destFile, false);
        } catch (FileUnexistException e) {
            downFailCounter.incrementAndGet();
            log.error("----doOneBean--roomId={} m3u8Path={} not found，直接从任务移除，需要调用add重新加入方可恢复", roomId, m3u8Path);
            queue.delete(roomId);
            queue.putErr(roomId);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downErr:" + e.getMessage() + "," + url);
            errorMap.put(roomId, bean);
            return;
        }
        if (null == result) {
            log.error("roomId={}下载失败,url={}", roomId, url);
            if (notExistRemote(url)) {
                log.error("roomId={}下载失败,直接从任务移除,需调用add重新加入方可恢复,url={}", roomId, url);
                queue.delete(roomId);
                return;
            }
            queue.putErr(roomId);
            return;
        }
        Integer successTsNums = result.getTss().size();
        Integer totalTsNums = result.getTsNums();
        bean.setSize(totalTsNums);
        bean.setDownCount(successTsNums);
        if (!totalTsNums.equals(successTsNums)) {
            downFailCounter.incrementAndGet();
            log.error("roomId={}下载不完全,ts总数{},下载成功数{},m3u8Path={}", roomId, totalTsNums, successTsNums, m3u8Path);
            queue.putErr(roomId);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downFail");
            errorMap.put(roomId, bean);
            return;
        }

        DownBean downBean = queue.get(roomId);
        boolean isSuccess = false;
        if (downBean != null) {
            M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
            fileInfoVo.setFilePath(m3u8Path);
            fileInfoVo.setFileCount(successTsNums);
            isSuccess = callbackOnSuccess(downBean, fileInfoVo);
        }
        if (isSuccess) {
            downSuccessCounter.incrementAndGet();
            errorMap.remove(roomId);
            queue.delete(roomId);
            Long curTime = System.currentTimeMillis();
            log.info("----doOneBean--finished--roomId={}下载完成，下载数:{}，用时:{}s", roomId, successTsNums, (curTime - bean.getInitTime().getTime()) / 1000);
        }
    }

    /**
     * 同步下载完成后回调接口
     *
     * @param downBean
     * @param fileInfoVo
     */
    private boolean callbackOnSuccess(DownBean downBean, M3u8FileInfoVo fileInfoVo) {
        boolean open = callbackConfiguration.isOpen();
        String roomId = downBean.getRoomId();
        CallbackVo callback = downBean.getCallback();
        //如果不用回调，则直接通过
        if (!open) {
            return true;
        }
        boolean isSuccess = false;
        if (callback == null) {
            log.warn("----callbackOnSuccess--roomId={} callback is null", roomId);
            downBean.setError("callback is null");
            return isSuccess;
        }

        //优先用户传的baseUrl
        String baseUrl = (String) CommUtils.nvl(callback.getBaseUrl(), callbackConfiguration.getBaseUrl());
        String paramUrl = (String) CommUtils.nvl(callback.getParamUrl(), callbackConfiguration.getParamUrl());
        if (StringUtils.isBlank(baseUrl)) {
            log.warn("----callbackOnSuccess--roomId={} callback.baseUrl is null", roomId, callback.getBaseUrl());
            downBean.setError("baseUrl is null");
            return isSuccess;
        }
        callback.setBaseUrl(baseUrl);
        String callbackUrl = CommUtils.appendUrl(baseUrl, paramUrl);
        try {
            callbackUrl = callbackUrl.replace("{roomId}", roomId);
            String body = JSONUtil.toJsonStr(fileInfoVo);
            log.info("----callbackOnSuccess--roomId={} callbackUrl={} req.body={}", roomId, callbackUrl, body);
            HttpResponse response = HttpUtil.createPost(callbackUrl).body(body).timeout(10000).execute();
            log.info("----callbackOnSuccess--roomId={} resp.body={}", roomId, response.body());
            if ("ok".equals(response.body())) {
                isSuccess = true;
                downBean.setError(null);
            }
        } catch (HttpException e) {
            callbackFailCounter.incrementAndGet();
            downBean.setError("callbackErr:" + e.getMessage());
            log.error("----callbackOnSuccess--roomId={} callbackUrl={}", roomId, callbackUrl, e);
        }
        return isSuccess;
    }

    private boolean notExistRemote(String url) {
        HttpResponse response = HttpRequest.head(url).setConnectionTimeout(5000).setReadTimeout(5000).execute();
        int status = response.getStatus();
        return 404 == status;
    }

    public Map<String, Object> status(String type) {
        Map<String, Object> treeMap = new TreeMap<>();
        if (StringUtils.isBlank(type) || "help".equals(type)) {
            treeMap.put("usage", "type=help,all,config,errors,queue");
            return treeMap;
        }

        boolean isAll = "all".equals(type);
        if (isAll || "queue".equals(type)) {
            treeMap.put("queueSize", queue.size());
            treeMap.put("errorSize", queue.errSize());
        }
        if (isAll || "error".equals(type)) {
            treeMap.put("errors", queue.errors());
            List<DownBean> list = errorMap.values().stream().sorted(Comparator.comparing(DownBean::getInitTime)).collect(Collectors.toList());
            treeMap.put("errorList", list);
        }

        if (isAll || "count".equals(type)) {
            treeMap.put("downSuccessCount", downSuccessCounter.get());
            treeMap.put("downFailCount", downFailCounter.get());
            treeMap.put("callbackFailCount", downFailCounter.get());
        }

        if (isAll || "config".equals(type)) {
            treeMap.put("downUpConfig", downUpConfig);
            treeMap.put("callbackConfig", callbackConfiguration);
        }
        return treeMap;
    }
}
