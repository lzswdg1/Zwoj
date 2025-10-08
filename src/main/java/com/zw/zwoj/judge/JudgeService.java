package com.zw.zwoj.judge;

import com.zw.zwoj.model.bean.QuestionSubmit;

/**
 * 判题服务
 */
public interface JudgeService {

    /**
     * 判题
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);
}
