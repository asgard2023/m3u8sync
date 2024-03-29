package org.ccs.m3u8sync.exceptions;



import cn.hutool.extra.servlet.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 捕获异常统一处理
 *
 * @version v1.0
 * @author chenjh
 */
@ControllerAdvice
public class M3u8GlobalExceptionHandler {

    static Logger logger = LoggerFactory.getLogger(M3u8GlobalExceptionHandler.class);

    private Map<String, Object> getRequestMap(HttpServletRequest req) {
        Map<String, Object> reqMap = new TreeMap<>();
        if (req == null) {
            return reqMap;
        }

        //获得request 相关信息
        String method = req.getMethod();
        reqMap.put("method", method);

        String noLogStr = "token,";
        Set<String> keys = req.getParameterMap().keySet();
        for (String key : keys) {
            if (noLogStr.contains(key + ",")) {
                continue;
            }
            reqMap.put(key, req.getParameter(key));
        }
        reqMap.remove("password");
        reqMap.put("remoteAddr", ServletUtil.getClientIP(req));
        String requestURI = req.getRequestURI();
        reqMap.put("requestURI", requestURI);
        return reqMap;
    }


    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object handleException(HttpServletRequest request, Exception ex) {
        if(ex==null){
            logger.error("---handleException method={} \n request={}", request.getMethod(), this.getRequestMap(request), ex);
            return null;
        }
        if (ex instanceof HttpRequestMethodNotSupportedException
                || ex instanceof HttpMediaTypeNotSupportedException
                || ex instanceof HttpMediaTypeNotAcceptableException){
            logger.error("---handleException method={} uri={} error={}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
            ResultData resultData = ResultData.error(new FailedException());
            resultData.setErrorMsg(ex.getMessage());
            return resultData;
        }


        String messageError = ex.getMessage();
        logger.error("---handleException method={} error={} \n request={}", request.getMethod(), messageError, this.getRequestMap(request), ex);

        ResultData resultData = ResultData.error(new UnknownException(ex.getMessage()));
        resultData.setErrorType("sys");
        return resultData;
    }

    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResultData handleBaseException(HttpServletRequest request, BaseException e) {
        String logType = getLogExceptionTypeBase();
        Map<String, Object> parameterMap = this.getRequestMap(request);
        if ("full".equals(logType)) {
            logger.warn("----handleBaseException method={} request={}\n error:{}", request.getMethod(), parameterMap, e.getMessage(), e);
        } else {
            logger.warn("----handleBaseException method={} request={}\n error:{}", request.getMethod(), parameterMap, e.getMessage());
        }
        return ResultData.error(e);
    }


    private static String logExceptionTypeBase = "simple";
    private static String getLogExceptionTypeBase() {
        return logExceptionTypeBase;
    }
    public static void setLogExceptionTypeBase(String logExceptionTypeBase) {
        M3u8GlobalExceptionHandler.logExceptionTypeBase=logExceptionTypeBase;
        logger.info("-----setLogExceptionTypeBase--logExceptionTypeBase={}", logExceptionTypeBase);
    }

}