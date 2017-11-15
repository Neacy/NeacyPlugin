package com.neacy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记协议
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/6
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface NeacyProtocol {

    String value();
}

