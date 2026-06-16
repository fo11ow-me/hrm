package com.qiujie.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.Accessors;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Docs对象 - 文件管理")
public class Docs implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "文件名称")
    @TableField("name")
    private String name;

    @Schema(description = "文件类型")
    @TableField("type")
    private String type;

    @Schema(description = "文件原名称")
    @TableField("old_name")
    private String oldName;

    @Schema(description = "文件SHA256哈希")
    @TableField("file_hash")
    private String fileHash;

    @Schema(description = "文件原始大小kB")
    @TableField("size")
    private Long size;

    @Schema(description = "磁盘实际占用(字节)")
    @TableField("stored_size")
    private Long storedSize;

    @Schema(description = "是否zstd压缩: 0=否 1=是")
    @TableField("compressed")
    private Integer compressed;

    @Schema(description = "文件上传者id")
    @TableField("staff_id")
    private Integer staffId;

    @Schema(description = "员工备注")
    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "创建时间")
    @TableField("create_time")
    private Timestamp createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Schema(description = "修改时间")
    @TableField("update_time")
    private Timestamp updateTime;

    @Schema(description = "0未删除，1已删除，默认为0")
    @TableField("is_deleted")
    @TableLogic
    private Integer deleteFlag;


}
