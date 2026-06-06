package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * <p>
 * 
 * </p>
 *
 * @author qiujie
 * @since 2022-02-24
 */
@Data
@Accessors(chain = true)
@TableName("sys_docs")
@ApiModel(value = "Docs对象", description = "文件管理")
public class Docs implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("文件名称")
    @TableField("name")
    private String name;

    @ApiModelProperty("文件类型")
    @TableField("type")
    private String type;

    @ApiModelProperty("文件原名称")
    @TableField("old_name")
    private String oldName;

    @ApiModelProperty("文件md5信息")
    @TableField("md5")
    private String md5;

    @ApiModelProperty("文件大小kB")
    @TableField("size")
    private Long size;

    @ApiModelProperty("文件上传者id")
    @TableField("staff_id")
    private Integer staffId;

    @ApiModelProperty("员工备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("修改时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @ApiModelProperty("0未删除，1已删除，默认为0")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
