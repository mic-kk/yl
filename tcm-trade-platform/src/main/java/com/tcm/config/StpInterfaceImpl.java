package com.tcm.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tcm.user.entity.SysUser;
import com.tcm.user.service.SysUserService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 权限数据源：从账号角色映射权限码。
 * 说明：脚手架为最小演示实现；后续 M1 按 6 端角色权限表完善。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService sysUserService;

    public StpInterfaceImpl(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SysUser user = sysUserService.getById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        if ("admin".equals(user.getRoleCode())) {
            return List.of("*");
        }
        if ("seller".equals(user.getRoleCode())) {
            return List.of("demo:view");
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = sysUserService.getById(Long.valueOf(loginId.toString()));
        return user == null ? Collections.emptyList() : List.of(user.getRoleCode());
    }
}
