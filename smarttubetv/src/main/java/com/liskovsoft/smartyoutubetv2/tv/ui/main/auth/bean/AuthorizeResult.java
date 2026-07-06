package com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.bean;

/**
 * 授权返回
 * @author fire
 */
public class AuthorizeResult {
    private int code;
    private String verify;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getVerify() {
        return verify;
    }

    public void setVerify(String verify) {
        this.verify = verify;
    }
}
