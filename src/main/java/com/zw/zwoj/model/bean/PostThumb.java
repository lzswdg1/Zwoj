package com.zw.zwoj.model.bean;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "post_thumb")
public class PostThumb implements Serializable {

    @TableId(type = IdType.AUTO)
    private long id;
    
    
    private long postId;
    
    private long userId;
    
    private Date createTime;
    
    private Date updateTime;
    
    @TableField(exist = false)
    private static final long serivalVersionUID = 1L;
    
}
