package com.dong.mianshiya.aop;

import com.dong.mianshiya.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class DistributedLockAspect {

    @Resource
    private RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 拿到注解中的参数
        String key = distributedLock.key();
        long leaseTime = distributedLock.leaseTime();
        long waitTime = distributedLock.waitTime();
        TimeUnit timeUnit = distributedLock.timeUnit();

        // 创建一把锁
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        // 尝试获取锁
        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired){
                // 获取成功，执行业务
                return joinPoint.proceed();
            } else {
                // 获取失败，抛出异常
                throw new RuntimeException("获取锁失败" + key);
            }
        }catch (Throwable t){
            throw new Exception(t);
        }finally {
            if (acquired){
                // 释放锁
                lock.unlock();
            }
        }
    }

}
