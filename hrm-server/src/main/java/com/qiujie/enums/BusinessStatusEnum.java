package com.qiujie.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BusinessStatusEnum implements BaseEnum<T> {

    SUCCESS(200, "成功"),
    ERROR(300, "失败"),
    STAFF_NOT_EXIST(400,"没有此员工，请重新登录"),
    STAFF_STATUS_ERROR(500, "状态异常，请联系管理员"),
    FILE_NOT_EXIST(600, "文件不存在"),
    FILE_READ_ERROR(700, "文件读取失败"),
    FILE_WRITE_ERROR(800, "文件写入失败"),
    FILE_UPLOAD_ERROR(900, "文件上传失败"),
    DATA_IMPORT_ERROR(1000, "数据导入失败"),
    BATCH_DELETE_ERROR(1100, "批量删除失败"),
    UNAUTHORIZED(1200,"认证失败，请重新登录"),
    FORBIDDEN(1300,"不好意思，你没有此权限");

    private final Integer code;
    private final String message;
}
