package com.zw.zwoj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.zwoj.model.bean.PostThumb;
import com.zw.zwoj.model.bean.User;

public interface PostThumbService extends IService<PostThumb> {
    
    int doPostThumb(long postId, User loginUser);
    
    int doPostThumbInner(long userId,long postId);
}
