package com.dong.mianshiya.esdao;

import com.dong.mianshiya.model.dto.question.QuestionEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目es操作
 */
public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsDTO, Long> {

    /**
     * ES这里可以根据方法名自动映射为查询语句
     * 根据用户ID查询
     * @param userId
     * @return
     */
    List<QuestionEsDTO> findByUserId(Long userId);


}
