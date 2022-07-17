package org.ccs.m3u8sync.exceptions;

public enum ResultCode {
    DEFAULT_SUCCESS("100000", "成功"),
    DEFAULT_FAILED("100001", "失败"),
    ERROR_UNKNOWN("100002", "服务器内部错误"),
    DATA_NOT_EXIST("100003", "查询数据不存在"),
    DATA_EXIST("100004", "数据已存在"),

    PARAMS_NULL("100010", "请输入参数"),
    PARAMS_ERROR("100011", "参数错误"),
    DATA_FORMAT_ERROR("100012", "数据格式错误"),
    DECRYPT_ERROR("100013", "参数解密失败"),
    USER_TOKEN_FAILED("100020", "Token验证失败"),
    USER_TOKEN_EXPIRE("101021", "登录状态已经过期"),
    NEED_LOGIN("100022", "需要登入"),
    PERMISSION_DENIED("100023", "没有权限"),

    USER_FREQUENCY_ERROR("100030", "访问频率限制"),
    USER_BLACK_ERROR("100031", "黑名单限制"),
    USER_WHITE_ERROR("100032", "白名单限制"),

    FILE_UNEXIST("100011", "文件不存在"),
    FILE_INVALID("100012", "文件无效"),
    FILE_NORMAL("100013", "文件正常");
    private final String code;
    private final String msg;

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return this.code;
    }

    public String getMsg() {
        return msg;
    }
}