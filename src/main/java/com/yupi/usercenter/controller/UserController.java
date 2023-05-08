package com.yupi.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercenter.common.BaseResponse;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.common.ResultUtils;
import com.yupi.usercenter.constant.UserConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.UserLoginRequest;
import com.yupi.usercenter.model.domain.request.UserRegisterRequest;
import com.yupi.usercenter.model.domain.vo.UserVO;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: HWQ
 * @DateTime:2023/4/21 14:13
 * @Description: 控制层接口
 **/
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://127.0.0.1:5173","http://127.0.0.1:8000"},methods = {RequestMethod.GET, RequestMethod.POST})
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    /**
     * 注册用户接口
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误");
        }
        //获取请求题信息
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            //todo；异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"有参数为空");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        //返回请求结果
        return ResultUtils.success(result);
    }

    /**
     * 登录接口
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userPassword = userLoginRequest.getUserPassword();
        String userAccount = userLoginRequest.getUserAccount();
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            //todo；异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.doLogin(userAccount, userPassword, request);
        //返回请求结果
        return ResultUtils.success(user);
    }

    /**
     * 注销接口
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer flag = userService.userLogout(request);
        return ResultUtils.success(flag);
    }

    /**
     * 获取用户列表
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        long userId = currentUser.getId();
        //todo 校验用户是否合法
        User user = userService.getById(userId);
        return ResultUtils.success(userService.getSafetyUser(user));
    }

    /**
     * 搜索用户接口
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUser(String username, HttpServletRequest request){
        //鉴权，仅管理员可查询
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> qw = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){//username不为空
            qw.like("username",username);
        }
        List<User> userList = userService.list(qw);
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return user;
        }).collect(Collectors.toList());
        // 返回经过处理(将密码隐藏)的List
        return ResultUtils.success(list);
    }

    /**
     * 根据标签搜索用户
     * @param tagNameList 用户搜索标签列表
     * @return 符合标签列表的用户
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    @GetMapping("/recommend")
     public BaseResponse<Page<User>> recommendUsers(HttpServletRequest request, long pageNum, long pageSize){
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前用户的信息
        Long id = userService.getLoginUser(request).getId();
        //查缓存
        String redisKey = String.format("yupao:user:recommed:%s",id);
        ValueOperations ops = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>)ops.get(redisKey);
        if(userPage != null){//缓存中有数据
            return ResultUtils.success(userPage);
        }
        //缓存中没数据，从数据库中获取，并写入到缓存中
        QueryWrapper<User> qw = new QueryWrapper<>();
        Page<User> userList = userService.page(new Page<>(pageNum,pageSize),qw);
        try {
            ops.set(redisKey,userList,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("写缓存错误");
        }
        return ResultUtils.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUserInfo(@RequestBody User user, HttpServletRequest request){
        //1.校验参数是否为空
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断要更新的id是否为空

        //2.校验权限
        User loginUser = userService.getLoginUser(request);

        //3，触发更新
        Integer result = userService.updateUserInfo(user, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 删除用户接口
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request){
        //鉴权，仅管理员可删除
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_NULL_ERROR);
        }
        //这里的删除是逻辑删除
        return ResultUtils.success(userService.removeById(id));
    }

    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request){
        if(request == null || num < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userService.matchUsers(num, loginUser);
        return ResultUtils.success(userList);

    }
    }
