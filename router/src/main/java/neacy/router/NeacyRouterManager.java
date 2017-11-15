package neacy.router;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * 管理路由表
 *
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/6
 */
public class NeacyRouterManager {

    private static class Holder {
        private static NeacyRouterManager INSTANCE = new NeacyRouterManager();
    }

    private ArrayMap<String, String> routers = new ArrayMap<>();

    private HashMap<String, String> mRouters = new HashMap<>();

    /**
     * 初始化路由
     */
    public void initRouter() {
        try {
            Class clazz = Class.forName("com.neacy.router.NeacyProtocolManager");
            Object newInstance = clazz.newInstance();
            Field field = clazz.getField("map");
            field.setAccessible(true);
            HashMap<String, String> temps = (HashMap<String, String>) field.get(newInstance);
            if (temps != null && !temps.isEmpty()) {
                mRouters.putAll(temps);
                Log.w("Jayuchou", "=== mRouters.Size === " + mRouters.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据协议找寻路由实现跳转
     */
    public void startIntent(Context context, String protocol, Bundle bundle) {
        if (TextUtils.isEmpty(protocol)) return;
        String protocolValue = mRouters.get(protocol);
        try {
            Class destClass = Class.forName(protocolValue);
            Intent intent = new Intent(context, destClass);
            if (bundle != null) {
                intent.putExtras(bundle);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(Context context) {
        try {
            InputStream pathis = context.getAssets().open("router.conf");
            Properties properties = new Properties();
            properties.load(pathis);
            Enumeration<Object> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();//协议
                String value = properties.getProperty(key).trim();
                Log.w("Jayuchou", "=== key === " + key);
                Log.w("Jayuchou", "=== value === " + value);
                routers.put(key, value);
            }
            pathis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static NeacyRouterManager getInstance() {
        return Holder.INSTANCE;
    }

    public <T> T create(Class<T> service) {

        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Context context = (Context) args[0];
                String protocolName = (String) args[1];
                ArrayMap<String, String> values = null;
                if (args.length > 2) {
                    values = (ArrayMap<String, String>) args[2];
                }
                String protocolValue = routers.get(protocolName);
                Intent intent = new Intent(context, Class.forName(protocolValue));
                if (values != null) {
                    Set<String> sets = values.keySet();
                    Bundle bundle = new Bundle();
                    for (String s : sets) {
                        bundle.putString(s, values.get(s));
                    }
                    intent.putExtras(bundle);
                }
                context.startActivity(intent);
                return null;
            }
        });
    }
}