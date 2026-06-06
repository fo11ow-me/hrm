package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 
 * </p>
 *
 * @author qiujie
 * @since 2022-02-28
 */
@Data
@TableName("per_menu")
@ApiModel(value = "Menu对象", description = "")
public class Menu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("菜单id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("菜单编码")
    @TableField("code")
    private String code;

    @ApiModelProperty("菜单名称")
    @TableField("name")
    private String name;

    @TableField("icon")
    private String icon;

    @ApiModelProperty("权限标识")
    @TableField("permission")
    private String permission;

    @ApiModelProperty("父菜单id，0代表根菜单")
    @TableField("parent_id")
    private Integer parentId;

    @ApiModelProperty("0一级菜单，1二级菜单，2权限点")
    @TableField("level")
    private Integer level;

    @ApiModelProperty("权限点是否启用，0禁用、1正常，默认1")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

    @TableField(exist = false)
    private List<Menu> children;

}
