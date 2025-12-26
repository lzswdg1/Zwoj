package com.zw.zwoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.constant.CommonConstant;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.mapper.UserMapper;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.dto.user.UserQueryRequest;
import com.zw.zwoj.model.enums.UserRoleEnum;
import com.zw.zwoj.model.vo.LoginUserVO;
import com.zw.zwoj.model.vo.UserVO;
import com.zw.zwoj.service.UserService;
import com.zw.zwoj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zw.zwoj.constant.UserConstant.USER_LOGIN_STATE;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static final String SALT = "zw";
    
    
    @Override
    public long userRegister(String userAccount,String userPassword,String checkPassword) {
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if(userPassword.length()<8 || checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不一致");
        }
        synchronized (userAccount.intern()) {
            //账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if(count>0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");
            }
            //加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            //插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if(saveResult){
                return user.getId();
            }else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"注册失败，数据库错误");
            }
        }
     }
     
     @Override
    public LoginUserVO userLogin(String userAccount, String pwd, HttpServletRequest request) {
        if(StringUtils.isAnyBlank(userAccount,pwd)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号错误");
        }
        if(pwd.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        }
        //加密
         String encryptPwd = DigestUtils.md5DigestAsHex((SALT + pwd).getBytes());
         //查询用户是否存在
         QueryWrapper<User> queryWrapper = new QueryWrapper<>();
         queryWrapper.eq("userAccount", userAccount);
         queryWrapper.eq("userPassword", encryptPwd);
         User user = this.baseMapper.selectOne(queryWrapper);
         if(user==null){
             log.info("登入失败，账号和密码不匹配");
             throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或者密码错误");
         }
         //记住用户的登入状态
         request.getSession().setAttribute(USER_LOGIN_STATE,user);
         return this.getLoginUserVO(user);
     }
    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }
    
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser==null || currentUser.getId()==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //从数据库查询（追求性能直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if(currentUser==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
    
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        //先判断是否已经登入
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser==null || currentUser.getId()==null){
            return null;
        }
        
        long userId = currentUser.getId();
        return this.getById(userId);
    }
    
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        return isAdmin(currentUser);
    }
    
    @Override
    public boolean isAdmin(User user) {
        return user!= null &&UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
    
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if(request.getSession().getAttribute(USER_LOGIN_STATE)==null){
            throw  new BusinessException(ErrorCode.OPERATION_ERROR,"未登入");
        }
        
        //移除登入态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }
    
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if(user==null){
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVO);
        return loginUserVO;
    }
    
    @Override
    public UserVO getUserVO(User user) {
        if(user==null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }
    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if(CollectionUtils.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if(userQueryRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id!=null,"id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId),"unionid", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId),"mpOpenid", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userName),"userName", userName);
        queryWrapper.eq(StringUtils.isNotBlank(userProfile),"userProfile", userProfile);
        queryWrapper.eq(StringUtils.isNotBlank(userRole),"userRole", userRole);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),sortField.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
