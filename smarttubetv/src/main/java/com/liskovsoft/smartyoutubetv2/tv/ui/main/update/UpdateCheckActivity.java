package com.liskovsoft.smartyoutubetv2.tv.ui.main.update;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.linkin.base.version.VManager;
import com.linkin.base.version.bean.AppVInfo;
import com.linkin.base.version.listener.impl.PreCheckResultListener;
import com.linkin.base.version.listener.impl.PreVListener;

import java.io.File;

/**
 * 透明Activity，用于执行版本检测并直接弹出升级对话框
 */
public class UpdateCheckActivity extends Activity {
    private Activity activity;
    /**
     * 一行代码启动检测（无需 Service）
     */
    public static void startCheck(Context context) {
        Log.i("wyltest2", "startCheck");
        Intent intent = new Intent(context, UpdateCheckActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("wyltest2", "UpdateCheckActivity");
        // 立即执行检测
        checkUpdate(this);
    }
    public  void startCheck(Activity activity) {
        this.activity = activity;
        checkUpdate(activity);
    }
    private void checkUpdate(Activity activity) {
        Log.i("wyltest2", "checkUpdate");
        PreVListener preVListener = new PreVListener(activity) {
            @Override
            public void showHint(Activity activity, AppVInfo appVInfo, boolean isCancelable,
                                 File file) {
                showUpdateDialog2(appVInfo);
            }
        };

        VManager.getInstance().check(new PreCheckResultListener(this.activity , preVListener) {
            @Override
            public void onNo(int statusCode) {
                // 无更新，安静关闭
                finish();
            }
        });
    }

    private void showUpdateDialog2(AppVInfo appVInfo) {
        UpdateDialog dialog = new UpdateDialog(this.activity , appVInfo);
        // 根据是否强制升级设置取消性
        boolean force = com.linkin.base.utils.PackageUtils.getVersionCode(this.activity ) < appVInfo.minForceUpdateVersion;
        dialog.setCancelable(!force);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        dialog.show();
    }
}