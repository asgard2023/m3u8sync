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
import org.ccs.m3u8sync.exceptions.FileUnexistException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
public class DownLoadUtil {
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
     * @param url
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

    public static DownResult downloadM3u8NoException(String roomId, String m3u8Url, File destFile, boolean reM3u8) {
        try {
            return downloadM3u8(roomId, m3u8Url, destFile, reM3u8);
        } catch (Exception e) {
            log.error("downloadM3u8NoException--roomId={} m3u8Url={},下载失败,e={}", roomId, m3u8Url, e.getMessage());
            return null;
        }
    }

    /**
     * 从m3u8文件中提取二级m3u8地址,并继续下载, 下载超时10秒
     *
     * @param m3u8Url  下载的m3u8原始地址
     * @param destFile 本地保存的m3u8文件
     * @param reM3u8   是否重建本地m3u8
     */
    public static DownResult downloadM3u8(String roomId, String m3u8Url, File destFile, boolean reM3u8) {
        log.info("downloadM3u8--roomId={} m3u8Url={},下载文件路径={}", roomId, m3u8Url, destFile.getAbsolutePath());
        File file;
        try {
            file = HttpUtil.downloadFileFromUrl(m3u8Url, destFile, threadDownloadExpire * 1000);
        } catch (Exception e) {
            log.error("roomId={} m3u8Url={},下载失败,e={}", roomId, m3u8Url, e.getMessage());
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
                downloadM3u8NoException(roomId, parseUrl(m3u8Url, line), destFile, reM3u8);
                break;
            }
            //多码率处理
            if (line.contains("#EXT-X-STREAM-INF")) {
                //用下一个链接替换s,虽然不一定是高清的,但
                String newM3u8Url = lines.get(++i);
                log.info("多码率文件提供,选择首个码率进行下载,m3u8={},line={},newM3u8={}", m3u8Url, line, newM3u8Url);
                downloadM3u8NoException(roomId, parseUrl(m3u8Url, newM3u8Url), destFile, reM3u8);
                break;
            }
            //解析key,iv
            if (line.contains("#EXT-X-KEY")) {
                tsKey = getKey(m3u8Url, line);
                log.info("ts分片加密,解析key和iv进行解密,tsKey={}", tsKey.toString());
            }
            //解析ts分片
            if (line.contains("#EXTINF")) {
                String tsUrl = lines.get(++i);
                if (CharSequenceUtil.isBlank(tsUrl) || !tsUrl.contains(DownConstant.TS)) {
                    continue;
                }
                //存入Ts的下载路径,如果原来的地址带签名参数,现在仍旧带签名参数
                tsUrls.add(parseTsUrl(m3u8Url, tsUrl));
            }
        }
        //下载所有的ts,并返回ts目录
        List<String> successLines = downTsByThread(roomId, file, tsKey, tsUrls);
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
            if (line.contains(DownConstant.KEY)) {
                continue;
            }
            if (line.startsWith("#EXT-X-PROGRAM-DATE-TIME")) {
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
    private static List<String> downTsByThread(String roomId, File m3u8File, TsKey tsKey, List<String> tsUrls) {
        Map<Integer, String> successLineMap = new ConcurrentHashMap<>();
        int totalSize = tsUrls.size();
        ArrayBlockingQueue<String> task = m3u8TsToQueueTask(tsUrls);

        // 这两个需要注意线程安全
        List<String> successLines = new Vector<>();
        List<String> errUrls = new Vector<>();
        //运行下载线程
        runTaskDownThread(roomId, m3u8File, tsKey, successLineMap, task, successLines, errUrls);
        //显示线程下载进度，如果下载未完成就休眠等待
        showWaitDownProcess(roomId, m3u8File, totalSize, successLines, errUrls);
        //增加task的强移除, 确保分线程能够正常退出
        task.clear();
        return MapUtil.sort(successLineMap).values().stream().collect(Collectors.toList());
    }

    /**
     * 显示线程下载进度
     *
     * @param m3u8File
     * @param totalSize
     * @param successLines
     * @param errUrls
     */
    private static void showWaitDownProcess(String roomId, File m3u8File, int totalSize, List<String> successLines, List<String> errUrls) {
        //阻塞并监听当前进度
        int totalWaitTimes = 5;
        int initWaitTimes = 0;
        String fileName = m3u8File.getName();
        int oldSuccessSize = 0;
        int sleepTime = threadDownloadExpire * 1000;
        //这里是单线程循环
        while (true) {
            ThreadUtil.sleep(sleepTime);
            //这里读取size, 将导致errUrls和successLines 线程不安全
            int errSize = errUrls.size();
            int successSize = successLines.size();
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
        log.info("下载任务{}已结束, 失败数/成功下载/总数: {}/{}/{}", fileName, errSize, successSize, totalSize);
    }

    private static final String separator = "__";

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
                task.put(i + separator + tsUrl);
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
     * @param successLineMap
     * @param task
     * @param successLines
     * @param errUrls
     */
    private static void runTaskDownThread(final String roomId, File m3u8File, TsKey tsKey, Map<Integer, String> successLineMap, ArrayBlockingQueue<String> task, List<String> successLines, List<String> errUrls) {
        //失败的链接的索引
        Set<String> errIndexs = new ConcurrentHashSet<>();
        for (int i = 1; i < taskTheadCount; i++) {
            consumer.execute(() -> {
                while (!task.isEmpty()) {
                    try {
//                        获取一个ts进行下载
                        String tsLine = task.take();
                        String index = tsLine.split(separator)[0];
                        String url = tsLine.split(separator)[1];
                        boolean hasDown = downloadTs(roomId, m3u8File, tsKey, url);
                        if (hasDown) {
                            successLines.add(url);
                            successLineMap.put(Integer.parseInt(index), url);
                        } else {
                            //增加锁判断
                            if (!errIndexs.contains(index)) {
                                if(!errUrls.contains(url)) {
                                    log.warn("---runTaskDownThread--roomId={} url={},下载失败,重新放入下载队列", roomId, url);
                                    errUrls.add(url);
                                    task.put(tsLine);
                                }
                            } else {
                                errUrls.add(url);
                                errIndexs.add(index);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("---runTaskDownThread--队列读取异常,roomId={}线程退出,错误信息e={}", roomId, e.getMessage());
                        break;
                    }
                }
            });
        }
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
    private static long getRemoteSize(String downloadUrl) {
        try {
            Long response = getRemoteSize(downloadUrl, 5000);
            if (response != null) return response;
        } catch (Exception e) {
            log.error("---getRemoteSize--{} error={}", downloadUrl, e.getMessage(), e);
        }
        return 0;
    }

    public static Long getRemoteSize(String downloadUrl, int timeout) {
        HttpResponse response = HttpRequest.head(downloadUrl).setReadTimeout(timeout).setConnectionTimeout(timeout)
                //使nginx支持gzip时，仍然可以拿到contentLength
                .header(Header.ACCEPT_ENCODING, "none").execute();
        if (response.isOk()) {
            return response.contentLength();
        }
        return null;
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
    private static boolean downloadTs(String roomId, File m3u8File, TsKey tsKey, String tsUrl) {
        File pFile = m3u8File.getParentFile();
        //重定义文件名
        File tsDestFile = new File(pFile, obtainTsUrl(tsUrl, false));
        //判定本地文件是否存在
        if (tsDestFile.exists()) {
            //判断本地文件是否完整
            long localSize = tsDestFile.length();
            long remoteSize = getRemoteSize(tsUrl);
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
        } catch (Exception e) {
            log.error("downloadTs--roomId={} tsUrl={},time={} error={}", roomId, tsUrl, System.currentTimeMillis() - time, e.getMessage());
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

    public static void main(String[] args) throws Exception {
        String m3u8Url = "https://maoyun-02.yxaar.com/sv/2715466f-180d9d0c01f/2715466f-180d9d0c01f.m3u8?auth_key=1652912051-32d96e2-0-5245976a7b15d582431bd6a73ff86c36";
/*        //业务上更合适的是定义 title
        String title = "11756328A20211208115007";
        String fileName =  title + DownConstant.M3U8;
        String baseDir = "F:\\download\\movie\\";
        FileUtil.mkdir(baseDir + title);
        File destFile = new File(baseDir + title , fileName);
        downloadM3u8(m3u8Url,destFile,true);*/
        long size = getRemoteSize(m3u8Url);
        log.info("大小为:" + size);
    }

}
