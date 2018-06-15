package com.one.mvcframework.servlet.annotation;

import java.lang.annotation.*;

/**
 * Created by huangyifei on 2018/6/12.
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
