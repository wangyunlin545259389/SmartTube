package com.liskovsoft.smartyoutubetv2.tv.util;

import com.linkin.base.utils.PropertyUtils;

/**
 * 属性
 * @author fire
 */
public class ProductUtils {
    public static String getOmodel() {
        return PropertyUtils.get("ro.product.omodel", "");
    }

    public static String getVendorIdNew() {
        return PropertyUtils.get("hw.yunos.vendorID", "");
    }
}
