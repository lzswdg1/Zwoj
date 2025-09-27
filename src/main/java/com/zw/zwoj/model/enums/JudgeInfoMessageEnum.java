package com.zw.zwoj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum JudgeInfoMessageEnum {
    ACCEPTED("成功","Accepted"),
    WRONG_ANSWER("答案错误","Wrong Anwser"),
    COMPILE_ERROR("编译错误","Compile Error"),
    MEMORY_LIMIT_EXCEEDED("内存溢出","Memory Exceeded"),
    TIME_LIMIT_EXCEEDED("时间超时","Time Limit Exceeded"),
    PRESENTATION_ERROR("展示错误","Presentation Error"),
    WAITING("等待中","Waiting"),
    OUTPUT_LIMIT_EXCEEDED("输出溢出","Output Limit Exceeded"),
    DANGEROUS_OPERATION("危险操作","Dangerous Operation"),
    RUNTION_ERROR("运行错误","Runtime Error"),
    SYSTEM_ERROR("系统错误","System Error");
    
    private final String text;
    private final String value;
    
    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    };
    
    public static List<String> getValues() {
    return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }
    
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if(ObjectUtils.isEmpty(value)){
            return null;
        }
        for(JudgeInfoMessageEnum item : values()) {
            if(item.value.equals(value)) {
             return item;
            }
        }
        return null;
    }
    
    public String getText() {return text;}
    public String getValue() {return value;}
}
