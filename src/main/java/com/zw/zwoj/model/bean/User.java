package com.zw.zwoj.model.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value="user")
public class User implements Serializable {
   @TableId(type= IdType.ASSIGN_ID)
    private Long id;
   
   private String userAccount;
   
   private String userPassword;
   
   private String unionId;
   
   private String mpOpenId;
   
   private String userName;
   
   private String userAvatar;
   
   private String userProfile;
   
   private String userRole;
   
   private Date createTime;
   private Date updateTime;
   
   @TableLogic
    private Integer isDelete;
   
   @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
