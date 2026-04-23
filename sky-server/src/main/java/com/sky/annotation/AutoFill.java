package com.sky.annotation;

import com.sky.enumeration.OperationType;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识需要自动填充的属性
 */
@Target(ElementType.METHOD) //指定这个只能加在方法上面
@Retention(RetentionPolicy.RUNTIME) //保留到运行时
public @interface AutoFill {
    //数据库操作类型,update、insert
    OperationType value();
}
