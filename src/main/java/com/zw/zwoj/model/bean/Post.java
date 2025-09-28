package com.zw.zwoj.model.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value="post")
public class Post implements Serializable {

     @TableId(type= IdType.ASSIGN_ID)
     private long id;


    private String title;

    private String content;

    private String tags;
    
    private Integer thumbNum;
    
    
    private Integer favourNum;
    
    private Long userId;
    
    private Date createTime;
    
    private Date updateTime;
    
    @TableLogic
    private Integer isDelete;
    
    @TableField(exist = false)
    private static final  long serialVersionUID = 1L;

}
