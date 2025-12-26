package com.zw.zwoj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.zwoj.model.bean.QuestionSubmit;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.zw.zwoj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.zw.zwoj.model.vo.QuestionSubmitVO;

public interface QuestionSubmitService extends IService<QuestionSubmit> {
    
    long doQuestionSubmit(QuestionSubmitAddRequest request, User loginUser);
    
    
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest request);
    
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser);
    
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> page,User loginUser);
}
