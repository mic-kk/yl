package com.tcm.user.dto.resp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResp {

    private String token;
    private String tokenName;
    private Long userId;
    private String username;
    private String realName;
}
