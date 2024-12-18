package com.dong.mianshiya.blackfilter;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class NacosListener implements InitializingBean {


    @NacosInjected
    private ConfigService configService;

    @Value("${nacos.config.data-id}")
    private String dataId;

    @Value("${nacos.config.group}")
    private String group;



    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("NacosListener init start");


        // 获取配置并注册监听器
        String config = configService.getConfigAndSignListener(dataId, group, 5000, new Listener(){

            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String s) {
                log.info("receiveConfigInfo: " + s);
                BlackIpUtils.rebuildBlackIp(s);
            }
        });
        log.info("NacosListener init config:" + config);
        // 初始化黑名单
        BlackIpUtils.rebuildBlackIp(config);

    }
}
