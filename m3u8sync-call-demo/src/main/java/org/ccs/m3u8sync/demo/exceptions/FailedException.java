package org.ccs.m3u8sync.demo.exceptions;

public class FailedException extends BaseException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public FailedException(String errorMsg) {
        super(ResultCode.DEFAULT_FAILED_CODE, errorMsg);
    }

    public FailedException() {
        super(ResultCode.DEFAULT_FAILED_CODE, null);
    }

    public FailedException(String i18nCode, String lang) {
        super(ResultCode.DEFAULT_FAILED_CODE, null);
    }
}
