package com.dong.mianshiya.satoken;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.dong.mianshiya.common.ErrorCode;
import com.dong.mianshiya.exception.ThrowUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 设备工具类
 * 从客户端的请求头中获取设备信息
 */
public class SaDeviceUtils {

    public static String getDeviceByRequest(HttpServletRequest request){
        // 拿到 请求头
        String userAgentStr = request.getHeader(Header.USER_AGENT.toString());
        // 解析数据，这里可以用hutool工具类
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        // 判断是否为null
        ThrowUtils.throwIf(userAgent == null, ErrorCode.SYSTEM_ERROR);
        // 默认登录设备为PC
        String device = "PC";
        // 判断是否为小程序或者app或者pad
        if (userAgent.isMobile()) {
            device = "mobile";
        } else if (isPad(userAgentStr)) {
            device = "pad";
        } else if (isMiniProgram(userAgentStr)) {
            device = "miniProgram";
        }
        return device;
    }

    /**
     * 如何判断是否是小程序呢
     *
     * @param userAgentStr
     * @return
     */
    private static boolean isMiniProgram(String userAgentStr) {
        //一般是通过是否携带 MicroMessage来判断 或者 MiniProgram来判断
        return StrUtil.containsAnyIgnoreCase(userAgentStr, "MicroMessage")
                && StrUtil.containsAnyIgnoreCase(userAgentStr, "MiniProgram");
    }


    /**
     * 如何判断登录设备是否为pad
     * @param userAgentStr
     * @return
     */
    private static boolean isPad(String userAgentStr) {
        // 包含iPad就是pad
        Boolean isPad = StrUtil.containsIgnoreCase(userAgentStr, "iPad");

        // 安卓平板（包含 Android 但是不包含 Mobile）
        Boolean isAndroidPad = StrUtil.containsIgnoreCase(userAgentStr, "Android")
                && !StrUtil.containsIgnoreCase(userAgentStr, "Mobile");

        // 两者满足一个即可
        return isPad || isAndroidPad;
    }

}
