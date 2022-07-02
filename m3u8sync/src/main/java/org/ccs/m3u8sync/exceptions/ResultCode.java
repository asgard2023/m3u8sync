package org.ccs.m3u8sync.exceptions;

public class ResultCode {
    private ResultCode() {

    }

    /**
     * 100-000<= 通用 <100-999
     */
    //
    public static final String POP_ERROR_CODE = "100021";

    public static final String POP_ERROR_AUTO_CLOSE_CODE = "100022";

    public static final String DEFAULT_SUCCESS_CODE = "100000";
    public static final String DEFAULT_SUCCESS_CODE_MSG = "成功";

    public static final String DEFAULT_FAILED_CODE = "100001";
    public static final String DEFAULT_FAILED_CODE_MSG = "失败";

    public static final String PARAMS_NULL = "100002";
    public static final String PARAMS_NULL_MSG = "请输入参数";

    public static final String ERROR_UNKNOWN = "100003";
    public static final String ERROR_UNKNOWN_MSG = "服务器内部错误";

    public static final String ENCRYPY_FAILED = "100004";
    public static final String ENCRYPY_FAILED_MSG = "参数加密失败";

    public static final String DECRYPT_FAILED = "100005";
    public static final String DECRYPT_FAILED_MSG = "参数解密失败";

    public static final String DATA_NOT_EXIST = "100006";
    public static final String DATA_NOT_EXIST_MSG = "查询数据不存在";

    public static final String DATA_FORMAT_ERROR = "100007";
    public static final String DATA_FORMAT_ERROR_MSG = "数据格式不对";

    public static final String DATA_EXIST = "100008";
    public static final String DATA_EXIST_MSG = "数据已存在";

    public static final String PARAMS_ERROR = "100009";
    public static final String PARAMS_ERROR_MSG = "参数错误";

    public static final String USER_FREQUENCY_ERROR = "100010";
    public static final String USER_FREQUENCY_ERROR_MSG = "访问频率限制";

    public static final String FILE_UNEXIST = "100011";
    public static final String FILE_UNEXIST_MSG = "File not exist";

    public static final String FILE_INVALID = "100012";
    public static final String FILE_INVALID_MSG = "File not exist";

    public static final String FILE_NORMAL = "100013";
    public static final String FILE_NORMAL_MSG = "File is normal";



    /**
     * 此回放无效
     */
    public static final String DATA_AVLIVE_HIS_VOID = "100014";
    public static final String DATA_AUDIT_ERROR_MSG = "该数据已被审核";
    public static final String DATA_AVLIVE_HIS_VOID_MSG = "此回放无效";


    /**
     * 内容违规
     */
    public static final String CONTENT_LEVEL_VIOLATION = "100017";

    /**
     * 101000<= 用户相关 <101999
     */
    public static final String USER_TOKEN_FAILED = "101000";
    public static final String USER_TOKEN_FAILED_MSG = "Token验证失败";

    public static final String PERMISSION_DENIED = "101023";
    public static final String PERMISSION_DENIED_MSG = "权限错误";
}

