package com.zw.zwoj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import com.zw.zwoj.model.vo.PostVO;

import javax.servlet.http.HttpServletRequest;

public interface PostService extends IService<Post> {
    
    void validPost(Post post,boolean add);
    
    
    QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest);
    
    Page<Post> searchFromEs(PostQueryRequest postQueryRequest);
    
    PostVO getPostVO(Post post, HttpServletRequest request);
    
 
    
    Page<PostVO>  getPostVOPage(Page<Post> page,HttpServletRequest request);
}
