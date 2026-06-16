package com.qiujie.dto;

import com.qiujie.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.formula.functions.T;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "数据传输对象 - ")
public class ResponseDTO {
    @Schema(description = "状态码")
    private int code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private Object data;

    @Schema(description = "token")
    private String token;

    public ResponseDTO(int code, String message){
        this.code = code;
        this.message = message;
    }

    public ResponseDTO(int code,String message,Object data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ResponseDTO(BaseEnum<T> e){
        this.code = e.getCode();
        this.message = e.getMessage();
    }

    public ResponseDTO(BaseEnum<T> e, Object data){
        this.code = e.getCode();
        this.message = e.getMessage();
        this.data = data;
    }

    public ResponseDTO(BaseEnum<T> e, Object data, String token){
        this.code = e.getCode();
        this.message = e.getMessage();
        this.data = data;
        this.token = token;
    }

}
