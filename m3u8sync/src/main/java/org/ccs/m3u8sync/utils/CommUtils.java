package org.ccs.m3u8sync.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * 常用工具类
 */
public class CommUtils {
    private CommUtils() {

    }

    /**
     * 取数据前maxLength位
     *
     * @param str string str
     * @param maxLength
     * @return
     */
    public static String getStringLimit(String str, int maxLength) {
        return str != null && str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public static Object nvl(Object... objects) {
        for (Object obj : objects) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public static String getStringFirst(String str, String split) {
        if (str == null) {
            return null;
        }
        int idx = str.indexOf(split);
        if (idx > 0) {
            return str.substring(0, idx);
        }
        return str;
    }

    public static String getString(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof String) {
            return (String) obj;
        } else {
            return "" + obj;
        }
    }

    /**
     * @return isStartWith
     */
    public static boolean startWithChar(String str, char ch) {
        if (str == null || str.length() < 1) {
            return false;
        }
        return str.charAt(0) == ch;
    }

    /**
     * @return isEndWith
     */
    public static boolean endWithChar(String str, char ch) {
        if (str == null || str.length() < 1) {
            return false;
        }
        return str.charAt(str.length() - 1) == ch;
    }

    /**
     * 只要确保你的编码输入是正确的,就可以忽略掉 UnsupportedEncodingException
     */
    public static String asUrlParams(Map<String, String> source){
        Iterator<String> it = source.keySet().iterator();
        StringBuilder paramStr = new StringBuilder();
        while (it.hasNext()){
            String key = it.next();
            String value = source.get(key);
            if (StringUtils.isBlank(value)){
                continue;
            }
            try {
                // URL 编码
                value = URLEncoder.encode(value, "utf-8");
            } catch (UnsupportedEncodingException e) {
                // do nothing
            }
            paramStr.append("&").append(key).append("=").append(value);
        }
        // 去掉第一个&
        return paramStr.substring(1);
    }

    public static String appendUrl(String url, String path) {
        if (url == null) {
            return null;
        }
        if (path == null) {
            path = "";
        }
        char splitChar = '/';
        //相当于startsWith
        if (endWithChar(url, splitChar) && startWithChar(path, splitChar)) {
            return url + path.substring(1);
        }
        //相当于startsWith
        else if (endWithChar(url, splitChar) || startWithChar(path, splitChar)) {
            return url + path;
        }
        return url + splitChar + path;
    }

    public static String getBaseUrl(String url) {
        int idx = url.indexOf("://");
        if (idx > 0) {
            int idxEnd = idx + 3;
            String tmp = url.substring(idxEnd);
            idx = tmp.indexOf('/');
            if (idx > 0) {
                tmp = tmp.substring(0, idx);
                idxEnd += tmp.length();
            }
            return url.substring(0, idxEnd);
        }
        return url;
    }

    public static String getRelayUrl(String url, String roomId, String relayNginxUrl){
        String baseUrl=CommUtils.getBaseUrl(url);
        String relPath=url.substring(baseUrl.length());
        relPath=relPath.substring(0, relPath.indexOf(roomId));
        //把下载地址换成relayNginx的网址
        url=url.replace(baseUrl, relayNginxUrl);
        url=url.replace(relPath, "/");
        return url;
    }
}
