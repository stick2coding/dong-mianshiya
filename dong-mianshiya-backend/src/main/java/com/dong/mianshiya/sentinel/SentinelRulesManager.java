package com.dong.mianshiya.sentinel;

import cn.hutool.core.io.FileUtil;
import com.alibaba.csp.sentinel.datasource.*;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.transport.util.WritableDataSourceRegistry;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 定义限流规则
 */
@Component
public class SentinelRulesManager {


    @PostConstruct
    public void initRules() throws Exception {
        // 加载规则
        initFlowRules();
        initDegradeRules();
        // todo 这里应该是从本地文件数据源读取配置文件，但是加上后控制台就显示不出来了，需要排查一下
        //listenRules();
        // todo 另外这里可以扩展动态数据源，比如引入nacos
    }

    public void initFlowRules() {
        // IP查看题目限流
        ParamFlowRule rule = new ParamFlowRule("listQuestionVOByPage")
                .setParamIdx(0) // 对第0个参数进行统计
                .setCount(5) // 每分钟最多多少次
                .setDurationInSec(60); //统计周期
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    // 熔断降级
    public void initDegradeRules() {
        DegradeRule rule = new DegradeRule("listQuestionVOByPage")
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType()) // 慢调用
                .setCount(0.2) // 慢调用比例
                .setTimeWindow(10) // 熔断持续时间
                .setStatIntervalMs(30 * 1000) //统计时长
                .setMinRequestAmount(5) // 最小请求数量
                .setSlowRatioThreshold(3); // 响应时间阈值


        DegradeRule rule2 = new DegradeRule("listQuestionBankVOByPage")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType()) // 异常
                .setCount(0.1) // 慢调用比例
                .setTimeWindow(60) // 熔断持续时间
                .setStatIntervalMs(30 * 1000) //统计时长
                .setMinRequestAmount(5); // 最小请求数量


        DegradeRuleManager.loadRules(Arrays.asList(rule, rule2));
    }



    /**
     * 持久化配置为本地文件
     */
    public void listenRules() throws Exception {
        // 获取项目根目录
        String rootPath = System.getProperty("user.dir");
        // sentinel 目录路径
        File sentinelDir = new File(rootPath, "sentinel");
        // 目录不存在则创建
        if (!FileUtil.exist(sentinelDir)) {
            FileUtil.mkdir(sentinelDir);
        }
        // 规则文件路径
        String flowRulePath = new File(sentinelDir, "FlowRule.json").getAbsolutePath();
        String degradeRulePath = new File(sentinelDir, "DegradeRule.json").getAbsolutePath();

        // Data source for FlowRule
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new FileRefreshableDataSource<>(flowRulePath, flowRuleListParser);
        // Register to flow rule manager.
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        WritableDataSource<List<FlowRule>> flowWds = new FileWritableDataSource<>(flowRulePath, this::encodeJson);
        // Register to writable data source registry so that rules can be updated to file
        WritableDataSourceRegistry.registerFlowDataSource(flowWds);

        // Data source for DegradeRule
        FileRefreshableDataSource<List<DegradeRule>> degradeRuleDataSource
                = new FileRefreshableDataSource<>(
                degradeRulePath, degradeRuleListParser);
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
        WritableDataSource<List<DegradeRule>> degradeWds = new FileWritableDataSource<>(degradeRulePath, this::encodeJson);
        // Register to writable data source registry so that rules can be updated to file
        WritableDataSourceRegistry.registerDegradeDataSource(degradeWds);
    }

    private Converter<String, List<FlowRule>> flowRuleListParser = source -> JSON.parseObject(source,
            new TypeReference<List<FlowRule>>() {
            });
    private Converter<String, List<DegradeRule>> degradeRuleListParser = source -> JSON.parseObject(source,
            new TypeReference<List<DegradeRule>>() {
            });

    private <T> String encodeJson(T t) {
        return JSON.toJSONString(t);
    }


}
