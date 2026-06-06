package com.qiujie.exception;

import com.qiujie.enums.BaseEnum;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;

/**
 * 自定义异常
 * @author
 * @date
 */
@Getter
public class ServiceException extends RuntimeException {
    private final int code;

    public ServiceException(int code,String message){
        super(message);
        this.code = code;
    }

    public ServiceException(BaseEnum<T> e){
        super(e.getMessage());
        this.code = e.getCode();
    }


}
