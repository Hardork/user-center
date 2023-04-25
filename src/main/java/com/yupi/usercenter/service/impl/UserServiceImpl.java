package com.yupi.usercenter.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.constant.UserConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.mapper.UserMapper;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author HWQ
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2023-04-20 21:08:52
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    final String SALT = "yupi";
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //非空校验
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword,planetCode)){
            //todo；异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        //校验账户长度
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");
        }
        //校验密码
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //账户不能包含特定字符
        String regEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        if(m.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户不能包含特定字符");
        }
        //密码与校验密码相同
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码与校验密码不同");
        }
        //星球编号长度
        if(planetCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号过长");
        }
        //账户重复
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("userAccount",userAccount);
        long count = userMapper.selectCount(qw);
        if(count > 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户重复");
        }

        //星球编号重复
        qw = new QueryWrapper<>();
        qw.eq("planetCode",planetCode);
        count = userMapper.selectCount(qw);
        if(count > 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号重复");
        }

        //密码加密
        String newPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(newPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if(!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"插入错误");
        }
        return user.getId();
    }

    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.非空校验
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户或密码不能为空");
        }
        //2.校验账户长度
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户长度错误");
        }
        //3.校验密码
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度错误");
        }
        //4.账户不能包含特定字符
        String regEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        if(m.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户不能包含特殊字符");
        }
        //密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        //查询用户是否存在
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("userAccount",userAccount);
        qw.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(qw);
        if(user == null){
            log.info("user login failed, userAccount can not match");
            throw new BusinessException(ErrorCode.PARAMS_NULL_ERROR,"账户不存在");
        }
        //3.用户脱敏
        User safetyUser = getSafetyUser(user);
        //4.记录用户的登录态,用于下一次用户登录，判断是否已经登录过
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);
        //返回信息
        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param user 原来的用户对象
     * @return 经过脱敏处理的用户对象
     */
    public User getSafetyUser(User user){
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户重复");
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setPlanetCode(user.getPlanetCode());
        return safetyUser;
    }

    /**
     * 用户注销
     * @param request 用于获取session
     */
    @Override
    public Integer userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }
}




