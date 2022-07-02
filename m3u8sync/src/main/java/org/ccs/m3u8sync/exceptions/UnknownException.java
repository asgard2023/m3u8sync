package org.ccs.m3u8sync.exceptions;


public class UnknownException extends BaseException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public UnknownException(String errorMsg) {
        super(ResultCode.ERROR_UNKNOWN, errorMsg);
    }

    public UnknownException() {
        super(ResultCode.ERROR_UNKNOWN, null);
    }

    public UnknownException(String i18nCode, String lang) {
        super(ResultCode.ERROR_UNKNOWN, null);
    }
}
