package neacy.router;

import java.util.HashMap;

/**
 * 耗时统计
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/15
 */
public class NeacyCostManager {

    public static HashMap<String, Long> mStartTimes = new HashMap<>();
    public static HashMap<String, Long> mEndTimes = new HashMap<>();

    public static void addStartTime(String key, long time) {
        mStartTimes.put(key, time);
    }

    public static void addEndTime(String key, long time) {
        mEndTimes.put(key, time);
    }

    public static void startCost(String key) {
        long start = mStartTimes.get(key);
        long end = mEndTimes.get(key);
        System.out.println("----->>>>>> method : " + key + " cost " + (end - start) + " ms <<<<<<-----");
    }
}
