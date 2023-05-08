package com.yupi.usercenter.service;

import com.yupi.usercenter.constant.UserConstant;
import com.yupi.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 根据用户标签搜索用户
     * @param tagList 用户要搜索的标签列表
     * @return
     */
    List<User> searchUserByTags(List<String> tagList);

    /**
     * 更新用户信息
     * @param user
     * @param loginUser
     * @return 是否跟新成功 成功1 失败0
     */
    int  updateUserInfo(User user, User loginUser);

    /**
     * 获取当前登录用户的信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 根据请求鉴权
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 根据用户实体鉴权
     * @param user
     * @return 是否为管理员
     */
    boolean isAdmin(User user);


    /**
     * 根据用户标签给用户推荐其它用户
     * @param num 要推荐的用户个数
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);
}
