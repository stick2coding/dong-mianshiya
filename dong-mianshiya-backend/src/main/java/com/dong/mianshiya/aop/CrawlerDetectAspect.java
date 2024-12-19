package com.dong.mianshiya.aop;

import com.dong.mianshiya.annotation.CrawlerDetect;
import com.dong.mianshiya.manager.CrawlerDetectManager;
import com.dong.mianshiya.model.entity.User;
import com.dong.mianshiya.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class CrawlerDetectAspect {

    @Resource
    private UserService userService;

    @Resource
    CrawlerDetectManager crawlerDetectManager;


    @Around("@annotation(crawlerDetect)")
    public Object around(ProceedingJoinPoint joinPoint, CrawlerDetect crawlerDetect) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 爬虫检测
        crawlerDetectManager.crawlerDetect(loginUser.getId());

        // 通过权限校验，放行
        return joinPoint.proceed();
    }

}
