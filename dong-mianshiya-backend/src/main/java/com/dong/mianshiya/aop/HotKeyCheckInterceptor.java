package com.dong.mianshiya.aop;

import com.dong.mianshiya.annotation.HotKeyCheck;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HotKeyCheckInterceptor {


    @Around("@annotation(hotKeyCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, HotKeyCheck hotKeyCheck) throws Throwable {
        return joinPoint.proceed();
    }

}
