package com.neacy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记方法耗时
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/15
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NeacyCost {
    String value();
}
