package com.liskovsoft.smartyoutubetv2.tv.ui.search;

import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.linkin.base.utils.StringUtils;
import com.linkin.base.version.VManager;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.bean.AuthorizeRequest;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.bean.AuthorizeResult;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.manager.AuthorizeManager;
import com.vsoontech.base.http.inter.IHttpObserver;
import com.vsoontech.base.http.request.error.HttpError;
import android.os.Process;

/**
 * 授权应用
 *
 * @author fire
 */
public class AuthActivity extends AppCompatActivity {
    public static final int REQUEST_LICE_COE = 1001;
    public static final int RESULT_CODE = 1;
    private static final String TAG = "#AuthActivity";

    public static final int ERROR_EMPTY = 1000;
    public static final String ERROR_CODE = "error_code";

    private AuthorizeManager mAuthorizeManager;
    public static boolean isStart;

    TextView tvTip;
    TextView tvInfo;
    TextView tvStbInfo;
    Button btnRetry;
    LinearLayout llDialog;
    LinearLayout llLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorize);
        Log.d(TAG, "onCreate");
        mAuthorizeManager = AuthorizeManager.getInstance(this);
        initView();
        doCheck();
        VManager.getInstance().check(this);

    }



    @Override
    protected void onStart() {
        super.onStart();
        isStart = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStart = false;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CODE);
        super.onBackPressed();
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    @Override
    public void finish() {
        super.finish();
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    private void initView() {
        tvTip = (TextView) findViewById(R.id.tv_tip);
        tvInfo = (TextView) findViewById(R.id.tv_info);
        tvStbInfo = (TextView) findViewById(R.id.tv_stb_info);
        StringBuilder builder = new StringBuilder();
        builder.append("O:").append(mAuthorizeManager.getOmodel()).append(" ");
        builder.append("I:").append(mAuthorizeManager.getVendorID()).append(" ");
        String netMac = mAuthorizeManager.getNetMac();
        String wifiMac = mAuthorizeManager.getWifiMac();
        if (!mAuthorizeManager.isErrorMac(netMac)) {
            builder.append("M1:").append(netMac);
        } else if (!mAuthorizeManager.isErrorMac(wifiMac)) {
            builder.append("M2:").append(wifiMac);
        } else {
            builder.append("M1:").append(netMac);
        }
        builder.append(" ");
        builder.append("V:").append(com.liskovsoft.smartyoutubetv2.tv.BuildConfig.VERSION_NAME);

        tvStbInfo.setText(builder);

        btnRetry = (Button) findViewById(R.id.btn_retry);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doCheck();
            }
        });

        llDialog = (LinearLayout) findViewById(R.id.ll_dialog);
        llLoading = (LinearLayout) findViewById(R.id.ll_loading);
    }

    private void doCheck() {
        String omodel = mAuthorizeManager.getOmodel();
        String vendorID = mAuthorizeManager.getVendorID();
        if (StringUtils.isEmpty(omodel) || StringUtils.isEmpty(vendorID)) {
            showErrorCode(ERROR_EMPTY);
            return;
        }
        doRequest();
    }

    private void doRequest() {
        llDialog.setVisibility(View.GONE);
        llLoading.setVisibility(View.VISIBLE);

        new AuthorizeRequest(this).execute(new IHttpObserver() {
            @Override
            public void onHttpSuccess(String s, Object o) {
                AuthorizeResult mAuthorizeResult = (AuthorizeResult) o;
                if (mAuthorizeResult.getCode() > 0) {
                    showErrorCode(mAuthorizeResult.getCode());
                } else {
                    mAuthorizeManager.setResult(mAuthorizeResult);
                    if (mAuthorizeManager.isAuth()) {
                        setResult(RESULT_CODE);
                        finish();
                    } else {
                        showErrorCode(mAuthorizeResult.getCode());
                    }
                }
            }

            @Override
            public void onHttpError(String taskId, int statusCode, HttpError error) {
                showErrorCode(statusCode);
            }

        }, AuthorizeResult.class);
    }

    private void showErrorCode(int code) {
        llDialog.setVisibility(View.VISIBLE);
        llLoading.setVisibility(View.GONE);
        if (code == ERROR_EMPTY) {
            tvInfo.setText(R.string.auth_empty_param);
        } else {
            String tip = getString(R.string.auth_error_tip) + ":" + code;
            tvInfo.setText(tip);
        }

        btnRetry.requestFocus();

    }
}
