package com.yupi.usercenter.model.domain.request;

import lombok.Data;

/**
 * @Author:HWQ
 * @DateTime:2023/6/28 9:34
 * @Description:
 **/
@Data
public class UpdateMyRequest {
    private String username;
    private String profile;
    private String email;
    private String phone;
}
