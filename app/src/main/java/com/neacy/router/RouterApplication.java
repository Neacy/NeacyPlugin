package com.neacy.router;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/13
 */
public class RouterApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }
}
