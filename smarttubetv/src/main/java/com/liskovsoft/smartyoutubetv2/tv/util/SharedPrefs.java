package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.liskovsoft.smartyoutubetv2.tv.ui.main.MainApplication;


/**
 * @author wushixiong
 * @since 2019-07-22
 */
public class SharedPrefs {

    private static final String PRES_NAME = "app_manager";

    private static SharedPreferences mSharePrefs;

    private SharedPrefs() {
        mSharePrefs = MainApplication.getInstance().getSharedPreferences(PRES_NAME,
            Context.MODE_PRIVATE);
    }

    private static class SharePrefHolder {

        static final SharedPrefs INSTANCE = new SharedPrefs();
    }

    public static SharedPrefs instance() {
        return SharePrefHolder.INSTANCE;
    }

    public boolean putString(String key, String value) {
        return mSharePrefs.edit().putString(key, value).commit();
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String defValue) {
        return mSharePrefs.getString(key, defValue);
    }

    public void putBoolean(String key, boolean value) {
        mSharePrefs.edit().putBoolean(key, value).commit();
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSharePrefs.getBoolean(key, defValue);
    }

    public void putFloat(String key, float value) {
        mSharePrefs.edit().putFloat(key, value).commit();
    }

    public float getFloat(String key) {
        return getFloat(key, 0);
    }

    public float getFloat(String key, float def) {
        return mSharePrefs.getFloat(key, def);
    }

    public void putInt(String key, int value) {
        mSharePrefs.edit().putInt(key, value).commit();
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        return mSharePrefs.getInt(key, def);
    }

    public void putLong(String key, long value) {
        mSharePrefs.edit().putLong(key, value).commit();
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public long getLong(String key, long def) {
        return mSharePrefs.getLong(key, def);
    }

    public void remove(String key) {
        mSharePrefs.edit().remove(key).commit();
    }

    public void clear() {
        mSharePrefs.edit().clear().commit();
    }
}
