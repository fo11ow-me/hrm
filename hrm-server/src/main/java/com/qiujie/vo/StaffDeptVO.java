package com.qiujie.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.qiujie.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Date;

/**
 * @Author qiujie
 * @Date 2022/4/9
 * @Version 1.0
 */

@Data
@Accessors(chain = true)
public class StaffDeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "员工id")
    private Integer id;

    @ExcelProperty("工号")
    @Schema(description = "员工编码")
    private String code;

    @ExcelProperty("姓名")
    @Schema(description = "员工姓名")
    private String name;

    @ExcelProperty("年龄")
    @Schema(description = "员工年龄")
    private Integer age;

    @Schema(description = "性别，0男，1女，默认0")
    private GenderEnum gender;

    @ExcelProperty("地址")
    @Schema(description = "员工家庭住址")
    private String address;

    @Schema(description = "部门id")
    private Integer deptId;

    @ExcelProperty("部门")
    @Schema(description = "部门")
    private String deptName;

    @Schema(description = "员工头像")
    private String avatar;

    @ExcelProperty("生日")
    @Schema(description = "员工生日")
    private Date birthday;

    @ExcelProperty("电话")
    @Schema(description = "员工电话")
    private String phone;

    @ExcelProperty("备注")
    @Schema(description = "员工备注")
    private String remark;


    @Schema(description = "员工状态，0异常，1正常")
    private Integer status;
}
