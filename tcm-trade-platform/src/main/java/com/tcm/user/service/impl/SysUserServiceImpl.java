package com.tcm.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.entity.SysUser;
import com.tcm.user.enums.UserStatusEnum;
import com.tcm.user.mapper.SysUserMapper;
import com.tcm.user.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResp login(LoginReq req) {
        SysUser user = getByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 统一提示，不泄露账号是否存在
            throw new BusinessException(ErrorCode.LOGIN_ERROR);
        }
        if (UserStatusEnum.DISABLED.getCode() == user.getStatus()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        StpUtil.login(user.getId());
        LoginResp resp = new LoginResp();
        resp.setToken(StpUtil.getTokenValue());
        resp.setTokenName(StpUtil.getTokenName());
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        return resp;
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    }
}
