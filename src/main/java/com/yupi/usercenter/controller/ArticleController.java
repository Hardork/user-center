package com.yupi.usercenter.controller;

import com.yupi.usercenter.common.BaseResponse;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.common.ResultUtils;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Article;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.AddUserProjectRequest;
import com.yupi.usercenter.model.domain.request.ArticleAddRequest;
import com.yupi.usercenter.service.ArticleService;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author:HWQ
 * @DateTime:2023/6/28 0:35
 * @Description:
 **/
@RestController
@CrossOrigin(origins = {"http://127.0.0.1:5173","http://127.0.0.1:8000"},methods = {RequestMethod.GET, RequestMethod.POST})
@RequestMapping(value = "/article",method = {RequestMethod.GET, RequestMethod.POST})
@Slf4j
public class ArticleController {

    @Resource
    private UserService userService;
    @Resource
    private ArticleService articleService;

    @GetMapping("/search")
    public BaseResponse<List<Article>> getUserArticleList(HttpServletRequest request) {
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        List<Article> res = articleService.getUserArticleList(loginUser);
        return ResultUtils.success(res);
    }

    @PostMapping("/add")
    public BaseResponse<Boolean> addUserArticle(@RequestBody ArticleAddRequest articleAddRequest, HttpServletRequest request) {
        if(articleAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        boolean res = articleService.addUserProject(articleAddRequest, loginUser);
        return ResultUtils.success(res);
    }
}
