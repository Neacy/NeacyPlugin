package neacy.neacymodule;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.neacy.annotation.NeacyProtocol;

/**
 * @author yuzongxu <yuzongxu@xiaoyouzi.com>
 * @since 2017/11/7
 */
@NeacyProtocol("Neacy://neacymodule/NeacyModuleActivity")
public class NeacyModuleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.neacy_module_layout);

        Log.w("Jayuchou", getIntent().getStringExtra("neacy"));
    }
}
