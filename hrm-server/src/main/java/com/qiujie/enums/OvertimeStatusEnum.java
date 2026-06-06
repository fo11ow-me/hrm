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
public enum OvertimeStatusEnum implements BaseEnum<T> {

    NORMAL(0, "正常","success"),
    OVERTIME(1, "加班",""),
    TIME_OFF(2, "调休","info");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String message;

    private final String tagType;
}
