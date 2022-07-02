package org.ccs.m3u8sync.exceptions;

public class FileNormalException extends BaseException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public FileNormalException(String errorMsg) {
        super(ResultCode.FILE_NORMAL, errorMsg);
    }

    public FileNormalException() {
        super(ResultCode.FILE_NORMAL, null);
    }

    public FileNormalException(String i18nCode, String lang) {
        super(ResultCode.FILE_NORMAL, null);
    }
}
