package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING("PENDING"),
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    PARTIAL_SUCCESS("PARTIAL_SUCCESS"),
    FAILED("FAILED");

    @EnumValue
    private final String value;
}
