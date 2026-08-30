package com.tcm.user.dto.resp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoResp {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}
