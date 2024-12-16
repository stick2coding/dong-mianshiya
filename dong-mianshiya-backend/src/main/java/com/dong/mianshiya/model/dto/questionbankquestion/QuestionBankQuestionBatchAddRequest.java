package com.dong.mianshiya.model.dto.questionbankquestion;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建题目题库关系请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class QuestionBankQuestionBatchAddRequest implements Serializable {

    /**
     * 题目id
     */
    private List<Long> questionIdList;

    /**
     * 题库id
     */
    private Long questionBankId;

    private static final long serialVersionUID = 1L;
}