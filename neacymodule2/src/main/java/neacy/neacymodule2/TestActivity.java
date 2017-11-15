package neacy.neacymodule2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.view.View;

import com.neacy.annotation.NeacyProtocol;

import neacy.router.NeacyRouterManager;

/**
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/7
 */
@NeacyProtocol("Neacy://neacymodule2/TestActivity")
public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_main_);

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("neacy", "Neacy from Intent");
                NeacyRouterManager.getInstance().startIntent(TestActivity.this, "Neacy://neacymodule/NeacyModuleActivity", bundle);


//                ArrayMap<String, String> maps = new ArrayMap<>();
//                maps.put("neacy", "neacy");
//                NeacyRouterManager.getInstance().create(IntentProtocol.class).doIntent(TestActivity.this, "/neacymodule", maps);
            }
        });
    }
}
