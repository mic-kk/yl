package com.tcm.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    /** 登录：校验账号密码，签发 Sa-Token，返回 token 信息 */
    LoginResp login(LoginReq req);

    /** 按用户名查询（未删除） */
    SysUser getByUsername(String username);
}
