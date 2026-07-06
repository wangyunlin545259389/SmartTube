package com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.manager;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.linkin.base.utils.FileUtils;
import com.linkin.base.utils.LogUtils;
import com.linkin.base.utils.MD5Utils;
import com.linkin.base.utils.NetworkUtils;
import com.linkin.base.utils.PackageUtils;
import com.linkin.base.utils.StringUtils;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.bean.AuthorizeResult;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.AuthActivity;
import com.liskovsoft.smartyoutubetv2.tv.util.ProductUtils;
import com.liskovsoft.smartyoutubetv2.tv.util.RandomUtils;

import java.io.File;
import java.util.Locale;

/**
 * 授权管理
 *
 * @author fire
 */
public class AuthorizeManager {
    private static final String TAG = AuthorizeManager.class.getSimpleName();
    private static AuthorizeManager mInstance;
    private static final String VERIFY_KEY = "Verify.key";
    private static final String FOCUS_KEY = "Focus.key";
    private static Context mContext;
    private String mOmodel;
    private String mVendorID;
    private String mNetMac;
    private String mWifiMac;

    private String mKey;
    private String mVerify;


    public static synchronized AuthorizeManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AuthorizeManager(context);
        }

        return mInstance;
    }

    private AuthorizeManager(Context context) {
//        mContext = MainApplication.getContext();
        mContext = context;
        init();
    }

    private void init() {

        mOmodel = ProductUtils.getOmodel();
        mVendorID = ProductUtils.getVendorIdNew();
        String netMac = NetworkUtils.getEthernetMac(mContext);
        String wifiMac = NetworkUtils.getWifiMac(mContext);
        if (netMac == null) {
            netMac = "02:00:00:00:00:00";
        }
        if (wifiMac == null) {
            wifiMac = "02:00:00:00:00:00";
        }
        mNetMac = netMac.toLowerCase(Locale.ROOT);
        mWifiMac = wifiMac.toLowerCase(Locale.ROOT);
        initKey();
        initVerify();
        LogUtils.i("#Auth", "mKey=" + mKey);
    }

    private void initKey() {
        File file = new File(mContext.getFilesDir(), FOCUS_KEY);
        if (file.exists()) {
            String str = FileUtils.readFile(file.getAbsolutePath());
            mKey = str.replaceAll("\n", "");
        }
        if (StringUtils.isEmpty(mKey)) {
            mKey = RandomUtils.generateString(32);
            FileUtils.writeFile(file.getAbsolutePath(), mKey, false);
        }
    }

    private void initVerify() {
        File file = new File(mContext.getFilesDir(), VERIFY_KEY);
        if (file.exists()) {
            String str = FileUtils.readFile(file.getAbsolutePath());
            mVerify = str.replaceAll("\n", "");
        }
    }

    public String getOmodel() {
        if (mOmodel == null) {
            return "";
        }
        return mOmodel;
    }

    public String getVendorID() {
        if (mVendorID == null) {
            return "";
        }
        return mVendorID;
    }

    public String getNetMac() {
        return mNetMac;
    }

    public String getWifiMac() {
        return mWifiMac;
    }

    public boolean isErrorMac(String str) {
        if (StringUtils.isEmpty(str)) {
            return true;
        }
        return "00:00:00:00:00:00".equals(str) || "02:00:00:00:00:00".equals(str);
    }

    public boolean isAuth() {
//        if (StringUtils.isEmpty(mVerify)) {
//            if (checkLauncher()) {
//                return true;
//            }
//            return false;
//        }
        return isAuthCheck(mKey, mVerify);
    }

    private boolean checkLauncher() {
        if (!PackageUtils.checkApkExist(mContext, "com.linkin.launchery")) {
            LogUtils.i(TAG, "桌面不存在");
            return false;
        }
        Uri uri = Uri.parse("content://com.linkin.provider.AuthProvider/auth");
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            LogUtils.i(TAG, "cursor = null");
            return false;
        }
        if (!cursor.moveToFirst()) {
            LogUtils.i(TAG, "cursor = moveToFirst");
            return false;
        }
        int count = cursor.getCount();
        LogUtils.i(TAG, "count = " + count);
        @SuppressLint("Range") String key = cursor.getString(cursor.getColumnIndex("key"));
        @SuppressLint("Range") String verify = cursor.getString(cursor.getColumnIndex("verify"));
        if (isAuthCheck(key, verify)) {
            mKey = key;
            mVerify = verify;
            //更新缓存key
            File file = new File(mContext.getFilesDir(), FOCUS_KEY);
            FileUtils.writeFile(file.getAbsolutePath(), mKey, false);
            File verifyFile = new File(mContext.getFilesDir(), VERIFY_KEY);
            FileUtils.writeFile(verifyFile.getAbsolutePath(), mVerify, false);
            return true;
        }
        return false;
    }

    private boolean isAuthCheck(String key, String verify) {
        String mac = isErrorMac(mNetMac) ? mWifiMac : mNetMac;
        String str = mOmodel + key.substring(2, 4) + mVendorID + key.substring(6, 8) + mac;
        String md5Str = MD5Utils.getMD5String(str);
        if (md5Str.equals(verify)) {
            return true;
        }
        return false;
    }

    public String getKey() {
        return mKey;
    }

    public void setResult(AuthorizeResult mAuthorizeResult) {
        mVerify = mAuthorizeResult.getVerify();
        if (isAuthCheck(mKey, mVerify)) {
            File file = new File(mContext.getFilesDir(), VERIFY_KEY);
            FileUtils.writeFile(file.getAbsolutePath(), mVerify, false);
        }
    }

    public void openAuthActivity(int code) {
        Intent intent = new Intent(mContext, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AuthActivity.ERROR_CODE, code);
        mContext.startActivity(intent);
    }
}
