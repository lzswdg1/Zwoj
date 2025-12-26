package com.zw.zwoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.mapper.PostFavourMapper;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.bean.PostFavour;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.service.PostFavourService;
import com.zw.zwoj.service.PostService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

@Service
public class PostFavourServiceImpl extends ServiceImpl<PostFavourMapper, PostFavour> implements PostFavourService {
    
    @Resource
    private PostService postService;
    
    @Override
    public int doPostFavour(long postId, User loginUser){
        Post post = postService.getById(postId);
        if(post==null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        long userId=loginUser.getId();
        
        PostFavourService postFavourService =(PostFavourService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return postFavourService.doPostFavourInner(userId,postId);
        }
    }
    
    @Override
    public Page<Post> listFavourPostByPage(IPage<Post> page, Wrapper<Post> queryWrapper,long favourUserId){
        if(favourUserId<=0){
            return new Page<>();
        }
        return baseMapper.listFavourPostByPage(page,queryWrapper,favourUserId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostFavourInner(long userId, long postId){
        PostFavour postFavour = new PostFavour();
        postFavour.setUserId(userId);
        postFavour.setPostId(postId);
        QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>(postFavour);
        PostFavour oldPostFavour= this.getOne(postFavourQueryWrapper);
        boolean result;
        
        if (oldPostFavour != null) {
            result = this.remove(postFavourQueryWrapper);
            if (result) {
                // 收藏数 - 1
                result = postService.update()
                        .eq("id", postId)
                        .gt("favourNum", 0) // 保证收藏数不会是负数
                        .setSql("favourNum = favourNum - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消收藏失败");
            }
        } else { // 如果 oldPostFavour 为 null，说明未收藏，本次操作为收藏
            result = this.save(postFavour);
            if (result) {
                // 收藏数 + 1
                result = postService.update()
                        .eq("id", postId)
                        .setSql("favourNum = favourNum + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏失败");
            }
        }
    }
}
