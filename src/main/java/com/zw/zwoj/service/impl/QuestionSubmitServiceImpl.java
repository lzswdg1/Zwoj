package com.zw.zwoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zw.zwoj.utils.SqlUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.constant.CommonConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.judge.JudgeService;
import com.zw.zwoj.mapper.QuestionSubmitMapper;
import com.zw.zwoj.model.bean.Question;
import com.zw.zwoj.model.bean.QuestionSubmit;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.zw.zwoj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.zw.zwoj.model.enums.QuestionSubmitLanguageEnum;
import com.zw.zwoj.model.enums.QuestionSubmitStatusEnum;
import com.zw.zwoj.model.vo.QuestionSubmitVO;
import com.zw.zwoj.service.QuestionService;
import com.zw.zwoj.service.QuestionSubmitService;
import com.zw.zwoj.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit> implements QuestionSubmitService {
    
    @Resource
    private QuestionService questionService;
    
    @Resource
    private UserService userService;
    
    @Resource
    @Lazy
    private JudgeService judgeService;
    
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser){
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if(languageEnum == null){
              throw new BusinessException(ErrorCode.PARAMS_ERROR,"编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        Question question = questionService.getById(questionId);
        if(question == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //是否已提交题目
        long userId = loginUser.getId();
        //每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setUserId(userId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        
        //设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if(!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        //执行判题任务
        CompletableFuture.runAsync(() ->{
            judgeService.doJudge(questionSubmitId);
        });
        return questionSubmitId;
    }
    
//    @Override
//    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest){
//        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
//        if(questionSubmitQueryRequest != null){
//            return  queryWrapper;
//        }
//        String language = questionSubmitQueryRequest.getLanguage();
//        Integer status = questionSubmitQueryRequest.getStatus();
//        Long questionId = questionSubmitQueryRequest.getQuestionId();
//        Long userId = questionSubmitQueryRequest.getUserId();
//        String sortField = questionSubmitQueryRequest.getSortField();
//        String sortOrder = questionSubmitQueryRequest.getSortOrder();
//
//        //拼接查询条件
//        queryWrapper.eq(StringUtils.isNotBlank(language),"language",language);
//        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
//        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status)!=null,"status",status);
//        queryWrapper.eq("isDelete",false);
//        queryWrapper.orderBy(SqlUtils.validSortField(sortField),sortField.equals(CommonConstant.SORT_ORDER_ASC),
//                sortField);
//        return queryWrapper;
//    }
//
@Override
public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
    QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
    if (questionSubmitQueryRequest == null) {
        return queryWrapper;
    }

    String language = questionSubmitQueryRequest.getLanguage();
    Integer status = questionSubmitQueryRequest.getStatus();
    Long questionId = questionSubmitQueryRequest.getQuestionId();
    Long userId = questionSubmitQueryRequest.getUserId();
    String sortField = questionSubmitQueryRequest.getSortField();
    String sortOrder = questionSubmitQueryRequest.getSortOrder();

    // 拼接查询条件
    queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
    queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
    // 原有代码漏了 questionId 的查询，建议补上
    queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
    queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
    queryWrapper.eq("isDelete", false);

    // --- 👇 重点修改：排序逻辑 👇 ---

    // 如果前端没有传排序字段，则默认按照创建时间倒序（最新的在最前）
    if (StringUtils.isBlank(sortField)) {
        queryWrapper.orderByDesc("createTime");
    } else {
        // 如果前端传了排序字段，则按照前端的要求来
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField);
    }

    return queryWrapper;
}
    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit,User loginUser){
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        //脱敏 仅本人和管理员能看见自己（提交userId 和登入用户id不同)提交的代码
        long userId = loginUser.getId();
        
        //处理脱敏
        if(userId != questionSubmit.getUserId()&& !userService.isAdmin(loginUser)){
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }
    
    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage,User loginUser){
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(),questionSubmitPage.getSize(),questionSubmitPage.getTotal());
        if(CollectionUtils.isEmpty(questionSubmitList)){
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit,loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
}
