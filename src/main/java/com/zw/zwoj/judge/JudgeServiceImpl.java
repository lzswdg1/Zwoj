//package com.zw.zwoj.judge;
//
//import cn.hutool.json.JSONUtil;
//import com.zw.zwoj.common.ErrorCode;
//import com.zw.zwoj.exception.BusinessException;
//import com.zw.zwoj.judge.codesandbox.CodeSandbox;
//import com.zw.zwoj.judge.codesandbox.CodeSandboxFactory;
//import com.zw.zwoj.judge.codesandbox.CodeSandboxProxy;
//import com.zw.zwoj.judge.codesandbox.model.ExecuteCodeRequest;
//import com.zw.zwoj.judge.codesandbox.model.ExecuteCodeResponse;
//import com.zw.zwoj.judge.codesandbox.model.JudgeInfo;
//import com.zw.zwoj.judge.strategy.JudgeContext;
//import com.zw.zwoj.model.dto.question.JudgeCase;
//import com.zw.zwoj.model.bean.Question;
//import com.zw.zwoj.model.bean.QuestionSubmit;
//import com.zw.zwoj.model.enums.QuestionSubmitStatusEnum;
//import com.zw.zwoj.service.QuestionService;
//import com.zw.zwoj.service.QuestionSubmitService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class JudgeServiceImpl implements JudgeService {
//
//    @Resource
//    private QuestionService questionService;
//
//    @Resource
//    private QuestionSubmitService questionSubmitService;
//
//    @Resource
//    private JudgeManager judgeManager;
//
//    @Value("${codesandbox.type:example}")
//    private String type;
//
//
//    @Override
//    public QuestionSubmit doJudge(long questionSubmitId) {
//        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
//        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
//        if (questionSubmit == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
//        }
//        Long questionId = questionSubmit.getQuestionId();
//        Question question = questionService.getById(questionId);
//        if (question == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
//        }
//        // 2）如果题目提交状态不为等待中，就不用重复执行了
//        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
//        }
//        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
//        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
//        questionSubmitUpdate.setId(questionSubmitId);
//        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
//        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
//        if (!update) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
//        }
//        // 4）调用沙箱，获取到执行结果
//        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
//        codeSandbox = new CodeSandboxProxy(codeSandbox);
//        String language = questionSubmit.getLanguage();
//        String code = questionSubmit.getCode();
//        // 获取输入用例
//        String judgeCaseStr = question.getJudgeCase();
//        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
//        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
//        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
//                .code(code)
//                .language(language)
//                .inputList(inputList)
//                .build();
//        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
//        List<String> outputList = executeCodeResponse.getOutputList();
//        // 5）根据沙箱的执行结果，设置题目的判题状态和信息
//        JudgeContext judgeContext = new JudgeContext();
//        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
//        judgeContext.setInputList(inputList);
//        judgeContext.setOutputList(outputList);
//        judgeContext.setJudgeCaseList(judgeCaseList);
//        judgeContext.setQuestion(question);
//        judgeContext.setQuestionSubmit(questionSubmit);
//        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
//        // 6）修改数据库中的判题结果
//        questionSubmitUpdate = new QuestionSubmit();
//        questionSubmitUpdate.setId(questionSubmitId);
//        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
//        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
//        update = questionSubmitService.updateById(questionSubmitUpdate);
//        if (!update) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
//        }
//        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionId);
//        return questionSubmitResult;
//    }
//}
package com.zw.zwoj.judge;

import cn.hutool.json.JSONUtil;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.judge.codesandbox.CodeSandbox;
import com.zw.zwoj.judge.codesandbox.CodeSandboxFactory;
import com.zw.zwoj.judge.codesandbox.CodeSandboxProxy;
import com.zw.zwoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.zw.zwoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.zw.zwoj.judge.codesandbox.model.JudgeInfo;
import com.zw.zwoj.judge.strategy.JudgeContext;
import com.zw.zwoj.model.dto.question.JudgeCase;
import com.zw.zwoj.model.bean.Question;
import com.zw.zwoj.model.bean.QuestionSubmit;
import com.zw.zwoj.model.enums.QuestionSubmitStatusEnum;
import com.zw.zwoj.service.QuestionService;
import com.zw.zwoj.service.QuestionSubmitService;
import lombok.extern.slf4j.Slf4j; // 1. 引入 Slf4j
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j // 2. 添加注解，允许使用 log
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }

        // --- 核心改动：使用 try-catch 包裹沙箱执行过程 ---
        CodeSandbox codeSandbox = null;
        ExecuteCodeResponse executeCodeResponse = null;
        try {
            // 4）调用沙箱，获取到执行结果
            codeSandbox = CodeSandboxFactory.newInstance(type);
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            String language = questionSubmit.getLanguage();
            String code = questionSubmit.getCode();
            // 获取输入用例
            String judgeCaseStr = question.getJudgeCase();
            List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
            List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .language(language)
                    .inputList(inputList)
                    .build();
            // 【关键】这里可能会抛出异常（如连接超时、沙箱挂了）
            executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);

            List<String> outputList = executeCodeResponse.getOutputList();
            // 5）根据沙箱的执行结果，设置题目的判题状态和信息
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
            judgeContext.setInputList(inputList);
            judgeContext.setOutputList(outputList);
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);

            // 6）修改数据库中的判题结果
            questionSubmitUpdate = new QuestionSubmit();
            questionSubmitUpdate.setId(questionSubmitId);
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            update = questionSubmitService.updateById(questionSubmitUpdate);
            if (!update) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
            }
        } catch (Exception e) {
            // --- 异常处理逻辑 ---
            log.error("判题失败，questionSubmitId = " + questionSubmitId, e);
            // 将状态更新为“失败”
            QuestionSubmit questionSubmitFail = new QuestionSubmit();
            questionSubmitFail.setId(questionSubmitId);
            questionSubmitFail.setStatus(QuestionSubmitStatusEnum.FAILED.getValue()); // 确保您的枚举里有 FAILED (通常值为3)
            questionSubmitFail.setJudgeInfo("判题系统错误：" + e.getMessage());
            boolean updateFail = questionSubmitService.updateById(questionSubmitFail);
            if (!updateFail) {
                log.error("判题状态更新失败, questionSubmitId = " + questionSubmitId);
            }
            // 重新抛出异常，或者直接结束（看业务需求，通常抛出让外层也能感知）
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题执行失败：" + e.getMessage());
        }

        // 返回最新的提交信息
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionSubmitId);
        return questionSubmitResult;
    }
}