package com.dong.mianshiya.model.dto.questionbankquestion;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建题目题库关系请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class QuestionBankQuestionAddRequest implements Serializable {

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题库id
     */
    private Long questionBankId;

    private static final long serialVersionUID = 1L;
}