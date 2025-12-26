package com.zw.zwoj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.exception.ThrowUtils;
import com.zw.zwoj.model.bean.Post;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.post.PostQueryRequest;
import com.zw.zwoj.model.dto.postfavour.PostFavourAddRequest;
import com.zw.zwoj.model.dto.postfavour.PostFavourQueryRequest;
import com.zw.zwoj.model.vo.PostVO;
import com.zw.zwoj.service.PostFavourService;
import com.zw.zwoj.service.PostService;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/post_favour")
@Slf4j
public class PostFavourController {
    @Resource
    private PostFavourService postFavourService;

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    @PostMapping("/")
    public BaseResponse<Integer> doPostFavour(@RequestBody PostFavourAddRequest postFavourAddRequest,
                                              HttpServletRequest request) {
     if(postFavourAddRequest ==null || postFavourAddRequest.getPostId() <=0){
         throw new BusinessException(ErrorCode.PARAMS_ERROR);
     }
     final User loginUser  = userService.getLoginUser(request);
     long postId = postFavourAddRequest.getPostId();
     int result = postFavourService.doPostFavour(postId,loginUser);
     return ResultUtils.success(result);
    }


    @PostMapping("/my/list/page")
    public BaseResponse<Page<PostVO>> listMyFavourPostByPage(@RequestBody PostQueryRequest request,
                                                             HttpServletRequest httpServletRequest) {
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        long current = request.getCurrent();
        long size = request.getPageSize();
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current,size),
                postService.getQueryWrapper(request),loginUser.getId());
        return ResultUtils.success(postService.getPostVOPage(postPage,httpServletRequest));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<PostVO>> listFavourPostByPage(@RequestBody PostFavourQueryRequest postFavourQueryRequest,
                                                           HttpServletRequest httpServletRequest) {
        if(postFavourQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postFavourQueryRequest.getPostQueryRequest().getCurrent();
        long size =postFavourQueryRequest.getPostQueryRequest().getPageSize();
        long userId = postFavourQueryRequest.getPostQueryRequest().getUserId();

        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current,size),
                postService.getQueryWrapper(postFavourQueryRequest.getPostQueryRequest()),userId);
        return ResultUtils.success(postService.getPostVOPage(postPage,httpServletRequest));
    }
}
