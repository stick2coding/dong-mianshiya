package com.dong.mianshiya.blackfilter;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * 黑名单过滤
 */
@Slf4j
public class BlackIpUtils {


    //定义一个布隆过滤器
    private static BitMapBloomFilter bloomFilter;


    // 判断黑名单
    public static boolean isBlackIp(String ip) {
        return bloomFilter.contains(ip);
    }

    // 重建黑名单
    public static void rebuildBlackIp(String configInfo) {
        // 参数校验，如果为空，传{}
        if (StrUtil.isBlank(configInfo)) {
            configInfo = "{}";
        }

        // 解析yaml文件
        Yaml yaml = new Yaml();
        Map map = yaml.load(configInfo);

        //获取黑名单
        List<String> blackIpList = (List<String>) map.get("blackIpList");

        // 加锁防止并发
        synchronized (BlackIpUtils.class) {
            if (CollUtil.isEmpty(blackIpList)){
                //清空过滤器
                bloomFilter = new BitMapBloomFilter(100);
            }else {
                // 不为空就添加
                BitMapBloomFilter newBloomFilter = new BitMapBloomFilter(100);
                for (String ip : blackIpList){
                    newBloomFilter.add(ip);
                }
                bloomFilter = newBloomFilter;
            }
        }


    }
}
