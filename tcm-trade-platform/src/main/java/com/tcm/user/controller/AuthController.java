package com.tcm.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.tcm.common.audit.AuditLog;
import com.tcm.common.base.Result;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.dto.resp.UserInfoResp;
import com.tcm.user.entity.SysUser;
import com.tcm.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;

    public AuthController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Operation(summary = "登录")
    @AuditLog(module = "user", action = "login", description = "账号登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(sysUserService.login(req));
    }

    @Operation(summary = "退出登录")
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户信息")
    @SaCheckLogin
    @GetMapping("/info")
    public Result<UserInfoResp> info() {
        SysUser user = sysUserService.getById(StpUtil.getLoginIdAsLong());
        UserInfoResp resp = new UserInfoResp();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoleCode(user.getRoleCode());
        return Result.ok(resp);
    }

    @Operation(summary = "权限注解 demo（需 demo:view 权限）")
    @SaCheckPermission("demo:view")
    @GetMapping("/need-permission")
    public Result<String> needPermission() {
        return Result.ok("你有 demo:view 权限");
    }
}
