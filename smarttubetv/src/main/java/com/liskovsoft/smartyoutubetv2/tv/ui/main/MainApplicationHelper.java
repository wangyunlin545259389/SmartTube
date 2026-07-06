package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import com.linkin.base.app.BaseApplicationHelper;
import com.linkin.base.utils.FileUtils;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.auth.comm.WaServer;
import com.liskovsoft.smartyoutubetv2.tv.util.SharedPrefs;
import com.vsoontech.base.http.request.presenter.RequestBuilder;
import com.vsoontech.base.reporter.bean.ActionReportPage;

import java.io.File;

/**
 * @author kingpengLin
 * @e-mail linjinpeng@vsoontech.com
 * @since 2018/3/14.
 */
public class MainApplicationHelper extends BaseApplicationHelper {

    @Override
    protected RequestBuilder initHost() {
        String server = com.liskovsoft.smartyoutubetv2.tv.BuildConfig.FLAVOR.equals("auth") ? WaServer.AUTH_SERVER : WaServer.RELEASE_SERVER;
        return new RequestBuilder()
                .setDomainName(server)
                .setTestDomainName(WaServer.TEST_SERVER)
                .setOfflineDomainName(WaServer.DEBUG_SERVER)
                .setEnableHostFilter(true);
    }

    @Override
    public ActionReportPage initReportPage() {
        return new ActionReportPage(null);
    }

    @Override
    public boolean isBindVService() {
        return true;
    }

    @Override
    public void onChangeHostStatus(int hostStatus) {
        SharedPrefs.instance().clear();
        FileUtils.deleteDirectory(getContext().getFilesDir() + File.separator + "seri" + File.separator);
        super.onChangeHostStatus(hostStatus);
    }
}
