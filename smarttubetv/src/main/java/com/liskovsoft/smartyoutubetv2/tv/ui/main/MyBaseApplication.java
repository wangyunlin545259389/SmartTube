package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.multidex.MultiDex;

import com.linkin.base.app.BaseApplication;
import com.linkin.base.debug.logger.L;


/**
 * @author fire & keith
 * @since 2015-07-29
 */
public abstract class MyBaseApplication extends BaseApplication {

    private static final String TAG = "MyBaseApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        L.i(TAG, activity.getLocalClassName() + " : onActivityCreated()  do nothing");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        L.i(TAG, activity.getLocalClassName() + " : onActivityStarted do nothing");
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        L.i(TAG, activity.getLocalClassName() + ": onActivityResumed do nothing");
    }
}
