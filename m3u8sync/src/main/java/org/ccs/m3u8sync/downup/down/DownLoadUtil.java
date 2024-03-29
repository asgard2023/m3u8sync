package org.ccs.m3u8sync.downup.down;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrMatcher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.ccs.m3u8sync.downup.domain.DownErrorInfoVo;
import org.ccs.m3u8sync.exceptions.FileUnexistException;
import org.ccs.m3u8sync.utils.CommUtils;
import org.ccs.m3u8sync.vo.FileInfoVo;
import org.ccs.m3u8sync.vo.FileListVo;
import org.springframework.http.HttpStatus;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class DownLoadUtil {
    private DownLoadUtil() {

    }

    private static int maxThread = 40; //下载并发的最大线程数
    private static int taskTheadCount = 20; //线程池的最小线程数
    private static int threadDownloadExpire = 30;
    //这里使用最大容量线程池以应对可能存在的多线程调用
    private static Executor consumer = ThreadUtil.newExecutor(taskTheadCount, maxThread);

    public static void setTaskTheadCount(Integer taskTheadCount, Integer maxThread, int threadDownloadExpire) {
        DownLoadUtil.taskTheadCount = taskTheadCount;
        DownLoadUtil.maxThread = maxThread;
        DownLoadUtil.threadDownloadExpire = threadDownloadExpire;
        consumer = ThreadUtil.newExecutor(taskTheadCount, maxThread);
    }

    /**
     * 示例返回 https://xxx.com
     *
     * @param url url地址
     * @return
     */
    private static String getDomainPrefix(String url) {
        int index = url.replaceAll("//", "aa").indexOf("/");
        return url.substring(0, index);
    }

    /**
     * 示例返回 https://xxx.com
     *
     * @param url
     * @return
     */
    private static String getUrlPrefix(String url) {
        int index = url.replaceAll("//", "aa").lastIndexOf("/");
        return url.substring(0, index);
    }

    /**
     * 合并URL
     *
     * @param domainPrefix https://xxx.com
     * @param urlSuffix    /love.ts 或者 love.ts
     * @return https://xxx.com/love.ts
     */
    private static String mergeUrl(String domainPrefix, String urlSuffix) {
        if (urlSuffix.startsWith("/")) {
            return domainPrefix + urlSuffix;
        }
        return domainPrefix + "/" + urlSuffix;
    }

    /**
     * 拼接m3u8的下载地址
     *
     * @param m3u8Url
     * @param url
     * @return
     */
    public static String parseUrl(String m3u8Url, String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return mergeUrl(getDomainPrefix(m3u8Url), url);
    }

    /**
     * 拼接tsUrl的下载地址
     *
     * @param m3u8Url
     * @param url
     * @return
     */
    public static String parseTsUrl(String m3u8Url, String url) {
        if (url.startsWith("http")) {
            return url;
        }
        if (url.startsWith("/")) {
            return mergeUrl(getDomainPrefix(m3u8Url), url);
        }
        return mergeUrl(getUrlPrefix(m3u8Url), url);
    }


    /**
     * 如果Key带有 "/" 前缀, 那么拼接域名; 如果没有"/" 则拼接m3u8的地址
     *
     * @param m3u8Url
     * @param key
     * @return
     */
    public static String parseKeyUrl(String m3u8Url, String key) {
        if (key.startsWith("http")) {
            return key;
        }
        if (key.length() == 16) {
            return key;
        }
        //需要替换域名前缀,如果不正确, 则需要使用拼接方式
        if (key.startsWith("/")) {
            return mergeUrl(getDomainPrefix(m3u8Url), key);
        }
        //需要拼接
        return mergeUrl(getUrlPrefix(m3u8Url), key);
    }

    /**
     * 解析TsUrl,读取Ts的文件名[带后缀] [带参数]
     * 兼容腾讯格式, 多个TS文件名共享前缀,但是可通过时间戳进行区别
     * 例如: 1041014962_935768979_1.ts?start=0&end=198151
     *
     * @param tsUrl
     * @return
     */
    public static String obtainTsUrl(String tsUrl, boolean hasSuffix) {
        int index = tsUrl.lastIndexOf("/");
        if (index != -1) {
            tsUrl = tsUrl.substring(index + 1);
        }
        if (hasSuffix) {
            return tsUrl;
        }
        int param = tsUrl.indexOf("?");
        if (param == -1) {
            return tsUrl;
        }
        //含?后参数,判定是否是特殊格式, 需重命名ts分片名称
        //腾讯m3u8的ts分片支持
        if (tsUrl.indexOf("start=") != -1 && tsUrl.indexOf("end=") != -1) {
            String pattern = "${tsName}.ts?start=${start}&end=${end}&${other}";
            Map<String, String> map = new StrMatcher(pattern).match(tsUrl);
            StringBuilder sb = new StringBuilder();
            sb.append(map.get("tsName"));
            sb.append("_");
            sb.append(map.get("start"));
            sb.append("_");
            sb.append(map.get("end"));
            sb.append(".ts");
            return sb.toString();
        }
        return tsUrl.substring(0, param);
    }

    public static DownResult downloadM3u8NoException(String roomId, String m3u8Url, File destFile, boolean reM3u8, DownErrorInfoVo downErrorInfoVo) {
        try {
            return downloadM3u8(roomId, m3u8Url, destFile, reM3u8, downErrorInfoVo);
        } catch (Exception e) {
            log.error("downloadM3u8NoException--roomId={} m3u8Url={},下载失败,e={}", roomId, m3u8Url, e.getMessage());
            downErrorInfoVo.getErrorMap().put(m3u8Url, e.getMessage());
            return null;
        }
    }

    public static DownResult downloadFiles(final FileListVo fileListVo, final String nginxUrl, final String downPath, DownErrorInfoVo downErrorInfoVo){
        List<String> tsUrls=new ArrayList<>();
        for(FileInfoVo fileInfoVo: fileListVo.getFiles()){
            String url= CommUtils.appendUrl(nginxUrl, fileListVo.getPath());
            url=CommUtils.appendUrl(url, fileInfoVo.getFileName());
            tsUrls.add(url);
        }
        String destPath=downPath;
        if(fileListVo.getPath()!=null) {
            destPath = CommUtils.appendUrl(destPath, fileListVo.getPath());
        }
        String deseFilePath=CommUtils.appendUrl(destPath, "test.txt");
        File destFile = FileUtil.file(deseFilePath);
        //下载所有的ts,并返回ts目录
        List<String> successLines = DownLoadUtil.downTsByThread(destFile, null, tsUrls, downErrorInfoVo);
        return new DownResult(destFile, tsUrls.size(), successLines);
    }

    /**
     * 从m3u8文件中提取二级m3u8地址,并继续下载, 下载超时10秒
     *
     * @param m3u8Url  下载的m3u8原始地址
     * @param destFile 本地保存的m3u8文件
     * @param reM3u8   是否重建本地m3u8
     */
    public static DownResult downloadM3u8(String roomId, String m3u8Url, File destFile, boolean reM3u8, DownErrorInfoVo downErrorInfoVo) {
        log.info("downloadM3u8--roomId={} m3u8Url={},下载文件路径={}", roomId, m3u8Url, destFile.getAbsolutePath());
        File file;
        try {
            file = HttpUtil.downloadFileFromUrl(m3u8Url, destFile, threadDownloadExpire * 1000);
        } catch (Exception e) {
            log.error("roomId={} m3u8Url={},下载失败,e={}", roomId, m3u8Url, e.getMessage());
            downErrorInfoVo.getErrorMap().put(m3u8Url, e.getMessage());
            throw new FileUnexistException(e.getMessage());
        }
        List<String> lines = FileUtil.readLines(file, StandardCharsets.UTF_8);
        TsKey tsKey = null;
        List<String> tsUrls = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            //嵌套m3u8递归处理
            if (line.contains(".m3u8")) {
                log.info("嵌套m3u8解析,并重新下载,m3u8={},line={}", m3u8Url, line);
                downloadM3u8NoException(roomId, parseUrl(m3u8Url, line), destFile, reM3u8, downErrorInfoVo);
                break;
            }
            //多码率处理
            else if (line.contains("#EXT-X-STREAM-INF")) {
                //用下一个链接替换s,虽然不一定是高清的,但
                String newM3u8Url = lines.get(++i);
                log.info("多码率文件提供,选择首个码率进行下载,m3u8={},line={},newM3u8={}", m3u8Url, line, newM3u8Url);
                downloadM3u8NoException(roomId, parseUrl(m3u8Url, newM3u8Url), destFile, reM3u8, downErrorInfoVo);
                break;
            }
            //解析key,iv
            else if (line.contains("#EXT-X-KEY")) {
                tsKey = getKey(m3u8Url, line);
                log.info("ts分片加密,解析key和iv进行解密,tsKey={}", tsKey.toString());
            }
            //解析ts分片
            else if (line.contains("#EXTINF")) {
                String tsUrl = lines.get(++i);
                if (CharSequenceUtil.isBlank(tsUrl) || !tsUrl.contains(DownConstant.TS)) {
                    continue;
                }
                //存入Ts的下载路径,如果原来的地址带签名参数,现在仍旧带签名参数
                tsUrls.add(parseTsUrl(m3u8Url, tsUrl));
            }
        }
        //下载所有的ts,并返回ts目录
        List<String> successLines = downTsByThread(file, tsKey, tsUrls, downErrorInfoVo);
        //重新编排m3u8
        if (reM3u8) {
            resetM3u8BySuccessTs(m3u8Url, file, lines, successLines);
        }
        return new DownResult(file, tsUrls.size(), successLines);
    }

    /**
     * 1.移除key 2.重命名ts分片名称 3.裁剪下载失败的分片,仅保留成功下载的分片数
     *
     * @param m3u8Url
     * @param file
     * @param lines
     * @param successLines
     */
    private static void resetM3u8BySuccessTs(String m3u8Url, File file, List<String> lines, List<String> successLines) {
        List<String> newLines = new ArrayList<>(successLines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains(DownConstant.KEY) || line.startsWith("#EXT-X-PROGRAM-DATE-TIME")) {
                continue;
            }
            if (line.contains(DownConstant.TS)) {
                //下载失败的line,需要移除其m3u8中的时间戳,即上一行
                if (!successLines.contains(parseTsUrl(m3u8Url, line))) {
                    newLines.remove(newLines.size() - 1);
                    continue;
                }
                line = obtainTsUrl(line, false);
            }
            newLines.add(line);
        }
        FileUtil.writeUtf8Lines(newLines, file);
        log.info("重新编写m3u8文件完成,path= {}", file.getAbsolutePath());
    }


    /**
     * 通过多线程下载m3u8分片的内容, 如果出错,会放入队列，再次下载一次。如果仍然出错就反馈到结果中
     *
     * @param m3u8File
     * @param tsKey
     * @param tsUrls
     * @return
     */
    private static List<String> downTsByThread(File m3u8File, TsKey tsKey, List<String> tsUrls, DownErrorInfoVo downErrorInfoVo) {

        int totalSize = tsUrls.size();
        downErrorInfoVo.setTotalTsNums(totalSize);
        ArrayBlockingQueue<String> task = m3u8TsToQueueTask(tsUrls);



        //需要注意线程安全
        downErrorInfoVo.setSuccessLineMap(new ConcurrentHashMap<>());
        downErrorInfoVo.setSuccessLines(new Vector<>());
        downErrorInfoVo.setErrUrls(new Vector<>());
        //运行下载线程
        runTaskDownThread(m3u8File, tsKey, task, downErrorInfoVo);
        //显示线程下载进度，如果下载未完成就休眠等待
        showWaitDownProcess(m3u8File, totalSize, downErrorInfoVo);
        //增加task的强移除, 确保分线程能够正常退出
        task.clear();
        return MapUtil.sort(downErrorInfoVo.getSuccessLineMap()).values().stream().collect(Collectors.toList());
    }

    /**
     * 显示线程下载进度
     *
     * @param m3u8File
     * @param totalSize
     */
    private static void showWaitDownProcess(File m3u8File, int totalSize, DownErrorInfoVo downErrorInfoVo) {
        String roomId=downErrorInfoVo.getRoomId();
        //阻塞并监听当前进度
        int totalWaitTimes = 5;
        int initWaitTimes = 0;
        String fileName = m3u8File.getName();
        int oldSuccessSize = 0;
        int sleepTime = threadDownloadExpire * 1000;
        List<String> successLines=downErrorInfoVo.getSuccessLines();
        List<String> errUrls=downErrorInfoVo.getErrUrls();
        //这里是单线程循环
        while (true) {
            ThreadUtil.sleep(sleepTime);
            //这里读取size, 将导致errUrls和successLines 线程不安全
            int errSize = errUrls.size();
            int successSize = successLines.size();
            downErrorInfoVo.setSuccessTsNums(successSize);
            log.info("检测任务roomId={} fileName={},执行进度,失败数/成功下载/总数: {}/{}/{} ,异常监听次数:{}(与上次检测时成功数没变化异常监听次数+1)", roomId, fileName, errSize, successSize, totalSize, initWaitTimes);
            if ((successSize + errSize) == totalSize || initWaitTimes >= totalWaitTimes) {
                if (successSize == totalSize) {
                    log.info("任务roomId={} fileName={}下载任务已结束", roomId, fileName);
                } else {
                    log.info("下载任务roomId={} fileName={}已结束,但不完整下载,errSize={}", roomId, fileName, errSize);
                    if (errSize > 0) {
                        //这里循环会导致errUrls线程不安全
                        errUrls.forEach(url -> {
                            log.error("roomId={}下载失败的链接: {}", roomId, url);
                        });
                    }
                }
                break;
            }
            //比较两次下载简格，如果成功数没有变化，视为异常
            if (oldSuccessSize == successSize) {
                initWaitTimes++;
            }
            oldSuccessSize = successSize;
        }
        int errSize = errUrls.size();
        int successSize = successLines.size();
        downErrorInfoVo.setSuccessTsNums(successSize);
        log.info("下载任务{}已结束, 失败数/成功下载/总数: {}/{}/{}", fileName, errSize, successSize, totalSize);
    }

    private static final String SEPARATOR = "__";

    /**
     * m3u8 ts转成队列
     *
     * @param tsUrls
     * @return
     */
    private static ArrayBlockingQueue<String> m3u8TsToQueueTask(List<String> tsUrls) {
        ArrayBlockingQueue<String> task = new ArrayBlockingQueue<>(tsUrls.size());
        for (int i = 0; i < tsUrls.size(); i++) {
            String tsUrl = tsUrls.get(i);
            try {
                //将链接原顺序加入到信息栏位中
                task.put(i + SEPARATOR + tsUrl);
            } catch (InterruptedException e) {
                log.error("任务队列添加失败,e={}", e.getMessage());
            }
        }
        return task;
    }

    /**
     * 运行下载线程
     *
     * @param m3u8File
     * @param tsKey
     * @param task
     */
    private static void runTaskDownThread(File m3u8File, TsKey tsKey, ArrayBlockingQueue<String> task, DownErrorInfoVo downErrorInfoVo) {
        //失败的链接的索引
        final Set<String> errIndexs = new ConcurrentHashSet<>();
        for (int i = 1; i < taskTheadCount; i++) {
            consumer.execute(() -> {
                while (!task.isEmpty()) {
                    try {
//                        获取一个ts进行下载
                        String tsLine = task.take();
                        runTaskDown(m3u8File, tsKey, task, downErrorInfoVo, errIndexs, tsLine);
                    } catch (Exception e) {
                        log.warn("---runTaskDownThread--队列读取异常,roomId={}线程退出,错误信息e={}", downErrorInfoVo.getRoomId(), e.getMessage());
                        addExceptioinLog(downErrorInfoVo, e.getMessage());
                        break;
                    }
                }
            });
        }
    }

    private static void runTaskDown(File m3u8File, TsKey tsKey, ArrayBlockingQueue<String> task, DownErrorInfoVo downErrorInfoVo, Set<String> errIndexs, String tsLine) throws InterruptedException {
        String index = tsLine.split(SEPARATOR)[0];
        String url = tsLine.split(SEPARATOR)[1];
        String roomId = downErrorInfoVo.getRoomId();
        boolean hasDown = downloadTs(roomId, m3u8File, tsKey, url, downErrorInfoVo);
        if (hasDown) {
            downErrorInfoVo.getSuccessLines().add(url);
            downErrorInfoVo.setSuccessTsNums(downErrorInfoVo.getSuccessLines().size());
            downErrorInfoVo.getSuccessLineMap().put(Integer.parseInt(index), url);
        } else {
            //增加锁判断
            if (!errIndexs.contains(index)) {
                if (!downErrorInfoVo.getErrUrls().contains(url)) {
                    log.warn("---runTaskDownThread--roomId={} url={},下载失败,重新放入下载队列", roomId, url);
                    downErrorInfoVo.getErrUrls().add(url);
                    task.put(tsLine);
                }
            } else {
                downErrorInfoVo.getErrUrls().add(url);
                errIndexs.add(index);
            }
        }
    }

    private static void addExceptioinLog(DownErrorInfoVo downError, String key) {
        if(key==null){
            return;
        }
        AtomicInteger tsErrorCount= downError.getTsErrorCounterMap().get(key);
        if(tsErrorCount==null){
            tsErrorCount=new AtomicInteger();
            downError.getTsErrorCounterMap().put(key, tsErrorCount);
        }
        tsErrorCount.incrementAndGet();
    }


    private static TsKey getKey(String m3u8Url, String lineKey) {
        String key = null;
        String iv = "";
        String method = null;
        log.info("ts分片加密,解析key和iv进行解密");
        //先按照逗号进行分割
        String[] strs = lineKey.split(",");
        for (String keyInfo : strs) {
            if (keyInfo.contains("METHOD")) {
                method = keyInfo.split("=", 2)[1];
                continue;
            }
            //
            if (keyInfo.contains("URI")) {
                //判断key是否是一个链接
                String keyUrl = keyInfo.replaceFirst("URI=", "").replaceAll("\"", "");
                String kUrl = parseKeyUrl(m3u8Url, keyUrl);
                if (kUrl.length() == 16) {
                    key = kUrl;
                } else {
                    key = HttpUtil.get(kUrl);
                }
                continue;
            }
            if (keyInfo.contains("IV")) {
                iv = keyInfo.split("=", 2)[1];
            }
        }
        return new TsKey(iv, method, key);
    }

    /**
     * 判断远程文件的size大小
     * 支持nginx gzip模式的获取文件长度
     *
     * @param downloadUrl
     * @return
     */
    private static long getRemoteSize(String downloadUrl, DownErrorInfoVo downErrorInfoVo) {
        try {
            Long response = getRemoteSize(downloadUrl, 5000);
            if (response != null) return response;
        } catch (Exception e) {
            addExceptioinLog(downErrorInfoVo, e.getMessage());
            log.error("---getRemoteSize--{} error={}", downloadUrl, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * hutool通过head取文件在小，如果出现405则再通过url conn再试一次
     *
     * @param downloadUrl
     * @param timeout
     * @return
     * @author chenjh
     */
    public static Long getRemoteSize(String downloadUrl, int timeout) {
        HttpResponse response = HttpRequest.head(downloadUrl).setReadTimeout(timeout).setConnectionTimeout(timeout)
                //使nginx支持gzip时，仍然可以拿到contentLength
                .header(Header.ACCEPT_ENCODING, "none").execute();
        if (response.isOk()) {
            return response.contentLength();
        } else {
            log.warn("getRemoteSize-url={} status={}", downloadUrl, response.getStatus());
            if (response.getStatus() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                try {
                    return getRemoteLength(downloadUrl, timeout);
                } catch (IOException e) {
                    log.error("getRemoteSize-url={} err={}", downloadUrl, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * @return long
     * @Author chenjh
     * @Description //获取网络文件大小
     * @Param [downloadUrl]
     */
    private static Long getRemoteLength(String downloadUrl, int timeout) throws IOException {
        if (downloadUrl == null || "".equals(downloadUrl)) {
            return 0L;
        }
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows 7; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.73 Safari/537.36 YNoteCef/5.8.0.1 (Windows)");
            conn.setReadTimeout(timeout);
            conn.setConnectTimeout(timeout);
            return (long) conn.getContentLength();
        } catch (Exception e) {
            log.error("getRemoteLength-url={} err={}", downloadUrl, e.getMessage(), e);
            return 0L;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 下载并保存Ts分片,默认超时时间10s
     *
     * @param m3u8File
     * @param tsKey
     * @param tsUrl
     * @return
     * @throws Exception
     */
    private static boolean downloadTs(String roomId, File m3u8File, TsKey tsKey, String tsUrl, DownErrorInfoVo downErrorInfoVo) {
        File pFile = m3u8File.getParentFile();
        //重定义文件名
        File tsDestFile = new File(pFile, obtainTsUrl(tsUrl, false));
        //判定本地文件是否存在
        if (tsDestFile.exists()) {
            //判断本地文件是否完整
            long localSize = tsDestFile.length();
            long remoteSize = getRemoteSize(tsUrl, downErrorInfoVo);
            if (localSize == remoteSize) {
                return true;
            }
            log.info("---downloadTs--roomId={} m3u8File={}的远程文件{}大小为{},本地文件大小为{},需要重新下载", roomId, m3u8File.getName(), tsDestFile.getName(), remoteSize, localSize);
        }
        //30s下载时长
        int timeOut = threadDownloadExpire * 1000;
        long time = System.currentTimeMillis();
        boolean hasDown = false;
        try {
            if (tsKey != null) {
                //需要解密,下载最多设置为10s
                byte[] ensBytes = HttpDownloadExtend.downloadBytes(tsUrl, timeOut);
                byte[] bytes = new byte[0];
                try {
                    bytes = decrypt(ensBytes, tsKey);
                    FileUtil.writeBytes(bytes, tsDestFile);
                    hasDown = true;
                } catch (Exception e) {
                    log.error("downloadTs--roomId={} tsUrl={},解密失败,e={}", roomId, tsUrl, e.getMessage());
                }
            } else {
                HttpUtil.downloadFileFromUrl(tsUrl, tsDestFile, timeOut);
                hasDown = true;
            }
            log.debug("downloadTs--roomId={} tsUrl={},hasDown={} time={}", roomId, tsUrl, hasDown, System.currentTimeMillis() - time);
            Long runTime=System.currentTimeMillis()-time;
            if(runTime>downErrorInfoVo.getMaxDownTime()){
                downErrorInfoVo.setMaxDownTime(runTime.intValue());
            }
        } catch (Exception e) {
            log.error("downloadTs--roomId={} tsUrl={},time={} error={}", roomId, tsUrl, System.currentTimeMillis() - time, e.getMessage());
            addExceptioinLog(downErrorInfoVo, e.getMessage());
            Long runTime=System.currentTimeMillis()-time;
            if(runTime>downErrorInfoVo.getMaxFailTime()){
                downErrorInfoVo.setMaxFailTime(runTime.intValue());
            }
        }
        return hasDown;
    }

    /**
     * 解密ts
     *
     * @param sSrc ts文件字节数组
     * @return 解密后的字节数组
     */
    private static byte[] decrypt(byte[] sSrc, TsKey tsKey) throws Exception {
        String sKey = tsKey.getKey();
        String method = tsKey.getMethod();
        String iv = tsKey.getIv();
        if (CharSequenceUtil.isNotEmpty(method) && !method.contains("AES"))
            throw new Exception("未知的算法！");
        // 判断Key是否正确
        if (CharSequenceUtil.isEmpty(sKey))
            return null;
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            throw new Exception("Key长度不是16位！");
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(sKey.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] ivByte;
        if (null != iv) {
            if (iv.startsWith("0x"))
                ivByte = hexStringToByteArray(iv.substring(2));
            else ivByte = iv.getBytes();
            if (ivByte.length != 16)
                ivByte = new byte[16];
            //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
        }
        int length = sSrc.length;
        return cipher.doFinal(sSrc, 0, length);
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if ((len & 1) == 1) {
            s = "0" + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
