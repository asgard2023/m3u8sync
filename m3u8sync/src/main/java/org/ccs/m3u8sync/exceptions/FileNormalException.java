package org.ccs.m3u8sync.exceptions;

public class FileNormalException extends BaseException {

    private static final ResultCode resultCode = ResultCode.FILE_NORMAL;
    private static final long serialVersionUID = 1L;

    public FileNormalException(String errorMsg) {
        super(resultCode.getCode(), errorMsg);
    }

    public FileNormalException() {
        super(resultCode.getCode(), resultCode.getMsg());
    }
}
