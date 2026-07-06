package com.liskovsoft.smartyoutubetv2.tv.ui.main.update;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.linkin.base.version.bean.AppVInfo;

/**
 * 用于显示升级弹窗的透明 Activity
 */
public class UpdateNotifyActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取传递的 AppVInfo
        AppVInfo appVInfo = getIntent().getParcelableExtra("appVInfo");
        if (appVInfo == null) {
            finish();
            return;
        }

        // 显示 UpdateDialog
        UpdateDialog dialog = new UpdateDialog(this, appVInfo);
        dialog.setCancelable(true);  // 根据需求设置
        dialog.show();

        // 注意：对话框关闭时，Activity 可以自行 finish，
        // 但为了不干扰用户，可在对话框 dismiss 时调用 finish。
        // 可通过自定义 UpdateDialog 或监听 dismiss 事件实现。
        // 这里简单处理：在 dialog 的 onDismiss 中 finish（需修改 UpdateDialog）
        // 或者直接使用一个监听器，但为了快速演示，我们采用如下方式：
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }
}