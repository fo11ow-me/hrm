package com.qiujie.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.enums.LeaveEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * @Author qiujie
 * @Date 2022/4/5
 * @Version 1.0
 */

@Data
public class StaffLeaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @ApiModelProperty("工号")
    private String code;

    @ApiModelProperty("员工姓名")
    private String name;

    @ApiModelProperty("部门")
    private String deptName;

    @ApiModelProperty("电话")
    private String phone;


    @ApiModelProperty("员工id")
    private Integer staffId;

    @ApiModelProperty("请假的天数")
    private Integer days;

    @ApiModelProperty("请假类型")
    private LeaveEnum typeNum;

    @ApiModelProperty("请假的起始日期")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp createTime;

    @ApiModelProperty("0未审核，1审核通过，2驳回，3撤销")
    private AuditStatusEnum status;

    @ApiModelProperty("审批意见")
    private String auditRemark;

    private String remark;
}
