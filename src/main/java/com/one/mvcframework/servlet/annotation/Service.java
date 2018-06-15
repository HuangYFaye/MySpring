package com.one.mvcframework.servlet.annotation;

import java.lang.annotation.*;

/**
 * Created by huangyifei on 2018/6/12.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String value() default "";
}
