package com.dong.mianshiya.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.dong.mianshiya.common.ErrorCode;
import com.dong.mianshiya.exception.BusinessException;
import com.dong.mianshiya.model.entity.User;
import com.dong.mianshiya.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 检测爬虫
 */
@Component
public class CrawlerDetectManager {

    @Resource
    private CounterManager counterManager;

    @Resource
    UserService userService;


    public void crawlerDetect(long loginUserId){
        // 设置告警次数
        final int WARN_COUNT = 5;
        // 设置封号次数
        final int BAN_COUNT = 10;
        // 拼接key 使用user:access:loginUserId
        String key = String.format("user:access:%s", loginUserId);
        // 统计一分钟内访问的次数，并设置过期事件180s
        long count = counterManager.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 180);

        // 业务逻辑
        // 先判断是否封号
        if (count >= BAN_COUNT) {
            // 达到要求先强制下线，这里使用satoken
            StpUtil.logout();
            // 修改数据库进行封号
            User updateUer = new User();
            updateUer.setId(loginUserId);
            updateUer.setUserRole("ban");
            userService.updateById(updateUer);
            // 同时抛出异常返回给前端
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您的账号已被封禁，请联系管理员");
        }

        // 是否告警
        if(count == WARN_COUNT){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "您的账号存在异常行为，请及时联系管理员");
        }
    }
}
