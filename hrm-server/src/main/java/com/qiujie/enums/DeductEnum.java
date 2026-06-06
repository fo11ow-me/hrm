package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;

@Getter
@AllArgsConstructor
public enum DeductEnum implements BaseEnum<T>{
    LATE_DEDUCT(0, "迟到扣款",50),
    LEAVE_EARLY_DEDUCT(1, "早退扣款",50),
    ABSENTEEISM_DEDUCT(2, "旷工扣款",100),
    LEAVE_DEDUCT(3, "休假扣款",80);


    @EnumValue
    private final Integer code;
    @JsonValue
    private final String message;

    // 默认的扣款金额
    private final Integer defaultValue;
}
