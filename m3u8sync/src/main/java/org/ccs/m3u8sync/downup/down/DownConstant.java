package org.ccs.m3u8sync.downup.down;

public class DownConstant {

    //因子
    public static final float FACTOR = 1.15F;

    //默认文件每次读取字节数
    public static final int BYTE_COUNT = 40960;

    //日志级别 控制台不输出
    public static final int NONE = 0X453500;

    //日志级别 控制台输出所有信息
    public static final int INFO = 0X453501;

    //日志级别 控制台输出调试和错误信息
    public static final int DEBUG = 0X453502;

    //日志级别 控制台只输出错误信息
    public static final int ERROR = 0X453503;

    //ts文件后缀
    public static final String TS = ".ts";
    //ts原文件后缀
    public static final String TS_SOURCE = ".ts";

    public static final String M3U8 = ".m3u8";
    public static final String M3U8_FFmpeg = ".text";
    public static final String MP4= ".mp4";
    public static final String KEY = ".key";
}
