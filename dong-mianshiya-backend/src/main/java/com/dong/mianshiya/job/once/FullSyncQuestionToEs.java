package com.dong.mianshiya.job.once;

import cn.hutool.core.collection.CollUtil;
import com.dong.mianshiya.esdao.QuestionEsDao;
import com.dong.mianshiya.model.dto.question.QuestionEsDTO;
import com.dong.mianshiya.model.entity.Question;
import com.dong.mianshiya.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 这里启动后会先执行一次，但是如果没有配置ES，这里就会出现错误
 */
@Component
@Slf4j
public class FullSyncQuestionToEs implements CommandLineRunner {

    @Resource
    private QuestionService questionService;
    @Autowired
    private QuestionEsDao questionEsDao;

    @Override
    public void run(String... args) throws Exception {
        // 全量获取题目
        List<Question> allQuestionList = questionService.list();
        if (CollUtil.isEmpty(allQuestionList)){
            return;
        }

        // 转为ES实体
        List<QuestionEsDTO> questionEsDTOList = allQuestionList.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());

        //分页批量插入ES
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("FullSyncQuestionToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            // 以i为当前坐标，往后查pagesize个数据，如果结束坐标大于等于总长度，则以总长结束
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            try {
                questionEsDao.saveAll(questionEsDTOList.subList(i, end));
            } catch (Exception e) {
                log.error("执行异常", e);
            }
            // 结束后，当前坐标向右移动pagesize个位置
        }
        log.info("FullSyncQuestionToEs end, total {}", total);
    }
}
