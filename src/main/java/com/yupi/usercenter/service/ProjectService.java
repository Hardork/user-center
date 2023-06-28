package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.Project;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.AddUserProjectRequest;

import java.util.List;

/**
* @author HWQ
* @description 针对表【project(用户项目表)】的数据库操作Service
* @createDate 2023-06-27 19:02:12
*/
public interface ProjectService extends IService<Project> {

    /**
     * 获取用户项目信息列表
     * @param loginUser
     * @return
     */
    List<Project> getUserProjectInfo(User loginUser);

    /**
     * 用户添加项目信息
     *
     * @param addUserProjectRequest
     * @param loginUser
     * @return
     */
    boolean addUserProject(AddUserProjectRequest addUserProjectRequest, User loginUser);
}
