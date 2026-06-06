package com.qiujie.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;


@Getter
@AllArgsConstructor
public enum OvertimeEnum implements BaseEnum<T> {

    WORKDAY_OVERTIME(0, "工作日加班",1.5),
    HOLIDAY_OVERTIME(1, "节假日加班",3.0),
    DAY_OFF_OVERTIME(2, "休息日加班",2.0);

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String message;

    // 基本工资倍数下限
    private final Double lowerLimit;
}
