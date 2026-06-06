package com.qiujie.dto;

import com.qiujie.enums.BaseEnum;
import com.qiujie.enums.BusinessStatusEnum;
import org.apache.poi.ss.formula.functions.T;

/**
 * 请求响应数据
 */
public class Response {

    public static ResponseDTO success(){
        return new ResponseDTO(BusinessStatusEnum.SUCCESS);
    }

    public static ResponseDTO success(String message){
        return new ResponseDTO(BusinessStatusEnum.SUCCESS.getCode(), message);
    }

    public static ResponseDTO success(Object data){
        return new ResponseDTO(BusinessStatusEnum.SUCCESS,data);
    }

    public static ResponseDTO success(Object data,String token){
        return new ResponseDTO(BusinessStatusEnum.SUCCESS,data,token);
    }

    public static ResponseDTO success(String message, Object data){
        return new ResponseDTO(BusinessStatusEnum.SUCCESS.getCode(),message,data);
    }

    public static ResponseDTO error(String message){
        return new ResponseDTO(BusinessStatusEnum.ERROR.getCode(), message);
    }

    public static ResponseDTO error(){
        return new ResponseDTO(BusinessStatusEnum.ERROR);
    }

    public static ResponseDTO error(Integer code, String message){
        return new ResponseDTO(code,message);
    }

    public static ResponseDTO error(BaseEnum<T> e) {
        return new ResponseDTO(e);
    }
}
