
package org.ccs.m3u8sync.demo.exceptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BaseException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String resultCode;
    private String title = "Error";

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new ConcurrentHashMap<>();
        errorCodeMap.put(ResultCode.DATA_NOT_EXIST, ResultCode.DATA_NOT_EXIST_MSG);
        errorCodeMap.put(ResultCode.DEFAULT_FAILED_CODE, ResultCode.DEFAULT_FAILED_CODE_MSG);
        errorCodeMap.put(ResultCode.PERMISSION_DENIED, ResultCode.PERMISSION_DENIED_MSG);
        errorCodeMap.put(ResultCode.PARAMS_NULL, ResultCode.PARAMS_NULL_MSG);
        errorCodeMap.put(ResultCode.PARAMS_ERROR, ResultCode.PARAMS_ERROR_MSG);
        errorCodeMap.put(ResultCode.USER_TOKEN_FAILED, ResultCode.USER_TOKEN_FAILED_MSG);
    }


    private static String getMsg(String resultCode, String errorMsg, String lang) {
        return errorMsg;
    }

    public BaseException(String resultCode, String errorMsg) {
        super(getMsg(resultCode, errorMsg, null));
        this.resultCode = resultCode;
    }

    public BaseException(String resultCode, String errorMsg, String lang) {
        super(getMsg(resultCode, errorMsg, lang));
        this.resultCode = resultCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
