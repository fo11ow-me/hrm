package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskTypeEnum {

    IMPORT("IMPORT"),
    EXPORT("EXPORT");

    @EnumValue
    private final String value;
}
