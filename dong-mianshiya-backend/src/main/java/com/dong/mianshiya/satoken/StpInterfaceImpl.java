package com.dong.mianshiya.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.dong.mianshiya.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static com.dong.mianshiya.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 引入sa-token后，我们使用注解的方式来进行校验
 * 那么sa如何知道有哪些注解呢
 * 这里就需要自主实现一下sa里面的这个接口
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回账号所有的权限
     * @param o
     * @param s
     * @return
     */
    @Override
    public List<String> getPermissionList(Object o, String s) {
        return Collections.emptyList();
    }

    /**
     * 返回账号所有的角色
     * @param loginId
     * @param s
     * @return
     */
    @Override
    public List<String> getRoleList(Object loginId, String s) {
        User user = (User) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getUserRole());
    }
}
