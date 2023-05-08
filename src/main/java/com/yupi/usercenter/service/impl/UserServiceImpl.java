package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.constant.UserConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.mapper.UserMapper;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.vo.UserVO;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        safetyUser.setTags(user.getTags());
        safetyUser.setProfile(user.getProfile());
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

    /**
     * 根据用户标签搜索用户
     * @param tagList 用户要搜索的标签列表
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagList){
        if(CollectionUtils.isEmpty(tagList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> qw = new QueryWrapper<>();

        /**
         * sql查询方式
         */
//        for (String tag : tagList) {
//            qw = qw.like("tags",tag);
//        }
//        List<User> userList = userMapper.selectList(qw);
//        //返回脱敏后的用户列表
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
        /**
         * 内存查询方式
         */
        List<User> userList = userMapper.selectList(qw);
        Gson gson = new Gson();
        return userList.stream().filter(user -> {
            //如果用户的标签为空过滤掉
            if(StringUtils.isEmpty(user.getTags())){
                return false;
            }
            String tags = user.getTags();
            //将用户的标签json格式转换为字符串格式
            Set<String> tagNameList = gson.fromJson(tags, new TypeToken<Set<String>>() {}.getType());
            //如果tagNameList为null就自动赋值为空集合
            tagNameList = Optional.ofNullable(tagNameList).orElse(new HashSet<>());
            for (String tagName : tagList) {
                //如果当前用户的标签不在要搜索的标签中就过滤当前用户
                if(!tagNameList.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

    }

    /**
     *
     * @param user 更新后的用户信息
     * @param loginUser 当前登录用户的信息
     * @return
     */
    @Override
    public int updateUserInfo(User user, User loginUser) {
        //判断是否传了id
        long userId = user.getId();
        if(userId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //用户自己，只允许更新自己的信息
        //管理员，可以更新所有用户信息
        if(!isAdmin(loginUser) && loginUser.getId() != userId){//非管理员，并且要更新的用户不是自己
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //符合更新条件
        User oldUser = userMapper.selectById(userId);
        if(oldUser == null){
            throw new BusinessException(ErrorCode.PARAMS_NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if(request == null){
            return null;
        }
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(userObj == null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 鉴权
     * @param request
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request){
        //鉴权，仅管理员可操作
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        //不是管理员
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;//空数组
    }

    @Override
    public boolean isAdmin(User user){
        //鉴权，仅管理员可操作
        //不是管理员
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;//空数组
    }

    //1.查询数据库用户时，只查询部分字段（提升查询性能）
    //2.推荐用户中剔除自己
    //3.过滤掉空标签的用户
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        //1。先查询用户的标签
        String tags = loginUser.getTags();
        //判断当前标签是否存在
        if(tags == null){//当前用户无标签
            return null;
        }
        //2.将用户标签转为字符串列表
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //3.查询用户数据库，取出所有用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id","tags");
        userQueryWrapper.isNotNull("tags");
        List<User> userList = this.list(userQueryWrapper);
        //4.定义存储推荐得分排名的列表 <用户,当前用户得分>
        List<Pair<User, Long>> scoreList = new ArrayList<>();
        //5.遍历所有用户，找出符合条件的用户
        for (int i = 0; i < userList.size(); i++) {
            User curUser = userList.get(i);
            String curUserTags = curUser.getTags();
            //剔除自己以及无标签的用户
            if((long)curUser.getId() == loginUser.getId() || StringUtils.isBlank(curUserTags)){
                continue;
            }
            //将当前用户的tags转换为List格式
            List<String> curUserTagsList = gson.fromJson(curUserTags, new TypeToken<List<String>>() {
            }.getType());
            //计算当前用户的得分
            long score = AlgorithmUtils.compareTags(tagList, curUserTagsList);
            //存储信息到得分列表中
            scoreList.add(new Pair<User,Long>(curUser,score));
        }
        //6.处理得分列表,得分由小到大排序
        List<Pair<User, Long>> topUserPairList = scoreList.stream().
                sorted((a, b) ->
                        (int) (a.getValue() - b.getValue())
                ).limit(num).collect(Collectors.toList());

        //7.将处理得分列表转换为要返回的数据类型,先获取完整的用户实体
        List<Long> userIdList = topUserPairList.stream().map(a -> a.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.in("id",userIdList);
        //用户的Id与用户实体形成map
        Map<Long, List<User>> userIdUserListMap = this.list(qw).stream().map(user -> getSafetyUser(user)).collect(Collectors.groupingBy(User::getId));
        //按照得分排名顺序返回数据
        List<User> finalResult = new ArrayList<>();
        for (Long userId : userIdList) {
            finalResult.add(userIdUserListMap.get(userId).get(0));
        }
        return finalResult;
    }


}




