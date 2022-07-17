package org.ccs.m3u8sync.exceptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BaseException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String resultCode;
    private final String title = "Error";


    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new ConcurrentHashMap<>();
        ResultCode[] resultCodes = ResultCode.values();
        for (ResultCode resultCode : resultCodes) {
            errorCodeMap.put(resultCode.getCode(), resultCode.getMsg());
        }
    }


    private static String getMsg(String resultCode, String errorMsg) {
        if (errorMsg != null) {
            return errorMsg;
        }
        return errorCodeMap.get(resultCode);
    }

    public BaseException(String resultCode, String errorMsg) {
        super(getMsg(resultCode, errorMsg));
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getTitle() {
        return title;
    }
}
