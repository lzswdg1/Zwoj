package com.zw.zwoj.model.enums;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum FileUploadBizEnum {

    USER_AVATAR("用户头像","user_avatar");

    private  final String text;
    private  final String  value;
    
    FileUploadBizEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    
    public static List<String> getValues(){
    return Arrays.stream(values()).map(item-> item.value).collect(Collectors.toList());
    }
    
    public static FileUploadBizEnum getEunmByValue(String value){
    if(ObjectUtils.isEmpty(value)){
    return null;
    }
    
    for(FileUploadBizEnum item : FileUploadBizEnum.values()){
        if(item.value.equals(value)){
        return item;
        }
    }
    return null;
    }
    public String getText() { return text; }
    public String getValue() { return value; }
}
