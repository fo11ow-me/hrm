package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.enums.GenderEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

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
@ApiModel(value = "Staff对象", description = "")
public class Staff implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("员工id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("工号")
    @ApiModelProperty("员工编码")
    @TableField("code")
    private String code;

    @ExcelProperty("姓名")
    @ApiModelProperty("员工姓名")
    @TableField("name")
    private String name;

    @ApiModelProperty("性别，0男，1女，默认男")
    @TableField("gender")
    private GenderEnum gender;

    @ExcelProperty("地址")
    @ApiModelProperty("员工家庭住址")
    @TableField("address")
    private String address;

    @ApiModelProperty("员工密码")
    @TableField("pwd")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ApiModelProperty("员工头像")
    @TableField("avatar")
    private String avatar;

    @ExcelProperty("生日")
    @ApiModelProperty("员工生日")
    @TableField("birthday")
    private Date birthday;

    @ExcelProperty("电话")
    @ApiModelProperty("员工电话")
    @TableField("phone")
    private String phone;

    @ExcelProperty("备注")
    @ApiModelProperty("员工备注")
    @TableField("remark")
    private String remark;

    @ExcelProperty("部门id")
    @ApiModelProperty("部门id")
    @TableField("dept_id")
    private Integer deptId;

    @ApiModelProperty("员工状态，0离职，1在职，2禁用")
    @TableField("status")
    private Integer status;

    @ExcelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @ExcelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("逻辑删除，0未删除，1删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;
}
