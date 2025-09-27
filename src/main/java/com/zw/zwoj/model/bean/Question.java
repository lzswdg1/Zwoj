package com.zw.zwoj.model.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "question")
public class Question implements Serializable {
    
    @TableId(type = IdType.ASSIGN_ID)
    private long id;
    
    private String title;
    
    private String content;
    
    private String tags;
    
    private String answer;
    
    private Integer submitNum;
    
    private Integer acceptedNum;
    
    private String judgeCase;
    
    private String judgeConfig;
    
    private Integer thumbNum;
    
    private Integer favourNum;
    
    private Long userId;
    
    
    private Date createTime;
    private Date updateTime;
    
    @TableLogic
    private Integer isDelete;
    
    
    @TableField(exist = false)
    private static final long servialVersionUID = 1L;
}
