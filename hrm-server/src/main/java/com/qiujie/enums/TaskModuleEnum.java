package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskModuleEnum {

    ATTENDANCE("ATTENDANCE"),
    STAFF("STAFF"),
    DEPT("DEPT"),
    INSURANCE("INSURANCE"),
    SALARY("SALARY"),
    STAFF_LEAVE("STAFF_LEAVE"),
    STAFF_OVERTIME("STAFF_OVERTIME");

    @EnumValue
    private final String value;
}
