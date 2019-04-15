package com.example.administrator.hookandroid.Activity;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.administrator.hookandroid.R;
import com.example.administrator.hookandroid.Util.UmengUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;
import java.util.Map;

public class SecondActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);


        //创建一个启动其他Activity的Intent
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this , 0, intent, 0);
        //创建一个Notification
        Notification notify = new Notification();
        //为Notification设置图标，该图标显示在状态栏
//        notify.icon = R.drawable.abc_ic_go;     // 不能少
        //为Notification设置文本内容，该文本会显示在状态栏
        notify.tickerText = "启动其他Activity的通知";
        //为Notification设置发送时间
        notify.when = System.currentTimeMillis();
        //为Notification设置声音
        notify.defaults = Notification.DEFAULT_SOUND;
        //为Notification设置默认声音、默认振动、默认闪光灯
        notify.defaults = Notification.DEFAULT_ALL;
        //设置事件信息
//        notify.setLatestEventInfo(this, "普通通知", "点击查看", pi);
        //获取系统的NotificationManager服务
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //发送通知
        notificationManager.notify(123, notify);



        Button button = (Button) findViewById(R.id.button_jump);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int duration = 12000; //开发者需要自己计算音乐播放时长
                Map<String, String> map_value = new HashMap<String, String>();
                map_value.put("type", "popular");
                map_value.put("artist", "JJLin");
                MobclickAgent.onEventValue(SecondActivity.this, "music", map_value, duration);

                SecondActivity.this.finish();
            }
        });
    }

    public void onResume() {
        super.onResume();

        UmengUtil.onResumeToActivity(this);

        MobclickAgent.onPageStart("SecondActivity"); //统计页面(仅有Activity的应用中SDK自动调用，不需要单独写。"SplashScreen"为页面名称，可自定义)
        MobclickAgent.onResume(this);          //统计时长
    }

    public void onPause() {
        super.onPause();

        UmengUtil.onPauseToActivity(this);

        MobclickAgent.onPageEnd("SecondActivity"); // （仅有Activity的应用中SDK自动调用，不需要单独写）保证 onPageEnd 在onPause 之前调用,因为 onPause 中会保存信息。"SplashScreen"为页面名称，可自定义
        MobclickAgent.onPause(this);
    }
}
