package com.tcm.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tcm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台账号（6 端共用，通过角色区分终端权限）。
 */
@Getter
@Setter
@TableName("tcm_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;

    /** BCrypt 哈希，禁止明文 */
    private String password;

    /** 真实姓名/昵称 */
    private String realName;

    /** 角色编码：admin/seller/supplier/buyer_b/buyer_c/pharmacist */
    private String roleCode;

    /** 状态：1 启用 / 0 禁用 */
    private Integer status;
}
