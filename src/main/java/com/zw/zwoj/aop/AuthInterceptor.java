package com.zw.zwoj.aop;

import com.alibaba.excel.util.StringUtils;
import com.zw.zwoj.annotation.AuthCheck;
import com.zw.zwoj.common.ErrorCode;
import com.zw.zwoj.exception.BusinessException;
import com.zw.zwoj.model.bean.User;
import com.zw.zwoj.model.enums.UserRoleEnum;
import com.zw.zwoj.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;
    
    
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
          String mustRole = authCheck.mustRole();
          RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes)requestAttributes).getRequest();
        //当前登入用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if(StringUtils.isNotBlank(mustRole)){
            UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            if(userRoleEnum == null){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            String userRole = loginUser.getUserRole();
            //如果被封号直接拒绝
            if(UserRoleEnum.BAN.equals(userRoleEnum)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //必须有管理员权限
            if(UserRoleEnum.ADMIN.equals(userRoleEnum)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        return joinPoint.proceed();
    }
}
