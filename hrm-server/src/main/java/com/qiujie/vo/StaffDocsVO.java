package com.qiujie.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
@Data
public class StaffDocsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @ApiModelProperty("文件名称")
    private String name;

    @ApiModelProperty("员工姓名")
    private String staffName;

    @ApiModelProperty("文件类型")
    private String type;

    @ApiModelProperty("文件原名称")
    private String oldName;

    @ApiModelProperty("文件md5信息")
    private String md5;

    @ApiModelProperty("文件大小kB")
    private Long size;

    @ApiModelProperty("文件上传者id")
    private Integer staffId;

    @ApiModelProperty("备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("修改时间")
    private Timestamp updateTime;

}
