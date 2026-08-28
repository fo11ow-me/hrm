package com.qiujie.overtime;

import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;

import java.math.BigDecimal;

/**
 * 加班计算输出值对象——门面唯一的返回类型。
 *
 * @param typeNum       加班类型（信任输入或按日期分类得出）
 * @param status        OVERTIME / TIME_OFF（调休）
 * @param totalOvertime 加班时长（小时，非空）
 * @param overtimeSalary 加班工资；调休、未达计薪门槛或缺少参考数据时为 {@code null}
 */
public record OvertimeResult(OvertimeEnum typeNum, OvertimeStatusEnum status,
                             BigDecimal totalOvertime, BigDecimal overtimeSalary) {
}