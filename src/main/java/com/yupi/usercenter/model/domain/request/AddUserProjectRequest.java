package com.yupi.usercenter.model.domain.request;

import lombok.Data;

/**
 * @Author:HWQ
 * @DateTime:2023/6/27 22:40
 * @Description:
 **/
@Data
public class AddUserProjectRequest {
    private String title;
    private String description;
    private Integer percent;
    private String href;
    private String cover;
}
