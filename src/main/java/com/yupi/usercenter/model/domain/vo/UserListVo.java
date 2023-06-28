package com.yupi.usercenter.model.domain.vo;

import com.yupi.usercenter.model.domain.User;
import lombok.Data;

import java.util.List;

/**
 * @Author:HWQ
 * @DateTime:2023/6/25 17:10
 * @Description:
 **/
@Data
public class UserListVo {
    private long pageNo;
    private long pageSize;
    private long totalCount;
    private long totalPage;
    private List<User> data;
}
