package com.liskovsoft.smartyoutubetv2.common.app.views;

import android.content.Intent;

public interface KTVView {
    Intent getNewIntent();
    void finishView();
}
