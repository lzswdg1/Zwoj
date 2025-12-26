package com.zw.zwoj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.zw.zwoj.annotation.AuthCheck;
import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.DeleteRequest;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.constant.UserConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.exception.ThrowUtils;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.post.PostAddRequest;
import com.zw.zwoj.model.dto.post.PostEditRequest;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import com.zw.zwoj.model.dto.post.PostUpdateRequest;
import com.zw.zwoj.model.vo.PostVO;
import com.zw.zwoj.service.PostService;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import com.google.gson.Gson;


@RestController
@RequestMapping("/post")
@Slf4j
public class PostController {
    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

     private final static Gson gson = new Gson();


     @PostMapping("/add")
    public BaseResponse addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request){
         if(postAddRequest == null){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }
         Post post = new Post();
         BeanUtils.copyProperties(postAddRequest, post);
         List<String> tags = postAddRequest.getTags();
         if(tags != null){
             post.setTags(gson.toJson(tags));
         }
         postService.validPost(post,true);
         User loginUser = userService.getLoginUser(request);
         post.setUserId(loginUser.getId());
         post.setFavourNum(0);
         post.setThumbNum(0);
         boolean result = postService.save(post);
         ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
         long newPostId = post.getId();
         return ResultUtils.success(newPostId);
     }

     @PostMapping("/delete")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
         if(deleteRequest == null || deleteRequest.getId()<=0){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }
         User user = userService.getLoginUser(request);
         long id = deleteRequest.getId();
         //判断是否存在
         Post oldPost = postService.getById(id);
         ThrowUtils.throwIf(oldPost == null,ErrorCode.NOT_FOUND_ERROR);
         //仅本人和管理员可以删除
         if(!oldPost.getUserId().equals(user.getId())&&!userService.isAdmin(request)){
             throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
         }
         boolean b = postService.removeById(id);
         return ResultUtils.success(b);
     }

     @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse updatePost(@RequestBody PostUpdateRequest postUpdateRequest, HttpServletRequest request){

         if(postUpdateRequest == null || postUpdateRequest.getId()<=0){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }

         Post post = new Post();
         BeanUtils.copyProperties(postUpdateRequest, post);
         List<String> tags = postUpdateRequest.getTags();
         if(tags != null){
             post.setTags(gson.toJson(tags));
         }

         //参数校验
         postService.validPost(post,false);
         long id = postUpdateRequest.getId();

         Post oldPost = postService.getById(id);

         ThrowUtils.throwIf(oldPost == null,ErrorCode.NO_AUTH_ERROR);
         boolean result = postService.updateById(post);
         return ResultUtils.success(result);
     }

     @PostMapping("/get/vo")
    public BaseResponse<PostVO> getPostVOById(long id ,HttpServletRequest request){
         if(id <= 0){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }
         Post post = postService.getById(id);
         if(post == null){
             throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
         }
         return ResultUtils.success(postService.getPostVO(post,request));
     }

     @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<PostVO>> listMyPostVOByPage (@RequestBody PostQueryRequest postQueryRequest,
                                                          HttpServletRequest httpServletRequest){
         if(postQueryRequest == null){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }

         User loginUser = userService.getLoginUser(httpServletRequest);
         postQueryRequest.setUserId(loginUser.getId());
         long current = postQueryRequest.getCurrent();
         long size = postQueryRequest.getPageSize();
         //🚫爬虫
         ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
         Page<Post> postPage = postService.page(new Page<>(current,size),
                 postService.getQueryWrapper(postQueryRequest));
         return ResultUtils.success(postService.getPostVOPage(postPage,httpServletRequest));
     }
    @PostMapping("/search/page/vo")
    public BaseResponse<Page<PostVO>> searchPostVOByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                         HttpServletRequest httpServletRequest){
         long size = postQueryRequest.getPageSize();
         ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
         Page<Post> postPage = postService.searchFromEs(postQueryRequest);
         return ResultUtils.success(postService.getPostVOPage(postPage,httpServletRequest));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPost(@RequestBody PostEditRequest postEditRequest, HttpServletRequest request){
         if(postEditRequest == null || postEditRequest.getId() <= 0){
             throw new BusinessException(ErrorCode.PARAMS_ERROR);
         }
         Post post = new Post();
         BeanUtils.copyProperties(postEditRequest, post);
         List<String> tags = postEditRequest.getTags();
         if(tags != null){
             post.setTags(gson.toJson(tags));
         }

         postService.validPost(post,false);
         User loginUser = userService.getLoginUser(request);
         long id = postEditRequest.getId();
         Post oldPost = postService.getById(id);
         ThrowUtils.throwIf(oldPost == null,ErrorCode.NOT_FOUND_ERROR);

         if(!oldPost.getUserId().equals(loginUser.getId())&&!userService.isAdmin(request)){
             throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
         }
         boolean result = postService.updateById(post);
         return ResultUtils.success(result);
    }
}
