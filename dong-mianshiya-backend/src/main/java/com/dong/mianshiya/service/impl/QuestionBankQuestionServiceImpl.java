package com.dong.mianshiya.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.mianshiya.common.BatchResult;
import com.dong.mianshiya.common.ErrorCode;
import com.dong.mianshiya.constant.CommonConstant;
import com.dong.mianshiya.exception.BusinessException;
import com.dong.mianshiya.exception.ThrowUtils;
import com.dong.mianshiya.mapper.QuestionBankQuestionMapper;
import com.dong.mianshiya.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.dong.mianshiya.model.entity.Question;
import com.dong.mianshiya.model.entity.QuestionBank;
import com.dong.mianshiya.model.entity.QuestionBankQuestion;
import com.dong.mianshiya.model.entity.User;
import com.dong.mianshiya.model.vo.QuestionBankQuestionVO;
import com.dong.mianshiya.model.vo.UserVO;
import com.dong.mianshiya.service.QuestionBankQuestionService;
import com.dong.mianshiya.service.QuestionBankService;
import com.dong.mianshiya.service.QuestionService;
import com.dong.mianshiya.service.UserService;
import com.dong.mianshiya.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 题目题库关系服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    private QuestionBankService questionBankService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        //题目必须存在
        Long questionId = questionBankQuestion.getQuestionId();
        if (questionId != null){
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在！");
        }
        //题库必须存在
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if (questionBankId != null){
            questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBankService == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在！");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String title = questionBankQuestionQueryRequest.getTitle();
        String content = questionBankQuestionQueryRequest.getContent();
        String searchText = questionBankQuestionQueryRequest.getSearchText();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        List<String> tagList = questionBankQuestionQueryRequest.getTags();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目题库关系封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long questionBankQuestionId = questionBankQuestion.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        // endregion

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题目题库关系封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> questionBankQuestionIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> questionBankQuestionIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    /**
     * 批量添加题目题库关系（事务）
     * 这个方法有事务，如果一次性添加的数据太多，就会造成长事务，长事务出现问题，需要全部回滚
     * 两种方案
     * 1、这里的业务其实不会滚也可以，因为关系数据插入成功即可，插入失败的页面重新插入即可。
     * 2、将大量的数据拆分，每次插入一部分，这样不会造成长事务。
     * 其实就是把方法中循环的部分抽取出来单独做为事务，然后每次分批调用这个方法
     *
     * 异步任务可以使用定时任务或者消息队列来进行解耦
     * 定时任务：将任务存储在数据库，通过定时任务来扫描数据库进行处理
     * 消息队列：将任务发送到队列中，通过消费者来消费队列进行处理
     * @param questionBankId
     * @param questionIds
     * @param user
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BatchResult batchAddQuestionBankQuestion(Long questionBankId, List<Long> questionIds, User user) {
        // 这里需要保存执行的结果，但是并行操作或者异步操作的话，这里就没有意义了
        BatchResult batchResult = new BatchResult();
        // 先检验
        ThrowUtils.throwIf(questionBankId == null || questionBankId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIds), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        // 检查题目ID是否存在
        List<Question> questionList = questionService.listByIds(questionIds);
        // 拿到正常的题目ID
        List<Long> normalQuestionIdList = questionList.stream().map(Question::getId).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(normalQuestionIdList), ErrorCode.PARAMS_ERROR, "题目为空");
        // 检查题库ID是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // todo 这里可以在题库表中增加一个版本号字段，用来进行乐观锁判断
        // 当更新的时候，需要先查询版本号，用于和当前操作版本号进行匹配，如果版本号不一致，说明其他用户已经修改了
        //

        // 过滤掉已经存在关系的数据
        LambdaQueryWrapper<QuestionBankQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, normalQuestionIdList);
        List<QuestionBankQuestion> existQuestionBankQuestionList = this.list(queryWrapper);
        Set<Long> existQuestionIdSet = existQuestionBankQuestionList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toSet());
        // 过滤
        normalQuestionIdList.removeAll(existQuestionIdSet);
        ThrowUtils.throwIf(CollUtil.isEmpty(normalQuestionIdList), ErrorCode.PARAMS_ERROR, "题目已存在");

        // 自定义线程池，用来处理任务
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                10,
                20,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy() //拒绝策略，默认是抛出异常，这里改为由主线程处理
        );
        // 保存所有批次的future
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        // 分批处理避免长事务，假设每次处理 1000 条数据
        int batchSize = 1000;
        int totalQuestionListSize = normalQuestionIdList.size();
        for (int i = 0; i < totalQuestionListSize; i += batchSize) {
            // 生成每批次的数据
            List<Long> subList = normalQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestions = subList.stream().map(questionId -> {
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(questionBankId);
                questionBankQuestion.setQuestionId(questionId);
                questionBankQuestion.setUserId(user.getId());
                return questionBankQuestion;
            }).collect(Collectors.toList());
            // 使用事务处理每批数据
            // 这里使用AopContext 获取代理对象，而不是直接使用this，因为事务是AOP拦截的，所以需要通过AopContext获取代理对象
            // 需要在启动类开启 @EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
            QuestionBankQuestionService questionBankQuestionService =
                    (QuestionBankQuestionServiceImpl) AopContext.currentProxy();
//            questionBankQuestionService.batchAddQuestionsToBankInner(questionBankQuestions);
            // 并发处理，使用自定义线程池
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                questionBankQuestionService.batchAddQuestionsToBankInner(questionBankQuestions);
            }, customExecutor).exceptionally(ex -> {
                log.error("任务失败", ex);
                return null;
            });
            futureList.add(future);
        }
        return batchResult;
    }

        /*// 执行插入
        for (Long questionId : questionIds) {
            QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
            questionBankQuestion.setQuestionBankId(questionBankId);
            questionBankQuestion.setQuestionId(questionId);
            questionBankQuestion.setUserId(user.getId());
            // 针对特定异常，特殊处理
            try {
                boolean result = this.save(questionBankQuestion);
                if (!result) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
                }
            } catch (DataIntegrityViolationException e) {
                log.error("数据库唯一键冲突或违反其他完整性约束，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
            } catch (DataAccessException e) {
                log.error("数据库连接问题、事务问题等导致操作失败，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
            } catch (Exception e) {
                // 捕获其他异常，做通用处理
                log.error("添加题目到题库时发生未知错误，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            }*/

    /**
     * 这种批量的任务，可以通过并发执行提高效率，
     * 可以通过异步执行提高执行效率
     * @param questionBankQuestions
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToBankInner(List<QuestionBankQuestion> questionBankQuestions) {
        for (QuestionBankQuestion questionBankQuestion : questionBankQuestions) {
            long questionId = questionBankQuestion.getQuestionId();
            long questionBankId = questionBankQuestion.getQuestionBankId();
            try {
                // 这里是可以直接使用mybatis plus提供的批量插入的方法，减少和数据库的交互
                boolean result = this.save(questionBankQuestion);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            } catch (DataIntegrityViolationException e) {
                log.error("数据库唯一键冲突或违反其他完整性约束，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
            } catch (DataAccessException e) {
                log.error("数据库连接问题、事务问题等导致操作失败，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
            } catch (Exception e) {
                // 捕获其他异常，做通用处理
                log.error("添加题目到题库时发生未知错误，题目 id: {}, 题库 id: {}, 错误信息: {}",
                        questionId, questionBankId, e.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
            }
        }
    }
}
