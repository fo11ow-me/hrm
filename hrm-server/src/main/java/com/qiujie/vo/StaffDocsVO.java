package com.qiujie.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
@Data
public class StaffDocsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "文件名称")
    private String name;

    @Schema(description = "员工姓名")
    private String staffName;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件原名称")
    private String oldName;

    @Schema(description = "文件md5信息")
    private String md5;

    @Schema(description = "文件大小kB")
    private Long size;

    @Schema(description = "文件上传者id")
    private Integer staffId;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "创建时间")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "修改时间")
    private Timestamp updateTime;

}
