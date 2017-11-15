package neacy.neacymodule2;

import android.util.ArrayMap;

/**
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/7
 */
public interface IntentProtocol {

    void doIntent(TestActivity activity, String protocol, ArrayMap<String, String> datas);
}
