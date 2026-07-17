package com.liskovsoft.smartyoutubetv2.tv.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.linkin.base.debug.logger.L;
import com.linkin.base.utils.AppUtils;
import com.linkin.base.utils.FileUtils;
import com.linkin.base.utils.LogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackageUtils2 {
    private static final String TAG = "#PackageUtils2";
    private static final String INTENT_ACTION_PACKAGE = "android.intent.action.INSTALL_PACKAGE";
    private static final String APK_MIMETYPE = "application/vnd.android.package-archive";

    public static boolean installNormal3(Context context, String filePath) {
        Log.d(TAG, "filePath = " + filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            L.e(TAG, "安装包不存在，无法安装");
            return false;
        }
        if (!(file.canRead() && file.canWrite() && file.canExecute())) {
            L.i(TAG, "修改apk文件权限...");
            FileUtils.changeFileModeRecursively(context, file);
        }
        ApkInstaller.requestPackageInstallsPermission(context);
        L.d(TAG, filePath + ", rwx = " + file.canRead() + " " + file.canWrite() + " " + file.canExecute());
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_PACKAGE);
        intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(
                FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider",
                        new File(filePath)), APK_MIMETYPE);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            L.w(TAG, "installNormal：跳转系统安装异常 \n" + Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    public static boolean installNormal(Context context, String apkPath) {
        File file = new File(apkPath.trim());
        if (!file.exists()) {
            return false;
        }

        if (ApkInstaller.canRequestPackageInstalls(context)) {
            ApkInstaller.installApk(context, apkPath);
//            ApkInstaller.installApkWithoutPermissionCheck(context, apkPath);
        } else {
//            ApkInstaller.installApk(context, apkPath);
            ApkInstaller.installApkWithoutPermissionCheck(context, apkPath);
            ApkInstaller.requestPackageInstallsPermission(context);
//            if(context instanceof  Activity) {
//                ApkInstaller.requestPackageInstallsPermission(context, 1001);
//            } else {
//                LogUtils.d("packageutils", "context不是Activity类型");
//                return false;
//            }
        }
        return true;
    }

    public static boolean installNormal2(Context context, String apkPath) {
        try{
            LogUtils.d("wyltest", context.getPackageName() + "----" + context.getPackageResourcePath());
            int i = 1 / 0;
        }catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(apkPath.trim());
        if (!file.exists()) {
            return false;
        }

        // 修改文件权限（可选）
        changeFileModeRecursively("775", file);

        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);

        // 必须加这个，保证非 Activity context 可以启动
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = AppUtils.getFileUri(context, file.getPath()); // 通过 FileProvider 获取 URI
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");

            // 授权目标应用临时读取 URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // 显式授权目标应用访问 URI
            ClipData clip = ClipData.newUri(context.getContentResolver(), "apk", apkUri);
            intent.setClipData(clip);
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        LogUtils.d("wyltest", context instanceof Activity? "is activity" : "is not activity");
        LogUtils.d("wyltest", "context class = " + context.getClass().getName());
        context.startActivity(intent);
        return true;
    }


    public static void changeFileModeRecursively(String mode, File file) {
        for (File f : getParents(file)) {
            FileUtils.changeFileMode(mode, f);
        }
    }



    private static List<File> getParents(File file) {
        String abs = file.getAbsolutePath();
        String[] paths = abs.split(File.separator);
        for (int i = 0; i < paths.length; i++) {
            paths[i] = File.separator + paths[i];
        }
        List<File> parents = new ArrayList<>();
        for (int i = 0; i < paths.length; i++) {
            String path = "";
            for (int j = 0; j <= i; j++) {
                path = path + paths[j];
            }
            parents.add(new File(path));
        }
        return parents;
    }
}
