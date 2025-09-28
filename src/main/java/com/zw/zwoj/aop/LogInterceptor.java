package com.zw.zwoj.aop;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class LogInterceptor {

    @Around("execution(* com.zw.zwoj.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        //计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //获取请求路径
        RequestAttribute requestAttribute = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes)requestAttribute).getRequest();

        //生成请求唯一id
         String requestId = UUID.randomUUID().toString();
         String url = httpServletRequest.getRequestURL();

         //获取请求参数
        Object[] args = joinPoint.getArgs();
        String reqParam = "[" + StringUtils.join(args, ",") + "]";
        //输出日志
        log.info("request start , id: {}, path: {}, ip: {}, params: {}", requestId, url, httpServletRequest.getRemoteHost(), reqParam);

        //执行原方法
        Object result = joinPoint.proceed();
        //输出响应日志
        stopWatch.stop();
        long tatalTimeMillis = stopWatch.getTotalTimeMillis();

        log.info("request end, id:{}, cost: {} ms", requestId, tatalTimeMillis);
        return result;

    }
}
