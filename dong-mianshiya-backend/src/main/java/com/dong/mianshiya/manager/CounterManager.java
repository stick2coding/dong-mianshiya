package com.dong.mianshiya.manager;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.dong.mianshiya.common.ErrorCode;
import com.dong.mianshiya.exception.BusinessException;
import com.dong.mianshiya.model.entity.User;
import com.dong.mianshiya.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 计数器管理
 */
@Slf4j
@Service
public class CounterManager {

    /**
     * 这里是根据名字进行注入
     * autower是根据类型注入
     * 注入redis客户端
     */
    @Resource
    private RedissonClient redisClient;

    @Resource
    UserService userService;


    /**
     * 默认返回1分钟内的访问量
     * @param key
     * @return
     */
    public long incrAndGetCounter(String key){
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES);
    }


    /**
     * 自定义时间
     * @param key
     * @param timeInterval
     * @param unit
     * @return
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit unit){
        // 设置过期实践
        int expireTimeInSeconds;

        switch (unit) {
            case SECONDS:
                expireTimeInSeconds = timeInterval;
                break;
            case MINUTES:
                expireTimeInSeconds = (int) (timeInterval * 60);
                break;
            case HOURS:
                expireTimeInSeconds = (int) (timeInterval * 3600);
                break;
            default:
                throw new IllegalArgumentException("unsupported timeUnit");
        }
        return incrAndGetCounter(key, timeInterval, unit, expireTimeInSeconds);

    }

    /**
     * 增加并返回一定时间内的计数
     * 需要定义热点key、时间（长度和单位）、过期时间（秒）
     * // getEpochSecond 获取秒级时间戳，除以 60 为分钟，除以 3600 为小时
     * long timestampInMinutes = Instant.now().getEpochSecond() / 60;
     * @param key
     * @param timeInterval
     * @param unit
     * @param expireTimeInSeconds
     * @return
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit unit, int expireTimeInSeconds){
        if (StrUtil.isBlank(key)){
            return 0;
        }
        // 根据时间粒度生成key
        long timeFactor;
        switch (unit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 60);
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 3600);
                break;
            default:
                throw new IllegalArgumentException("unsupported timeUnit");
        }

        String redisKey  = StrUtil.format("{}:{}", key, timeFactor);

        // lua脚本
        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "return redis.call('incr', KEYS[1]); " +
                        "else " +
                        "redis.call('set', KEYS[1], 1); " +
                        "redis.call('expire', KEYS[1], ARGV[1]); " +
                        "return 1; " +
                        "end ";

        // 执行脚本
        RScript script = redisClient.getScript(IntegerCodec.INSTANCE);
        Object result = script.eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(redisKey),
                expireTimeInSeconds
        );
        return (long) result;
    }


    public void crawlerDetect(long loginUserId){
        // 设置告警次数
        final int WARN_COUNT = 5;
        // 设置封号次数
        final int BAN_COUNT = 10;
        // 拼接key 使用user:access:loginUserId
        String key = String.format("user:access:%s", loginUserId);
        // 统计一分钟内访问的次数，并设置过期事件180s
        long count = this.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 180);

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
