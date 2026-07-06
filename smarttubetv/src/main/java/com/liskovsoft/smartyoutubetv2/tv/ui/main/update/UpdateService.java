package com.liskovsoft.smartyoutubetv2.tv.ui.main.update;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


public class UpdateService extends Service {

    /**
     * 一行代码调起检测流程
     */
    public static void startCheck(Context context) {
        context.startService(new Intent(context, UpdateService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("wyltest", "onStartCommand");
        // 启动透明Activity执行检测
        Intent activityIntent = new Intent(this, UpdateCheckActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);
        // 服务任务完成，立即停止
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}