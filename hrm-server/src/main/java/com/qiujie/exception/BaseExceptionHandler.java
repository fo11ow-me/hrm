package com.qiujie.exception;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice   
public class BaseExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ResponseDTO handle(ServiceException exception){
        logger.info(exception.getMessage());
        return Response.error(exception.getCode(),exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseDTO handle(Exception exception) {
        logger.error(exception.getMessage(), exception);
        return Response.error("Request failed: " + exception.getMessage());
    }
}
