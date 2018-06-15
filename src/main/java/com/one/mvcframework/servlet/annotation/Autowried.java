package com.one.mvcframework.servlet.annotation;

import java.lang.annotation.*;

/**
 * Created by huangyifei on 2018/6/12.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowried {
    String value() default "";
}
