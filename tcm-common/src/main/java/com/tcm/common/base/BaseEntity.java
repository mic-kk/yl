package com.tcm.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类。
 * 强制约定：所有业务表必须包含 id(雪花)、create_time、update_time、create_by、is_deleted。
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /** 雪花主键，应用层生成，禁止数据库自增 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 逻辑删除：0 未删 / 1 已删，禁止物理删除 */
    @TableLogic
    private Integer isDeleted;
}
