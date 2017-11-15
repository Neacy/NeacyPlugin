
# 如何使用

##首先
在目录`NeacyPlugin/app/build.gradle`  中加入plugin声明 `apply plugin: com.neacy.plugin.NeacyPlugin`

##标记注解 

Neacyprotocol：标记跳转协议  
NeacyCost    ：标记方法耗时

```java
@NeacyProtocol("Neacy://neacymodule/NeacyModuleActivity")
public class NeacyModuleActivity extends AppCompatActivity {
  
    @Override  
    @NeacyCost("MainActivity.onCreate")  
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.neacy_module_layout);  
    }  
}
```

##通过plugin生成的代码

生成路由表：
<img src="./images/gradle_transform.png" width="600" height="300">

插入的方法耗时统计：
<img src="./images/gradle_cost.png" width="600" height="300">

##Router调用
轻松实现协议跳转：
```java
Bundle bundle = new Bundle();
bundle.putString("neacy", "Neacy from Intent");
NeacyRouterManager.getInstance().startIntent(TestActivity.this, "Neacy://neacymodule/NeacyModuleActivity", bundle);
```

# Thanks

感谢巴掌CostTime：  
<https://github.com/JeasonWong/CostTime/>


# License
Null...