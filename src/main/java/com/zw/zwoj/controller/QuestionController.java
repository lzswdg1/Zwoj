package com.zw.zwoj.controller;

import co.elastic.clients.elasticsearch.xpack.usage.Base;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.zw.zwoj.annotation.AuthCheck;
import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.DeleteRequest;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.constant.UserConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.exception.ThrowUtils;
import com.zw.zwoj.model.bean.Question;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.question.*;
import com.zw.zwoj.model.vo.QuestionVO;
import com.zw.zwoj.service.QuestionService;
import com.zw.zwoj.service.QuestionSubmitService;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionSubmitService questionSubmitService;
    private final static Gson gson = new Gson();

    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest,
                                          HttpServletRequest httpServletRequest) {
        if(questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if(tags != null) {
            question.setTags(gson.toJson(tags));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if(judgeConfig != null) {
            question.setJudgeConfig(gson.toJson(judgeConfig));
        }
        questionService.validQuestion(question,true);
        User loginUser = userService.getLoginUser(httpServletRequest);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result =questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newquestionId = question.getId();
        return ResultUtils.success(newquestionId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest ,HttpServletRequest
                                                httpServletRequest) {
        if(deleteRequest == null || deleteRequest.getId() <=0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(httpServletRequest);
        long id =deleteRequest.getId();

        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion==null,ErrorCode.NOT_FOUND_ERROR);

        if(!oldQuestion.getUserId().equals(user.getId())&&!userService.isAdmin(httpServletRequest)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if(questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if(tags != null) {
            question.setTags(gson.toJson(tags));
        }
        List<JudgeCase> judgeCase =questionUpdateRequest.getJudgeCase();
        if(judgeCase != null) {
            question.setJudgeConfig(gson.toJson(judgeCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if(judgeConfig != null) {
            question.setJudgeConfig(gson.toJson(judgeConfig));
        }

        questionService.validQuestion(question,false);

        long id = questionUpdateRequest.getId();

        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null,ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(Long id,
                                                  HttpServletRequest httpServletRequest) {
        if(id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if(question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        if(!question.getUserId().equals(loginUser.getId())&&!userService.isAdmin(httpServletRequest)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }

    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id ,HttpServletRequest httpServletRequest) {
        if(id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if(question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(questionService.getQuestionVO(question,httpServletRequest));
    }


    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest httpServletRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf((size>20,ErrorCode.PARAMS_ERROR));
        Page<Question> questionPage =questionService.page(new Page<>(current,size),
               questionService.getQueryWrapper(questionQueryRequest) );
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage,httpServletRequest));
    }
}

