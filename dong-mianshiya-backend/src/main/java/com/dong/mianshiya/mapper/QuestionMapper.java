package com.dong.mianshiya.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dong.mianshiya.model.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author sunbin
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2024-12-12 16:31:52
* @Entity com.dong.mianshiya.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 根据更新时间查出列表，用于同步给ES
     * @param minUpdateTime
     * @return
     */
    @Select("SELECT * FROM question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);

}




