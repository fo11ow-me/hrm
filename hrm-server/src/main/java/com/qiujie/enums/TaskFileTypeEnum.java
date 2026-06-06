package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskFileTypeEnum {

    SOURCE("SOURCE"),
    RESULT("RESULT"),
    ERROR("ERROR");

    @EnumValue
    private final String value;
}
