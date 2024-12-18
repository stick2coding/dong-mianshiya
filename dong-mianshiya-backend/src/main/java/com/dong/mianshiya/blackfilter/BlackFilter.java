package com.dong.mianshiya.blackfilter;

import com.dong.mianshiya.utils.NetUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class BlackFilter implements Filter {


    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        // 拿到请求的IP
        String ip = NetUtils.getIpAddress((HttpServletRequest)servletRequest);
        // 黑名单判断
        if(BlackIpUtils.isBlackIp(ip)){
            servletResponse.setContentType("text/json;charset=UTF-8");
            servletResponse.getWriter().write("{\"code\":403,\"data\":\"\",\"message\":\"您的IP已被加入黑名单，请联系管理员\"}");
            return;
        }
        // 继续
        filterChain.doFilter(servletRequest, servletResponse);

    }


}
