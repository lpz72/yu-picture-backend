package org.lpz.yupicturebackend.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.lpz.yupicturebackend.annotation.AuthCheck;
import org.lpz.yupicturebackend.exception.BusinessException;
import org.lpz.yupicturebackend.exception.ErrorCode;
import org.lpz.yupicturebackend.exception.ThrowUtils;
import org.lpz.yupicturebackend.model.enums.UserRoleEnum;
import org.lpz.yupicturebackend.model.entity.User;
import org.lpz.yupicturebackend.service.UserService;
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

    /**
     * 执行拦截
     * 使用 @Around 注解定义了一个环绕通知，拦截所有带有 @AuthCheck 注解的方法
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {

        String mustRole = authCheck.mustRole();
        // 获取当前线程绑定的HTTP请求对象（HttpServletRequest）
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 以下为：必须有该权限才通过
        // 获取当前登录用户具有的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，拒绝
        ThrowUtils.throwIf(userRoleEnum == null, ErrorCode.NO_AUTH_ERROR);

        // 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过权限校验，放行
        return joinPoint.proceed();

    }

}
