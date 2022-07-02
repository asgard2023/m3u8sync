package org.ccs.m3u8sync.exceptions;

public class FileUnexistException extends BaseException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public FileUnexistException(String errorMsg) {
        super(ResultCode.FILE_UNEXIST, errorMsg);
    }

    public FileUnexistException() {
        super(ResultCode.FILE_UNEXIST, null);
    }

    public FileUnexistException(String i18nCode, String lang) {
        super(ResultCode.FILE_UNEXIST, null);
    }
}
