package com.liskovsoft.smartyoutubetv2.tv.ui.main.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.linkin.base.utils.PackageUtils;
import com.linkin.base.version.VManager;
import com.linkin.base.version.bean.AppVInfo;
import com.linkin.base.version.listener.VListener;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.comm.SPKey;
import com.liskovsoft.smartyoutubetv2.tv.util.SharedPrefs;


/**
 * 升级弹窗
 *
 * @author assistne
 * @since 2019/1/16
 */
public class UpdateDialog extends AlertDialog {

    private TextView mDescTxt;
    private TextView mCancelBtn;
    private TextView mOkBtn;

    private AppVInfo mAppVInfo;
    private VListener mListener;
    private boolean mIsForce = false;
    private boolean mIsSave = false;

    public UpdateDialog(Context context, AppVInfo info) {
        this(context, info, false);
    }

    public UpdateDialog(final Context context, AppVInfo info, boolean isSave) {
        super(context);
        mAppVInfo = info;
        mIsSave = isSave;
        final Activity activity = (Activity) context;
        mListener = new VListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onProgress(int progress) {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onSucceed(String filePath) {
                PackageUtils.install(activity, filePath);
            }

            @Override
            public void onFailed(String reason) {
            }
        };
        Window window = this.getWindow();
        if (window != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        setOwnerActivity(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_update);

        // ---- 初始化视图（替代 ButterKnife） ----
        initViews();

        initView();
    }

    /**
     * 使用 findViewById 初始化视图
     */
    private void initViews() {
        mDescTxt = findViewById(R.id.update_dialog_desc_txt);
        mCancelBtn = findViewById(R.id.update_dialog_cancel_btn);
        mOkBtn = findViewById(R.id.update_dialog_ok_btn);

        // 设置点击监听（替代 @OnClick）
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUpdateClick();
            }
        });
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelClick();
            }
        });
    }

    private void initView() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mAppVInfo.desc.length; i++) {
            builder.append(i + 1);
            builder.append(".");
            builder.append(mAppVInfo.desc[i]);
            builder.append(";");
            if (i != mAppVInfo.desc.length - 1) {
                builder.append("\n");
            }
        }
        mDescTxt.setText(builder.toString());

        mIsForce = PackageUtils.getVersionCode(getContext()) < mAppVInfo.minForceUpdateVersion;
        setCancelable(!mIsForce);
        mCancelBtn.setVisibility(mIsForce ? View.GONE : View.VISIBLE);
        mOkBtn.requestFocus();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void onUpdateClick() {
        VManager.getInstance().start(mListener, mAppVInfo);
    }

    public void onCancelClick() {
        if (mIsSave && mAppVInfo != null
                && mAppVInfo.minForceUpdateVersion < PackageUtils.getVersionCode(getContext())) {
            SharedPrefs.instance().putInt(SPKey.UPDATE_COUNT,
                    SharedPrefs.instance().getInt(SPKey.UPDATE_COUNT, 0) + 1);
        }
        dismiss();
    }
}