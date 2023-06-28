package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Project;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.AddUserProjectRequest;
import com.yupi.usercenter.service.ProjectService;
import com.yupi.usercenter.mapper.ProjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author HWQ
* @description 针对表【project(用户项目表)】的数据库操作Service实现
* @createDate 2023-06-27 19:02:12
*/
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
    implements ProjectService{

    @Override
    public List<Project> getUserProjectInfo(User loginUser) {
        Long userId = loginUser.getId();
        if(userId == null || userId <= 0){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        QueryWrapper<Project> projectQueryWrapper = new QueryWrapper<>();
        projectQueryWrapper.eq("userId",userId);
        List<Project> res = this.list(projectQueryWrapper);
        if(res.size() == 0){
            res = new ArrayList<>();
        }
        return res;
    }

    @Override
    public boolean addUserProject(AddUserProjectRequest addUserProjectRequest, User loginUser) {
        Long userId = loginUser.getId();
        if(userId == null || userId <= 0){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        //校验参数
        String title = addUserProjectRequest.getTitle();
        String description = addUserProjectRequest.getDescription();
        Integer percent = addUserProjectRequest.getPercent();
        String cover = addUserProjectRequest.getCover();
        String href = addUserProjectRequest.getHref();
        if(StringUtils.isAnyBlank(title,description,cover,href)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(percent < 0 || percent > 1000){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //添加数据
        Project project = new Project();
        project.setUserId(userId);
        project.setCover(cover);
        project.setTitle(title);
        project.setDescription(description);
        project.setPercent(percent);
        project.setHref(href);
        boolean res = this.save(project);
        if(!res) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return true;
    }
}




