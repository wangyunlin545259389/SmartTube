package com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.bean;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.manager.AuthorizeManager;
import com.vsoontech.base.http.request.GetRequest;

/**
 * 授权请求
 *
 * @author fire
 */
public class AuthorizeRequest extends GetRequest {

    private AuthorizeManager mAuthorizeManager;

    public AuthorizeRequest(Context context) {
        mAuthorizeManager = AuthorizeManager.getInstance(context);
        setMaxRetry(1);
        setParamObject(new AuthParam());
    }

    @Override
    public String getApi() {
        return "/v2/app/auth";
    }

    @Override
    public boolean isHttps() {
        return true;
    }


    @Override
    protected String getSecondDomainName() {
        return "auth2";
    }

//    @Override
//    public synchronized String reqUrl() {
//        String reqUrl = super.reqUrl();
//        String[] arr = reqUrl.split("/");
//        String domain = arr[2];
//        return reqUrl.replace(domain, "auth2.vsoontech.com");
//    }

    @Override
    public synchronized String reqUrl() {
        String reqUrl = super.reqUrl();
        String[] arr = reqUrl.split("/");
        String domain = arr[2];
        //return reqUrl.replace(domain, "auth2.vsoontech.com");
        return reqUrl.replace(domain, "auth2.micnest.com");
    }

    private class AuthParam {
        String key;
        String mac;
        String wifiMac;
        String vendorId;
        String model;
        String pkg;

        AuthParam() {
            key = mAuthorizeManager.getKey();
            mac = mAuthorizeManager.getNetMac();
            wifiMac = mAuthorizeManager.getWifiMac();
            vendorId = mAuthorizeManager.getVendorID();
            model = mAuthorizeManager.getOmodel();
            pkg = com.liskovsoft.smartyoutubetv2.tv.BuildConfig.APPLICATION_ID;
        }
    }
}
