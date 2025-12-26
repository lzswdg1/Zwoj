package com.zw.zwoj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zw.zwoj.annotation.AuthCheck;
import com.zw.zwoj.common.BaseResponse;
import com.zw.zwoj.common.DeleteRequest;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.common.ResultUtils;
import com.zw.zwoj.config.WxOpenConfig;
import com.zw.zwoj.constant.UserConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.exception.ThrowUtils;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.user.*;
import com.zw.zwoj.model.vo.LoginUserVO;
import com.zw.zwoj.model.vo.UserVO;
import com.zw.zwoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.device.BaseResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    
    @Resource
    private WxOpenConfig wxOpenConfig;
    
    
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if(userRegisterRequest ==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount=userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            return null;
        }
        long result = userService.userRegister(userAccount,userPassword,checkPassword);
        return ResultUtils.success(result);
    }
    
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest request) {
        if(userLoginRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount=userLoginRequest.getUserAccount();
        String userPassword=userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount,userPassword,request);
        return ResultUtils.success(loginUserVO);
    }
    
    @GetMapping("/login/wx_open")
    public BaseResponse<LoginUserVO> userLoginWxOpen(HttpServletRequest request, HttpServletResponse response,
                                                     @RequestParam("code") String code) {
        WxOAuth2AccessToken accessToken;
        
        try {
            WxMpService wxMpService = wxOpenConfig.getWxMpService();
            accessToken = wxMpService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxMpService.getOAuth2Service().getUserInfo(accessToken,code);
            String mpOpenId = userInfo.getOpenid();
            String unionId = userInfo.getUnionId();
            if(StringUtils.isAnyBlank(mpOpenId,unionId)){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"登入失败，系统错误");
            }
            return ResultUtils.success(userService.userLoginByMpOpen(userInfo,request));
        }catch (Exception e){
            log.info("userloginByWxopen error",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"登入失败，系统错误");
            
        }
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
    
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }
    
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest,
                                      HttpServletRequest request) {
        if(userAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user =new User();
        BeanUtils.copyProperties(userAddRequest,user);
        boolean result =userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }
    
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request) {
        if(deleteRequest ==null ||deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b =userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }
    
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        if(userUpdateRequest==null ||userUpdateRequest.getId()==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user =new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        boolean result =userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    
    
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id,HttpServletRequest request) {
        if(id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }
    
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response =getUserById(id,request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }
    
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> page = userService.page(new Page<>(current,pageSize),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(page);
    }
    
    
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,HttpServletRequest request){
        if(userQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize>20,ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current,pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current,pageSize,userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateRequest userUpdateRequest,
                                              HttpServletRequest request) {
        if(userUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser =userService.getLoginUser(request);
        User user =new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        user.setId(loginUser.getId());
        boolean result =userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}

