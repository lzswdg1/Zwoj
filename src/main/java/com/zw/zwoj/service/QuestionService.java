package com.zw.zwoj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.zwoj.model.bean.Question;
import com.zw.zwoj.model.dto.question.QuestionQueryRequest;
import com.zw.zwoj.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;

public interface QuestionService extends IService<Question> {
    void validQuestion(Question question,boolean add);
    
    
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest request);
    
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);
    
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage,HttpServletRequest request);
}
