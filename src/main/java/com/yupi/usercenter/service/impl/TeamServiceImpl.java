package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.constant.TeamConstant;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.domain.dto.TeamQuery;
import com.yupi.usercenter.model.domain.enums.TeamStatusEnum;
import com.yupi.usercenter.model.domain.request.TeamJoinRequest;
import com.yupi.usercenter.model.domain.request.TeamQuitRequest;
import com.yupi.usercenter.model.domain.request.TeamUpdateRequest;
import com.yupi.usercenter.model.domain.vo.TeamUserVO;
import com.yupi.usercenter.model.domain.vo.UserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.mapper.TeamMapper;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author HWQ
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2023-05-03 17:17:43
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //   2. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        // 7. 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }


    /**
     * 2、查询队伍列表
     * 分页展示队伍列表，根据名称、最大人数等搜索队伍  P0，信息流中不展示已过期的队伍
     * 1. 从请求参数中取出队伍名称等查询条件，如果存在则作为查询条件
     * 2. 不展示已过期的队伍（根据过期时间筛选）
     * 3. 可以通过某个**关键词**同时对名称和描述查询
     * 4. **只有管理员才能查看加密还有非公开的房间**
     * 5. 关联查询已加入队伍的用户信息
     * 6. **关联查询已加入队伍的用户信息（可能会很耗费性能，建议大家用自己写 SQL 的方式实现）**
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            //不是查询所有状态的队伍
            if(status == null || status != TeamConstant.ALL){
                TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
                if (statusEnum == null) {
                    statusEnum = TeamStatusEnum.PUBLIC;
                }
                if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                    throw new BusinessException(ErrorCode.NO_AUTH);
                }
                queryWrapper.eq("status", statusEnum.getValue());
            }
        }
        // 不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 关联查询创建人的用户信息
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //查询已加入该队伍的人数
            QueryWrapper<UserTeam> qw = new QueryWrapper<>();
            qw.eq("teamId",team.getId());
            int hasJoinNum = (int)userTeamService.count(qw);
            teamUserVO.setHasJoinNum(hasJoinNum);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * ##### 3. 修改队伍信息
     * 1. 判断请求参数是否为空
     * 2. 查询队伍是否存在
     * 3. 只有管理员或者队伍的创建者可以修改
     * 4. 如果用户传入的新值和老值一致，就不用 update 了（可自行实现，降低数据库使用次数）
     * 5. **如果队伍状态改为加密，必须要有密码**
     * 6. 更新成功
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询要更新的队伍是否存在
        Long id = teamUpdateRequest.getId();
        if(id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //todo:判断是否有修改的必要
        //判断是否要加密
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if(teamStatusEnum.equals(TeamStatusEnum.SECRET) && StringUtils.isBlank(teamUpdateRequest.getPassword())){//加密,必须有密码
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密房间必须要设置密码");
        }
        //获取要修改队伍的对象
        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.eq("teamId",teamUpdateRequest.getId());
        UserTeam userTeam = userTeamService.getOne(qw);
        if(userTeam == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断权限
        if(userService.isAdmin(loginUser) || (long)userTeam.getUserId() == (long)loginUser.getId()){
            //可以修改
            Team updateTeam = new Team();
            BeanUtils.copyProperties(teamUpdateRequest,updateTeam);
            boolean result = this.updateById(updateTeam);
            return result;
        }else{
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
    }

    /**
     * 加入队伍
     * 其他人、未满、未过期，允许加入多个队伍，但是要有个上限  P0
     * 1. 用户最多加入 5 个队伍
     * 2. 队伍必须存在，只能加入未满、未过期的队伍
     * 3. 不能加入自己的队伍，不能重复加入已加入的队伍（幂等性）
     * 4. 禁止加入私有的队伍
     * 5. 如果加入的队伍是加密的，必须密码匹配才可以
     * 6. 新增队伍 - 用户关联信息
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求加入队伍不存在");
        }
        Team team = this.getById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        //todo:加入队伍需要加锁，防止一个用户连续加入同一个队伍
        //获取锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            //只允许一个服务器抢到锁，其它没抢到的不执行
            if(lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){//抢到锁了,执行业务
                System.out.println("getLock: " + Thread.currentThread().getId());
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("userId", userId);
                long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                if (hasJoinNum > 5) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                }
                // 不能重复加入已加入的队伍
                userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("userId", userId);
                userTeamQueryWrapper.eq("teamId", teamId);
                long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                if (hasUserJoinTeam > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                }
                // 已加入队伍的人数
                long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                if (teamHasJoinNum >= team.getMaxNum()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                }
                // 修改队伍信息
                UserTeam userTeam = new UserTeam();
                userTeam.setUserId(userId);
                userTeam.setTeamId(teamId);
                userTeam.setJoinTime(new Date());
                return userTeamService.save(userTeam);
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        }finally {
            //只释放自己的锁
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
                System.out.println("unLock: " + Thread.currentThread().getId());
            }
        }
        return false;
    }

    /**
     * 用户退出队伍
     * 请求参数：队伍 id
     * 1. 校验请求参数
     * 2. 校验队伍是否存在
     * 3. 校验我是否已加入队伍
     * 4. 如果队伍
     *    1. 只剩一人，队伍解散
     *    2. 还有其他人
     * 1. 如果是队长退出队伍，权限转移给第二早加入的用户 —— 先来后到
     *    只用取 id 最小的 2 条数据
     *2. 非队长，自己退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍Id错误");
        }
        Team team = this.getTeamById(teamId);
        //判断我是否在队伍中
        QueryWrapper<UserTeam> qw = new QueryWrapper<>();
        qw.eq("teamId",teamId);
        qw.eq("userId",loginUser.getId());
        UserTeam userTeam = userTeamService.getOne(qw);
        if(userTeam == null){//说明不在队伍中
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不在队伍中");
        }
        //判断当前队伍的人数
        long usersInTeam = countTeamUserByTeamId(teamId);
        //只剩一人，队伍解散
        if(usersInTeam == 1){
            this.removeById(teamId);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId",teamId);
            return userTeamService.remove(userTeamQueryWrapper);
        }else{//还有其他人,如果是队长退出，就把队长传给队长之后加入的第一个用户
            if((long)userTeam.getUserId() == loginUser.getId()){
                //找出第二个用户
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId",teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");//升序排序拿出前两条就是当前队伍加入最先的两个用户
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                //校验
                if(CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                //拿到要上位的队长id
                UserTeam nextUserTeam = userTeamList.get(1);
                //更新队伍信息
                Long userId = nextUserTeam.getUserId();
                Team newTeam = new Team();
                newTeam.setId(teamId);
                newTeam.setUserId(userId);
                boolean result = this.updateById(newTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队长失败");
                }
            }
        }
        //移除关系
        return userTeamService.remove(qw);
    }


    /**
     * 解散队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long teamId, User loginUser) {
        //校验
        if(teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断当前要删除的队伍是否存在
        Team team = this.getTeamById(teamId);
        //判断当前用户是否是队长，只有队长有权限解散队伍
        if((long)team.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        //清空用户-队伍表
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        boolean remove = userTeamService.remove(userTeamQueryWrapper);
        if(!remove){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

}




