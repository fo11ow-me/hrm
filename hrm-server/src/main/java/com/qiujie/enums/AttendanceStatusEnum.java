package com.qiujie.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;

/**
 * @Author qiujie
 * @Date 2022/3/31
 * @Version 1.0
 */

@Getter
@AllArgsConstructor
public enum AttendanceStatusEnum implements BaseEnum<T> {

    NORMAL(0, "正常","success"),
    LATE(1, "迟到",""),
    LEAVE_EARLY(2, "早退","warning"),
    ABSENTEEISM(3, "旷工","danger"),
    LEAVE(4, "休假","info"),
    TIME_OFF(5,"调休","info");


    @EnumValue
    private final Integer code;
    @JsonValue
    private final String message;

    private final String tagType;
}
