package com.qiujie.exception;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.enums.BusinessStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
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

    /**
     * 处理 @PreAuthorize 等注解抛出的 AccessDeniedException，
     * 返回标准 FORBIDDEN 响应，避免被兜底 ExceptionHandler 包装成 "Request failed: Access Denied"
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseDTO handle(AccessDeniedException exception) {
        logger.warn("Access denied: {}", exception.getMessage());
        return Response.error(BusinessStatusEnum.FORBIDDEN.getCode(),
                              BusinessStatusEnum.FORBIDDEN.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseDTO handle(Exception exception) {
        logger.error(exception.getMessage(), exception);
        return Response.error("Request failed: " + exception.getMessage());
    }
}
