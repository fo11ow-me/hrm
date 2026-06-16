package com.qiujie.vo;

import com.qiujie.annotation.ExcelColumn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @Author qiujie
 * @Date 2022/3/31
 * @Version 1.0
 */

@Data
public class StaffAttendanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "员工id")
    private Integer staffId;

    @Schema(description = "部门id")
    private Integer deptId;

    @ExcelColumn("员工工号")
    @Schema(description = "员工工号")
    private String code;

    @ExcelColumn("员工姓名")
    @Schema(description = "员工姓名")
    private String name;

    @ExcelColumn("电话")
    @Schema(description = "电话")
    private String phone;

    @ExcelColumn("地址")
    @Schema(description = "地址")
    private String address;

    @ExcelColumn("部门")
    @Schema(description = "部门")
    private String deptName;

    @Schema(description = "员工考勤数据")
    private List<HashMap<String,Object>> attendanceList;

}
