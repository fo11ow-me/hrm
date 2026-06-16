package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Menu对象 - ")
public class Menu implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "菜单编码")
    @TableField("code")
    private String code;

    @Schema(description = "菜单名称")
    @TableField("name")
    private String name;

    @TableField("icon")
    private String icon;

    @Schema(description = "权限标识")
    @TableField("permission")
    private String permission;

    @Schema(description = "父菜单id，0代表根菜单")
    @TableField("parent_id")
    private Integer parentId;

    @Schema(description = "0一级菜单，1二级菜单，2权限点")
    @TableField("level")
    private Integer level;

    @Schema(description = "权限点是否启用，0禁用、1正常，默认1")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "更新时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;

    @TableField(exist = false)
    private List<Menu> children;

}
