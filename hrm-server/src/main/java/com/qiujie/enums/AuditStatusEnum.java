package com.qiujie.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.poi.ss.formula.functions.T;

/**
 * @Author qiujie
 * @Date 2022/4/7
 * @Version 1.0
 */

@Getter
@AllArgsConstructor
public enum AuditStatusEnum implements BaseEnum<T> {

    UNAUDITED(0, "待审核","info"),
    APPROVE(1, "审核通过","success"),
    REJECT(2, "驳回","danger"),
    CANCEL(3, "撤销",""),
    AUDITING(4,"审核中","warning");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String message;

    private final String tagType;
}
