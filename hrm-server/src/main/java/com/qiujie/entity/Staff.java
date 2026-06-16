package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qiujie.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Accessors;
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
 * @since 2022-01-27
 */
@Data
@Accessors(chain = true)
@TableName("sys_staff")
@Schema(description = "Staff对象 - ")
public class Staff implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "员工id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("工号")
    @Schema(description = "员工编码")
    @TableField("code")
    private String code;

    @ExcelProperty("姓名")
    @Schema(description = "员工姓名")
    @TableField("name")
    private String name;

    @Schema(description = "性别，0男，1女，默认男")
    @TableField("gender")
    private GenderEnum gender;

    @ExcelProperty("地址")
    @Schema(description = "员工家庭住址")
    @TableField("address")
    private String address;

    @Schema(description = "员工密码")
    @TableField("pwd")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Schema(description = "员工头像")
    @TableField("avatar")
    private String avatar;

    @ExcelProperty("生日")
    @Schema(description = "员工生日")
    @TableField("birthday")
    private Date birthday;

    @ExcelProperty("电话")
    @Schema(description = "员工电话")
    @TableField("phone")
    private String phone;

    @ExcelProperty("备注")
    @Schema(description = "员工备注")
    @TableField("remark")
    private String remark;

    @ExcelProperty("部门id")
    @Schema(description = "部门id")
    @TableField("dept_id")
    private Integer deptId;

    @Schema(description = "员工状态，0离职，1在职，2禁用")
    @TableField("status")
    private Integer status;

    @ExcelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @ExcelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;
}
