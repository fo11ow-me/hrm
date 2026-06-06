package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;

@Getter
@AllArgsConstructor
public enum GenderEnum implements BaseEnum<T>{

    MALE(0,"男"),
    FEMALE(1,"女");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String message;
}
