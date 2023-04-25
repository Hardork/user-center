package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @author HWQ
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2023-04-20 21:08:52
*/
public interface UserService extends IService<User> {

    /**
     *  用户注释
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 用户信息
     */
    User doLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user 用户对象
     * @return 脱敏后的用户对象
     */
    User getSafetyUser(User user);

    /**
     * 用户注销
     * @param request
     * @return
     */
    Integer userLogout(HttpServletRequest request);
}
