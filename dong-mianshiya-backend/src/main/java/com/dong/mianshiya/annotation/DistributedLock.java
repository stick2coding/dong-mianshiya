package com.dong.mianshiya.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 自定义注解，做为分布式锁来用
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的key（名称）
     * @return
     */
    String key();

    /**
     * 锁的有效时间
     * @return
     */
    long leaseTime() default 30000;

    /**
     * 等待时间
     * @return
     */
    long waitTime() default 10000;

    /**
     * 时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
