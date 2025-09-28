package com.zw.zwoj.controller;

import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.postthumb.PostThumbAddRequest;
import com.zw.zwoj.service.PostService;
import com.zw.zwoj.service.PostThumbService;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("post_thumb")
@Slf4j
public class PostThumbController {
    @Resource
    private PostThumbService postThumbService;

    @Resource
    private UserService userService;

    @PostMapping("/")
    public BaseResponse<Integer> doThumb(@RequestBody PostThumbAddRequest postThumbAddRequest,
                                         HttpServletRequest httpServletRequest) {
        if(postThumbAddRequest.getPostId()==null || postThumbAddRequest.getPostId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        final User loginUser = userService.getLoginUser(httpServletRequest);
        long postId = postThumbAddRequest.getPostId();
        int result = postThumbService.doPostThumb(postId,loginUser);
       return ResultUtils.success(result);
    }
}
