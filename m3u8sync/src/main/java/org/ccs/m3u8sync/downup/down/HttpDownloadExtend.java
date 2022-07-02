package org.ccs.m3u8sync.downup.down;

import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class HttpDownloadExtend extends HttpDownloader {

    /**
     * 下载远程文件数据，支持30x跳转
     *
     * @param url 请求的url
     * @return 文件数据
     */
    public static byte[] downloadBytes(String url, int timeout) {
        return requestDownload(url, timeout).bodyBytes();
    }

    /**
     * 请求下载文件
     *
     * @param url     请求下载文件地址
     * @param timeout 超时时间
     * @return HttpResponse
     * @since 5.4.1
     */
    private static HttpResponse requestDownload(String url, int timeout) {
        Assert.notBlank(url, "[url] is blank !");

        final HttpResponse response = HttpUtil.createGet(url, true)
                .timeout(timeout)
                .executeAsync();

        if (response.isOk()) {
            return response;
        }

        throw new HttpException("Server response error with status code: [{}]", response.getStatus());
    }
}
