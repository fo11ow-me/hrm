package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.AuditStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.LeaveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>
 *
 * </p>
 *
 * @author qiujie
 * @since 2022-04-05
 */
@Getter
@Setter
@TableName("att_staff_leave")
@Schema(description = "StaffLeave对象 - ")
public class StaffLeave implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "员工id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "请假的天数")
    @TableField("days")
    private Integer days;

    @TableField("type_num")
    private LeaveEnum typeNum;

    @Schema(description = "请假的起始日期")
    @TableField("start_date")
    private Date startDate;

    @Schema(description = "0待审核，1审核通过，2驳回，3撤销，4审核中")
    @TableField("status")
    private AuditStatusEnum status;

    @Schema(description = "审批意见")
    @TableField("audit_remark")
    private String auditRemark;

    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
