package com.dong.mianshiya.esdao;

import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class QuestionEsDaoTest {

    @Resource
    private QuestionEsDao questionEsDao;

//    @Test
//    void findByUserId() {
//        List<QuestionEsDTO> result = questionEsDao.findByUserId(1L);
//        System.out.println(result);
//    }


}