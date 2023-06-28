package com.yupi.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:HWQ
 * @DateTime:2023/6/26 23:42
 * @Description:
 **/
@Data
public class UpdateUserRequest implements Serializable {
    private static final long serialVersionUID = 7851750037521868036L;
    private Long id;
    private String username;
    private Integer gender;
    private String profile;
    private String phone;
    private String email;
    private Integer userRole;
    private String userAccount;
    private String avatarUrl;
}
