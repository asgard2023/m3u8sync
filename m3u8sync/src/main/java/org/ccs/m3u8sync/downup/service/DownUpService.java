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
import org.ccs.m3u8sync.client.NextM3u8SyncRest;
import org.ccs.m3u8sync.client.NginxApiRest;
import org.ccs.m3u8sync.config.CallbackConfiguration;
import org.ccs.m3u8sync.config.DownUpConfig;
import org.ccs.m3u8sync.config.M3u8SyncConfiguration;
import org.ccs.m3u8sync.config.RelayConfiguration;
import org.ccs.m3u8sync.constants.SyncType;
import org.ccs.m3u8sync.downup.domain.DownBean;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.ccs.m3u8sync.downup.down.DownLoadUtil;
import org.ccs.m3u8sync.downup.down.DownResult;
import org.ccs.m3u8sync.downup.queue.DownQueue;
import org.ccs.m3u8sync.exceptions.FailedException;
import org.ccs.m3u8sync.exceptions.FileUnexistException;
import org.ccs.m3u8sync.exceptions.M3u8GlobalExceptionHandler;
import org.ccs.m3u8sync.exceptions.ResultData;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.vo.CallbackVo;
import org.ccs.m3u8sync.vo.FileListVo;
import org.ccs.m3u8sync.vo.M3u8FileInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下载上传服务
 */
@Service
@Slf4j
public class DownUpService {
    @Autowired
    private DownQueue queue;
    @Autowired
    private DownErrorService downErrorService;
    @Autowired
    private DownUpConfig downUpConfig;
    @Autowired
    private NginxApiRest nginxApiRest;
    @Autowired
    private CallbackConfiguration callbackConfiguration;
    @Autowired
    private M3u8SyncConfiguration m3u8SyncConfiguration;
    @Autowired
    private RelayConfiguration relayConfiguration;
    @Autowired
    private NextM3u8SyncRest nextM3u8SyncRest;
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
        M3u8GlobalExceptionHandler.setLogExceptionTypeBase(m3u8SyncConfiguration.getExceptionLogType());
        //自动恢复失败的任务
        if (queue.errSize() > 0) {
            queue.moveErr();
        }
        //检测queue结构中的hash和pop的数量是否一致,不一致就进行从hash中重新灌入到pop中
        queue.syncBreak();
        DownLoadUtil.setTaskTheadCount(downUpConfig.getTaskThreadCount(), downUpConfig.getThreadMax(), downUpConfig.getThreadDownloadTimeout());
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
                throw new FailedException("首次加入下载队列的时间为" + initTime + ",未超过配置的时间间隔" + timeInterval + "h,本次提交被忽略");
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
                        ThreadUtil.sleep(5 * 1000L);
                        continue;
                    }

                    try {
                        doTaskDown(bean);
                    } catch (Exception e) {
                        log.warn("----doTask--roomId={}", bean.getRoomId(), e);
                    }
                }
            });
        }
    }

    public DownErrorInfoVo doTaskDown(DownBean bean) {
        Long curTime = System.currentTimeMillis();
        Long timeHourExpire = bean.getInitTime().getTime() + 3600000 * downUpConfig.getExpireHour();
        String roomId = bean.getRoomId();
        //如果任务超过过期时间，则删除任务，不执行，超时移除任务
        if (timeHourExpire < curTime) {
            queue.delete(roomId);
            log.warn("----doTask--roomId={} initTime={} expireHour={} expired, remove task", roomId, bean.getInitTime(), downUpConfig.getExpireHour());
            return null;
        }
        DownErrorInfoVo downError = null;
        if (SyncType.M3U8.getType().equals(bean.getSyncType())) {
            downError = doDownBeanM3u8(bean);
        } else {
            downError = doDownBeanFile(bean);
        }
        log.info("---doTask--roomID={}的下载任务已结束", bean.getRoomId());
        return downError;
    }

    /**
     * 恢复的做法是, 将异常队列的任务,塞在正常队列的尾部, 以使其与正常产生任务交叉执行.
     * 正常业务依然享有优先执行权
     */
    public int recover() {
        if (checkDownupNotOpen()) {
            return 0;
        }
        long errSize = queue.errSize();
        log.info("开始迁移异常下载队列,队列总容量为{}", errSize);
        return queue.moveErr();
    }

    private DownErrorInfoVo getDownErrorCache(DownBean bean, String roomId) {
        DownErrorInfoVo downError = downErrorService.getDownErrorLocalCache(roomId);
        if (downError == null) {
            downError = downErrorService.getDownError(roomId);
        }
        try {
            if (downError == null) {
                downError = new DownErrorInfoVo(bean);
            } else {
                if (downError.getTsErrorCountMap() != null) {
                    downError.getTsErrorCountMap().clear();
                }
                downError.getErrorMap().clear();
            }
        } catch (Exception e) {
            log.error("----getDownErrorCache--roomId={} error={}", roomId, e.getMessage(), e);
            downError = new DownErrorInfoVo(bean);
        }
        return downError;
    }

    public DownErrorInfoVo doDownBeanM3u8(String roomId) {
        if (checkDownupNotOpen()) {
            return null;
        }
        DownBean bean = queue.get(roomId);
        if (null == bean) {
            log.warn("任务队列中未查询到指定roomId={}的资源", roomId);
            throw new FailedException("任务队列中未找到此任务");
        }
        return doDownBeanM3u8(bean);
    }

    private DownErrorInfoVo doDownBeanFile(DownBean bean) {
        String roomId = bean.getRoomId();
        FileListVo fileListVo = nginxApiRest.getFileListBy(bean.getPath(), null);
        DownErrorInfoVo downError = getDownErrorCache(bean, roomId);
        downError.setTryDownCount(downError.getTryDownCount() + 1);
        downErrorService.putDownError(roomId, downError);
        DownResult result = null;
        try {
            result = DownLoadUtil.downloadFiles(fileListVo, downUpConfig.getNginxUrl(), downUpConfig.getDownPath(), downError);
        } catch (Exception e) {
            downFailCounter.incrementAndGet();
            log.error("----doDownBeanFile--roomId={}，直接从任务移除，需要调用add重新加入方可恢复", roomId);
            queue.delete(roomId);
            queue.putErr(roomId, downError);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downErr:" + e.getMessage());
            errorMap.put(roomId, bean);
            return downError;
        }

        Integer successTsNums = result.getTss().size();
        Integer totalTsNums = result.getTsNums();
        bean.setSize(totalTsNums);
        bean.setDownCount(successTsNums);
        if (!totalTsNums.equals(successTsNums)) {
            downFailCounter.incrementAndGet();
            log.error("roomId={}下载不完全,ts总数{},下载成功数{}", roomId, totalTsNums, successTsNums);
            queue.putErr(roomId, downError);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downFail");
            errorMap.put(roomId, bean);
            return downError;
        }

        DownBean downBean = queue.get(roomId);
        boolean isSuccess = false;
        if (downBean != null) {
            M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
            fileInfoVo.setFilePath(fileListVo.getPath());
            fileInfoVo.setFileCount(successTsNums);
            if (relayConfiguration.isOpen()) {
                isSuccess = relayOnSuccess(downBean, fileInfoVo);
            } else {
                isSuccess = callbackOnSuccess(downBean, fileInfoVo);
            }
        }
        if (isSuccess) {
            downSuccessCounter.incrementAndGet();
            errorMap.remove(roomId);
            queue.delete(roomId);
            Long curTime = System.currentTimeMillis();
            log.info("----doDownBeanFile--finished--roomId={}下载完成，下载数:{}，用时:{}s", roomId, successTsNums, (curTime - bean.getInitTime().getTime()) / 1000);
        }
        return downError;
    }

    /**
     * 用于中继完成自动删除，即回调删除
     *
     * @param roomId
     * @param fileInfo
     */
    public void deleteDown(String roomId, M3u8FileInfoVo fileInfo) {
        if (fileInfo == null || StringUtils.isBlank(fileInfo.getFilePath())) {
            log.warn("----deleteDown--roomId={} fileInfo invalid", roomId);
            return;
        }
        String path = null;
        //文件模式
        if (fileInfo.getFilePath().endsWith(roomId + "/")) {
            path = CommUtils.appendUrl(downUpConfig.getDownPath(), fileInfo.getFilePath());

        }
        //m3u8模式
        else {
            if (fileInfo.getFilePath().endsWith(".m3u8")) {
                path = CommUtils.appendUrl(downUpConfig.getDownPath(), roomId);
            }
        }
        if (path != null) {
            boolean isDelete = FileUtil.del(path);
            log.info("----deleteDown--roomId={} path={} isDelete={}", roomId, path, isDelete);
        }
    }

    private DownErrorInfoVo doDownBeanM3u8(DownBean bean) {
        String roomId = bean.getRoomId();
        log.info("从队列获取一个下载任务{} {},队列剩余个数{},异常数量堆积{}个 roomId={}", bean.getRoomId(), bean.getUrl(), queue.size(), queue.errSize(), roomId);
        //先下载原画,然后上传原画成功后,再下载试看的M3u8,再上传试看的资源到阿里
        String url = bean.getUrl();
        if (StringUtils.isBlank(url) || "null".equals(url)) {
            queue.delete(roomId);
            log.warn("----doDownBeanM3u8--roomId={} url={} is null remove task", roomId, url);
            return null;
        }
        String m3u8Path = downUpConfig.getRoomIdFilePath(roomId);
        String fileName = url.substring(url.lastIndexOf("/"));
        m3u8Path = CommUtils.appendUrl(m3u8Path, fileName);
        File destFile = FileUtil.file(m3u8Path);
        DownErrorInfoVo downError = getDownErrorCache(bean, roomId);
        downError.setTryDownCount(downError.getTryDownCount() + 1);
        downErrorService.putDownError(roomId, downError);
        //不重新设置M3u8,保持与原文件一致
        DownResult result = null;
        try {
            result = DownLoadUtil.downloadM3u8(roomId, url, destFile, false, downError);
        } catch (FileUnexistException e) {
            downFailCounter.incrementAndGet();
            log.error("----doDownBeanM3u8--roomId={} m3u8Path={} not found，直接从任务移除，需要调用add重新加入方可恢复", roomId, m3u8Path);
            queue.delete(roomId);
            queue.putErr(roomId, downError);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downErr:" + e.getMessage() + "," + url);
            errorMap.put(roomId, bean);
            return downError;
        }
        if (null == result) {
            log.error("roomId={}下载失败,url={}", roomId, url);
            if (notExistRemote(url)) {
                log.error("roomId={}下载失败,直接从任务移除,需调用add重新加入方可恢复,url={}", roomId, url);
                queue.delete(roomId);
                return downError;
            }
            queue.putErr(roomId, downError);
            return downError;
        }
        Integer successTsNums = result.getTss().size();
        Integer totalTsNums = result.getTsNums();
        bean.setSize(totalTsNums);
        bean.setDownCount(successTsNums);
        if (!totalTsNums.equals(successTsNums)) {
            downFailCounter.incrementAndGet();
            log.error("roomId={}下载不完全,ts总数{},下载成功数{},m3u8Path={}", roomId, totalTsNums, successTsNums, m3u8Path);
            queue.putErr(roomId, downError);
            bean.setErrorCount(bean.getErrorCount() + 1);
            bean.setError("downFail");
            errorMap.put(roomId, bean);
            return downError;
        }

        DownBean downBean = queue.get(roomId);
        boolean isSuccess = false;
        if (downBean != null) {
            M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
            fileInfoVo.setFilePath(m3u8Path);
            fileInfoVo.setFileCount(successTsNums);
            if (relayConfiguration.isOpen()) {
                isSuccess = relayOnSuccess(downBean, fileInfoVo);
            } else {
                isSuccess = callbackOnSuccess(downBean, fileInfoVo);
            }
        }
        if (isSuccess) {
            downSuccessCounter.incrementAndGet();
            errorMap.remove(roomId);
            queue.delete(roomId);
            Long curTime = System.currentTimeMillis();
            log.info("----doOneBean--finished--roomId={}下载完成，下载数:{}，用时:{}s", roomId, successTsNums, (curTime - bean.getInitTime().getTime()) / 1000);
        }
        return downError;
    }

    /**
     * 同步下载完成后回调接口
     *
     * @param downBean
     * @param fileInfoVo
     * @author chenjh
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

        String callbackUrl = null;
        try {
            callbackUrl = getCallbackUrl(roomId, callback);
        } catch (Exception e) {
            downBean.setError(e.getMessage());
            return isSuccess;
        }

        try {
            String body = JSONUtil.toJsonStr(fileInfoVo);
            log.info("----callbackOnSuccess--roomId={} callbackUrl={} req.body={}", roomId, callbackUrl, body);
            HttpResponse response = HttpUtil.createPost(callbackUrl).body(body).timeout(10000).execute();
            log.info("----callbackOnSuccess--roomId={} resp.body={}", roomId, response.body());
            if (isCallBackOk(response.body())) {
                isSuccess = true;
                downBean.setError(null);
                removeFail("callbackOnSuccess", roomId);
            }
        } catch (HttpException e) {
            callbackFailCounter.incrementAndGet();
            downBean.setError("callbackErr:" + e.getMessage());
            log.error("----callbackOnSuccess--roomId={} callbackUrl={}", roomId, callbackUrl, e);
            //如果回调失败，可以重试3次
            Integer count = failCount("callbackOnSuccess", roomId);
            if (count <= RETRY_COUNT) {
                return callbackOnSuccess(downBean, fileInfoVo);
            }
        }
        return isSuccess;
    }

    /**
     * 同步下载完成后中续下一接口
     *
     * @param downBean
     * @param fileInfoVo
     * @author chenjh
     */
    private boolean relayOnSuccess(DownBean downBean, M3u8FileInfoVo fileInfoVo) {
        boolean open = relayConfiguration.isOpen();
        String roomId = downBean.getRoomId();

        //如果不用回调，则直接通过
        if (!open) {
            return true;
        }

        boolean isSuccess = false;

        try {
            Integer ifRelayCallDel = 0;
            if (relayConfiguration.isDeleteOnSuccess()) {
                ifRelayCallDel = 1;
            }
            String url = CommUtils.getRelayUrl(downBean.getUrl(), downBean.getRoomId(), relayConfiguration.getRelayNiginx());
            log.debug("----relayOnSuccess--url={} roomId={} relayNginx={} resultUrl={}", downBean.getUrl(), downBean.getRoomId(), relayConfiguration.getRelayNiginx(), url);
            ResultData resultData = nextM3u8SyncRest.addAsync(roomId, downBean.getFormat(), url, ifRelayCallDel, downBean.getCallback());
            isSuccess = resultData.getSuccess();

            if (downBean.getIfRelayCallDel() == 1) {
                nextM3u8SyncRest.callbackDel(roomId, "true", downBean.getLastM3u8Url(), fileInfoVo);
            }
        } catch (HttpException e) {
            callbackFailCounter.incrementAndGet();
            downBean.setError("callbackErr:" + e.getMessage());
            log.error("----relayOnSuccess--roomId={}", roomId, e);
            //如果回调失败，可以重试3次
            Integer count = failCount("relayOnSuccess", roomId);
            if (count <= RETRY_COUNT) {
                return relayOnSuccess(downBean, fileInfoVo);
            }
        }
        return isSuccess;
    }

    private String getCallbackUrl(String roomId, CallbackVo callback) {
        //优先用户传的baseUrl
        String baseUrl = (String) CommUtils.nvl(callback.getBaseUrl(), callbackConfiguration.getBaseUrl());
        String paramUrl = (String) CommUtils.nvl(callback.getParamUrl(), callbackConfiguration.getParamUrl());
        if (StringUtils.isBlank(baseUrl)) {
            log.warn("----callbackOnSuccess--roomId={} callback.baseUrl is null", roomId, callback.getBaseUrl());
            throw new FailedException("baseUrl is null");
        }
        String callbackUrl = CommUtils.appendUrl(baseUrl, paramUrl);
        return callbackUrl.replace("{roomId}", roomId);
    }

    private Map<String, String> callbackCheckMap = new ConcurrentHashMap<>();

    public static final String CHECK_CALLBACK = "checkCallback";
    public static final String CHECK_RELAY = "checkRelay";

    /**
     * 每个新的回调接口检查一下回调接口的有效性，如果成功只检查一次
     * 以免接口无效任务执行完真的开始回调时发现接口不可用。
     *
     * @param roomId
     * @param callback
     * @return
     * @author chenjh
     */
    public boolean checkCallback(String roomId, CallbackVo callback) {
        String callbackUrl = this.getCallbackUrl(roomId, callback);
        boolean isOk = false;
        String checkCallbackCode = CHECK_CALLBACK + ":";
        try {
            //如果开关未开启，不用检查，直接通过
            if (!downUpConfig.isOpen()) {
                return true;
            }
            //用baseUrl当key，作用是，每个新的回调接口检查一次
            String baseUrl = CommUtils.getBaseUrl(callbackUrl);
            String result = callbackCheckMap.get(baseUrl);
            if (isCallBackOk(result)) {
                return true;
            }
            synchronized (baseUrl.intern()) {
                result = callbackCheckMap.get(baseUrl);
                if (isCallBackOk(result)) {
                    return true;
                }
                M3u8FileInfoVo fileInfoVo = new M3u8FileInfoVo();
                fileInfoVo.setFilePath("test");
                String body = JSONUtil.toJsonStr(fileInfoVo);
                log.info("----checkCallback--roomId={} callbackUrl={} req.body={}", roomId, callbackUrl, body);
                HttpResponse response = HttpUtil.createPost(callbackUrl).body(body).timeout(10000).execute();
                String responseContent = response.body();
                isOk = isCallBackOk(responseContent);
                if (!isOk) {
                    log.warn("------checkCallback-roomId={} callbackUrl={} response={}", roomId, callbackUrl, responseContent);
                    throw new FailedException(checkCallbackCode + responseContent + " invalid (need:ok)");
                }
                callbackCheckMap.put(baseUrl, responseContent);
            }
            return isOk;
        } catch (HttpException e) {
            log.error("------checkCallback-roomId={} callbackUrl={}", roomId, callbackUrl, e);
            throw new FailedException(checkCallbackCode + e.getMessage() + "," + callbackUrl);
        } catch (Exception e) {
            log.error("------checkCallback-roomId={} callbackUrl={}", roomId, callbackUrl, e);
            throw new FailedException(checkCallbackCode + e.getMessage() + "," + callbackUrl);
        }
    }

    /**
     * 中继模式：检查下一节点是否正常
     *
     * @param roomId
     * @return
     * @author chenjh
     */
    public boolean checkRelay(String roomId) {
        String nextM3u8Sync = this.relayConfiguration.getNextM3u8Sync();
        boolean isOk = false;
        final String checkRelayCode = CHECK_RELAY + ":";
        try {
            //如果开关未开启，不用检查，直接通过
            if (!relayConfiguration.isOpen()) {
                return true;
            }
            //用baseUrl当key，作用是，每个新的回调接口检查一次
            String baseUrl = nextM3u8Sync;
            String result = callbackCheckMap.get(baseUrl);
            if (isCallBackOk(result)) {
                return true;
            }
            synchronized (baseUrl.intern()) {
                result = callbackCheckMap.get(baseUrl);
                if (isCallBackOk(result)) {
                    return true;
                }
                log.info("----checkRelay--roomId={} nextM3u8Sync={}", roomId, nextM3u8Sync);
                CallbackVo callback = new CallbackVo();
                callback.setBaseUrl(callbackConfiguration.getBaseUrl());
                callback.setParamUrl(callbackConfiguration.getParamUrl());
                ResultData resultData = nextM3u8SyncRest.addAsync(roomId, "test", "test", 0, callback);
                String responseContent = (String) resultData.getData();
                isOk = isCallBackOk(responseContent);
                if (!isOk) {
                    log.warn("------checkRelay-roomId={} nextM3u8Sync={} response={}", roomId, nextM3u8Sync, responseContent);
                    throw new FailedException(checkRelayCode + responseContent + " invalid (need:ok)");
                }
                callbackCheckMap.put(baseUrl, responseContent);
            }
            return isOk;
        } catch (HttpException e) {
            log.error("------checkRelay-roomId={} nextM3u8Sync={}", roomId, nextM3u8Sync, e);
            throw new FailedException(checkRelayCode + e.getMessage() + "," + nextM3u8Sync);
        } catch (Exception e) {
            log.error("------checkRelay-roomId={} nextM3u8Sync={}", roomId, nextM3u8Sync, e);
            throw new FailedException(checkRelayCode + e.getMessage() + "," + nextM3u8Sync);
        }
    }

    /**
     * 回调接口必须返回ok才算成功
     *
     * @param body
     * @return
     * @author chenjh
     */
    private boolean isCallBackOk(String body) {
        return "ok".equals(body);
    }

    private boolean notExistRemote(String url) {
        try {
            HttpResponse response = HttpRequest.head(url).setConnectionTimeout(5000).setReadTimeout(5000).execute();
            int status = response.getStatus();
            return 404 == status;
        } catch (Exception e) {
            log.error("-----notExistRemote--url={}", e);
            return false;
        }
    }

    public Map<String, Object> status(String type) {
        Map<String, Object> treeMap = new TreeMap<>();
        if (StringUtils.isBlank(type) || "help".equals(type)) {
            treeMap.put("usage", "type=help,all,count,config,errors,queues");
            return treeMap;
        }

        boolean isAll = "all".equals(type);

        if (isAll || "errors".equals(type)) {
            treeMap.put("errors", queue.errors(20));
        }

        if (isAll || "queues".equals(type)) {
            treeMap.put("queues", queue.queues(20));
        }

        if (isAll || "count".equals(type)) {
            treeMap.put("downSuccessCount", downSuccessCounter.get());
            treeMap.put("downFailCount", downFailCounter.get());
            treeMap.put("callbackFailCount", downFailCounter.get());
            treeMap.put("queueSize", queue.size());
            treeMap.put("errorSize", queue.errSize());
        }

        if (isAll || "config".equals(type)) {
            treeMap.put("downUpConfig", downUpConfig);
            treeMap.put("callbackConfig", callbackConfiguration);
            treeMap.put("relayConfig", relayConfiguration);
        }
        return treeMap;
    }

    private static Map<String, Integer> failCountMap = new ConcurrentHashMap<>();
    //失败重试次数
    private static final int RETRY_COUNT = 3;

    /**
     * 失败次一次，休眠时间+5杪，比如：5秒，10秒，15秒
     *
     * @param code
     * @param roomId
     * @return
     * @author chenjh
     */
    private Integer failCount(String code, String roomId) {
        String errorKey = code + ":" + roomId;
        Integer count = failCountMap.get(errorKey);
        if (count == null) {
            count = 0;
        }
        count++;
        failCountMap.put(errorKey, count);
        log.warn("---failCount-roomId={} code={} count={} errorCount={}", roomId, code, count, errorMap.size());
        //小休眠一下
        ThreadUtil.sleep(1000L * count * 5);
        return count;
    }

    private void removeFail(String code, String roomId) {
        String errorKey = code + ":" + roomId;
        failCountMap.remove(errorKey);
    }

    public void remove(String roomId) {
        queue.delete(roomId);
    }
}
