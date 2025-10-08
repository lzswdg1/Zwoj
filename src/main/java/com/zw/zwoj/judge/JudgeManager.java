package com.zw.zwoj.judge;

import com.zw.zwoj.judge.codesandbox.model.JudgeInfo;
import com.zw.zwoj.judge.strategy.DefaultJudgeStrategy;
import com.zw.zwoj.judge.strategy.JavaLanguageJudgeStrategy;
import com.zw.zwoj.judge.strategy.JudgeContext;
import com.zw.zwoj.judge.strategy.JudgeStrategy;
import com.zw.zwoj.model.bean.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
