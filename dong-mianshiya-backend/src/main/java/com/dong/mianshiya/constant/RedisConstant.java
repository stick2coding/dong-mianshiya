package com.dong.mianshiya.constant;

public interface RedisConstant {

    /**
     * 用户签到key前缀
     */
    public static final String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins:";


    static String getUserSignInKey(long userId, int year) {
        return String.format("%s:%s:%s", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }

}
