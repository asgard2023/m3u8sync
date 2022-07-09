package org.ccs.m3u8sync.exceptions;

public class FileUnexistException extends BaseException {
    private static final long serialVersionUID = 1L;
    private static final ResultCode resultCode = ResultCode.FILE_UNEXIST;

    public FileUnexistException(String errorMsg) {
        super(resultCode.getCode(), errorMsg);
    }

    public FileUnexistException() {
        super(resultCode.getCode(), resultCode.getMsg());
    }
}
