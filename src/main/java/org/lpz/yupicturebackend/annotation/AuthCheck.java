package org.lpz.yupicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Target用来指定注解可以应用的程序元素类型
 * ElementType.METHOD表示这个注解只能应用于方法上
 * @Retention用来指定注解的保留策略，即注解在哪个生命周期内有效
 * RetentionPolicy.RUNTIME表示注解在运行时仍然保留，可以通过反射获取
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须有某个角色
     * @return
     */
    String mustRole() default "";

}
