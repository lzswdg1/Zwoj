package com.zw.zwoj.model.bean;


import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "question_submit")
public class QuestionSubmit  implements Serializable {


     @TableId(type = IdType.ASSIGN_ID)
     private Long id;
     
     private String language;
     
     private String  code;
     
     private String  judgeInfo;
     
     private Integer status;
     
     
     private Long questionId;
     
     private Long userId;
     
     private Date  createTime;
     private Date  updateTime;
     
     @TableLogic
    private Integer isDelete;
    
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
