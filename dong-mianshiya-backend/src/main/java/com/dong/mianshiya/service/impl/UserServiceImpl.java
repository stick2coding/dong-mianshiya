package com.dong.mianshiya.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.mianshiya.common.ErrorCode;
import com.dong.mianshiya.constant.CommonConstant;
import com.dong.mianshiya.constant.RedisConstant;
import com.dong.mianshiya.exception.BusinessException;
import com.dong.mianshiya.mapper.UserMapper;
import com.dong.mianshiya.model.dto.user.UserQueryRequest;
import com.dong.mianshiya.model.entity.User;
import com.dong.mianshiya.model.enums.UserRoleEnum;
import com.dong.mianshiya.model.vo.LoginUserVO;
import com.dong.mianshiya.model.vo.UserVO;
import com.dong.mianshiya.satoken.SaDeviceUtils;
import com.dong.mianshiya.service.UserService;
import com.dong.mianshiya.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.dong.mianshiya.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author sunbin
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yupi";
    private final RedissonClient redissonClient;

    public UserServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        // 这里修改使用satoken，先登录并指定设备，然后将用户信息放入session中
        StpUtil.login(user.getId(), SaDeviceUtils.getDeviceByRequest(request));
        StpUtil.getSession().set(USER_LOGIN_STATE, user);
//        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        // 这里改用satoken
        // 先拿到用户id
        Object userId = StpUtil.getLoginId();
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        User currentUser = this.getById((String) userId);
        System.out.println("database currentUser" + currentUser);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 如果用户信息不多，可以直接从session中获取
//        return (User)StpUtil.getSessionByLoginId(userId).get(USER_LOGIN_STATE);
        return currentUser;
    }

    /**
     * 旧方法
     * @param request
     * @return
     */
    public User getLoginUserV1(HttpServletRequest request) {
        // 先判断是否已登录
        // 这里改用satoken
        // 先拿到用户id
        Object userObj = StpUtil.getLoginId();
        if (userObj == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        System.out.println("session currentUser" + currentUser);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        System.out.println("database currentUser" + currentUser);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录（改用satoken，更换原有从servlet中获取session的代码）
        Object userObj = StpUtil.getSession().get(USER_LOGIN_STATE);
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询（改用satoken，更换原有从servlet中获取session的代码）
        Object userObj = StpUtil.getSession().get(USER_LOGIN_STATE);
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //（改用satoken，更换原有从servlet中获取session的代码）

        // 先判断是否登录
        StpUtil.checkLogin();

        // 再移出
        StpUtil.logout();

        return true;

//        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
//        }
//        // 移除登录态
//        request.getSession().removeAttribute(USER_LOGIN_STATE);
//        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public boolean addUserSignIn(long userId) {
        //获取当前时间
        LocalDate now = LocalDate.now();
        String key = RedisConstant.getUserSignInKey(userId, now.getYear());
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        //获取当前日期是一年的第几天
        int offset = now.getDayOfYear();
        //先检测是否已经签到
        if(signInBitSet.get(offset)){
           return true;
        }
        boolean result = signInBitSet.set(offset, true);
        return result;
    }

    @Override
    public Map<LocalDate, Boolean> getUserSignInRecord(long userId, Integer year) {
        if (year == null){
            LocalDate now = LocalDate.now();
            year = now.getYear();
        }
        String key = RedisConstant.getUserSignInKey(userId, year);
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        //定义一个map，组装前端可用的数据（这里用链表，是为了按照可以按照时间顺序逐个放入签到）
        Map<LocalDate, Boolean> signInRecordMap = new LinkedHashMap<>();
        signInRecordMap = getRecordsV1(signInBitSet, year);

        return signInRecordMap;
    }

    /**
     * 快速获取签到记录
     * @param userId
     * @param year
     * @return
     */
    @Override
    public List<Integer> getUserSignInRecordFast(long userId, Integer year) {
        if (year == null){
            LocalDate now = LocalDate.now();
            year = now.getYear();
        }
        String key = RedisConstant.getUserSignInKey(userId, year);
        RBitSet signInBitSet = redissonClient.getBitSet(key);

        return getRecordsV3(signInBitSet, year);
    }

    private Map<LocalDate, Boolean> getRecords(RBitSet signInBitSet, Integer year) {
        Map<LocalDate, Boolean> signInRecordMap = new LinkedHashMap<>();
        // 获取当前年的总天数
        int totalDays = Year.of(year).length();
        // 循环依次获取每天的签到状态
        for (int dayIndex = 1; dayIndex <= totalDays; dayIndex++) {
            System.out.println(dayIndex);
            // 获取dayIndex所在的时间
            LocalDate currentDate = LocalDate.ofYearDay(year, dayIndex);
            // 获取当天是否有签到
            boolean isSignedIn = signInBitSet.get(dayIndex);
            signInRecordMap.put(currentDate, isSignedIn);
        }
        return signInRecordMap;
    }

    /**
     * 将 signInBitSet 的数据在本地缓存一份，避免在循环中每次都再向redis发请求
     * @param signInBitSet
     * @param year
     * @return
     */
    private Map<LocalDate, Boolean> getRecordsV1(RBitSet signInBitSet, Integer year) {
        Map<LocalDate, Boolean> signInRecordMap = new LinkedHashMap<>();
         BitSet signInLocalBitSet = signInBitSet.asBitSet();
        // 获取当前年的总天数
        int totalDays = Year.of(year).length();
        // 循环依次获取每天的签到状态
        for (int dayIndex = 1; dayIndex <= totalDays; dayIndex++) {
            // 获取dayIndex所在的时间
            LocalDate currentDate = LocalDate.ofYearDay(year, dayIndex);
            // 获取当天是否有签到
            boolean isSignedIn = signInLocalBitSet.get(dayIndex);
            signInRecordMap.put(currentDate, isSignedIn);
        }
        return signInRecordMap;
    }

    /**
     * 在返回的数据中，可以只返回有签到的数据，减少数据传输
     * @param signInBitSet
     * @param year
     * @return
     */
    private Map<LocalDate, Boolean> getRecordsV2(RBitSet signInBitSet, Integer year) {
        Map<LocalDate, Boolean> signInRecordMap = new LinkedHashMap<>();
        BitSet signInLocalBitSet = signInBitSet.asBitSet();
        // 获取当前年的总天数
        int totalDays = Year.of(year).length();
        // 循环依次获取每天的签到状态
        for (int dayIndex = 1; dayIndex <= totalDays; dayIndex++) {
            // 获取dayIndex所在的时间
            LocalDate currentDate = LocalDate.ofYearDay(year, dayIndex);
            // 获取当天是否有签到
            boolean isSignedIn = signInLocalBitSet.get(dayIndex);
            if (isSignedIn){
                signInRecordMap.put(currentDate, isSignedIn);
            }
        }
        return signInRecordMap;
    }

    /**
     * 优化循环
     * bitmap为我们提供了更为便捷的查找方法
     * nextSetBit(fromIndex),意思是从fromIndex开始，找到第一个为1的位置，返回这个位置，如果没有找到，返回-1
     * nextClearBit(fromIndex),意思是从fromIndex开始，找到第一个为0的位置，返回这个位置，如果没有找到，返回-1
     * @param signInBitSet
     * @param year
     * @return
     */
    private List<Integer> getRecordsV3(RBitSet signInBitSet, Integer year) {
        List<Integer> signInDayList = new ArrayList<>();
        BitSet signInLocalBitSet = signInBitSet.asBitSet();
        // 先从0开始找到第一天签到的地方
        int index = signInLocalBitSet.nextSetBit(0);
        // 当index >= 0 时，说明有签到，将签到的日期添加到list，并继续查找下一个签到日期
        while (index >= 0){
            signInDayList.add(index);
            index = signInLocalBitSet.nextSetBit(index + 1);
        }

        return signInDayList;
    }




}
