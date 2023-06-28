package com.yupi.usercenter.controller;

import com.yupi.usercenter.common.BaseResponse;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.common.ResultUtils;
import com.yupi.usercenter.constant.UserConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Project;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.AddUserProjectRequest;
import com.yupi.usercenter.service.ProjectService;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author:HWQ
 * @DateTime:2023/6/27 19:04
 * @Description:
 **/
@RestController
@CrossOrigin(origins = {"http://127.0.0.1:5173","http://127.0.0.1:8000"},methods = {RequestMethod.GET, RequestMethod.POST})
@RequestMapping(value = "/project",method = {RequestMethod.GET, RequestMethod.POST})
@Slf4j
public class ProjectController {

    @Resource
    private ProjectService projectService;
    @Resource
    private UserService userService;
    /**
     * 获取当前用户项目信息
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<Project>> getUserProject(HttpServletRequest request) {
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        List<Project> res = projectService.getUserProjectInfo(loginUser);
        return ResultUtils.success(res);
    }

    /**
     * 用户添加项目信息
     * @param addUserProjectRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Integer> addUserProject(@RequestBody AddUserProjectRequest addUserProjectRequest, HttpServletRequest request) {
        if(addUserProjectRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        boolean res = projectService.addUserProject(addUserProjectRequest, loginUser);
        return ResultUtils.success(1);
    }
}
