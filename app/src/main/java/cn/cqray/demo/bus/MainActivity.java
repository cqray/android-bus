package cn.cqray.demo.bus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import cn.cqray.android.bus.Rxbus;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Rxbus.setDebug(true);

        // 注册Rxbus
        Rxbus.with(this)
                .of(String.class)
                .tag("666")
                .sticky()
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                });

        Rxbus.post("123");
    }
}
