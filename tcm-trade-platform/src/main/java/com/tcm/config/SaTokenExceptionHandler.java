package com.tcm.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.tcm.common.base.Result;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 鉴权异常处理：未登录 2001、无权限 2002。
 * 异常类型比 common 的兜底处理器更具体，Spring 自动路由到此处。
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问: type={}", e.getType());
        return Result.fail(ErrorCode.NOT_LOGIN);
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(Exception e) {
        log.warn("无权限访问: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_PERMISSION);
    }
}
