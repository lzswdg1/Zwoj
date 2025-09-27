package com.zw.zwoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.mapper.PostThumbMapper;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.bean.PostThumb;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.service.PostService;
import com.zw.zwoj.service.PostThumbService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class PostThumbServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb> implements PostThumbService
{
    @Resource
    private PostService postService;
    
    @Override
    public int doPostThumb(long postId, User loginUser){
        Post post = postService.getById(postId);
        if(post ==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //是否已点赞
        long userId = loginUser.getId();
        //每个用户串行点赞
        //锁必须要包裹事务方法
        PostThumbService postThumbService = (PostThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return postThumbService.doPostThumbInner(userId, postId);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostThumbInner(long userId, long postId) {
        PostThumb postThumb = new PostThumb();
        postThumb.setUserId(userId);
        postThumb.setPostId(postId);
        QueryWrapper<PostThumb> queryWrapper = new QueryWrapper<PostThumb>();
        PostThumb oldPostThumb = this.getOne(queryWrapper);
        boolean result;
        //已点赞
        if (oldPostThumb != null) {
            result = this.remove(queryWrapper);
            if (result) {
                //点赞数-1
                result = postService.update()
                        .eq("id", postId)
                        .gt("thumbNum", 0)
                        .setSql("thumbNum = thumbNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            //未点赞
            result = this.save(postThumb);
            if (result) {
                result = postService.update()
                        .eq("id", postId)
                        .setSql("thumb = thumb + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }
}
