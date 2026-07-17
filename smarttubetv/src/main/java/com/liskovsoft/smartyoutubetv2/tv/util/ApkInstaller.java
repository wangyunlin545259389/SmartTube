package com.liskovsoft.smartyoutubetv2.tv.util;

import static com.linkin.base.utils.AppUtils.startAty;

import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.linkin.base.utils.LogUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;

public class ApkInstaller {
    private static final String TAG = "#ApkInstaller";
    private static final int REQUEST_CODE_TAKE_PHOTO = 0x110;
    private static final int REQ_PERMISSION_CODE_SDCARD = 0X111;
    private static final int REQ_PERMISSION_CODE_TAKE_PHOTO = 0X112;
    private static final String AUTHORITY_APK = "com.android.settings.files";
    private static final String APK_MIMETYPE = "application/vnd.android.package-archive";
    private static final String INTENT_ACTION_PACKAGE = "android.intent.action.INSTALL_PACKAGE";
//    /**
//     * 安装 APK 文件
//     * @param context 上下文
//     * @param apkPath APK 源路径（可以是 SD 卡或 U 盘）
//     */
//    public static boolean installApk2(Context context, String apkPath) {
//        //检查读写
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMISSION_CODE_SDCARD);
//        } else {
//            //有读写 查是否 未知来源
////            installApk();
//            checkAndroidO();
//        }
//    }

    //    private static void openIntent(Intent intent) {
//        try {
//            startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            Toast.makeText(BaseFileActivity.this, getString(R.string.no_file_support),
//                    Toast.LENGTH_SHORT).show();
//            LogTool.singleton()
//                    .debug("not find activity to open this type : " + intent.getType()
//                            + ", message :" + e.getMessage());
//        }
//    }
    public static boolean installApk(Context context, String filePath) {
        LogUtils.d(TAG, "filePath = " + filePath);
        Intent intent = new Intent();
        intent.setAction(INTENT_ACTION_PACKAGE);
        intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());


        if (Build.VERSION.SDK_INT >= 24) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setDataAndType(
                    FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider",
                            new File(filePath)), APK_MIMETYPE);
        } else {
            intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
        }

        startAty(context, intent, true);


//        try {
//            File file = new File(filePath);
//            Uri apkUri = FileProvider.getUriForFile(context,
//                    context.getPackageName() + ".fileprovider", file);
//
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(apkUri, APK_MIMETYPE);
//
//            // 关键：传入文件管理器的包名
//            intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());
//
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//            context.startActivity(intent);
//            return true;
//
//        } catch (Exception e) {
//            LogUtils.e(TAG, "Install error: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }


//        File file = new File(filePath);
//        if (!file.exists()) {
//            L.e(TAG, "安装包不存在，无法安装");
//            return false;
//        }
//        if (!(file.canRead() && file.canWrite() && file.canExecute())) {
//            L.i(TAG, "修改apk文件权限...");
//            FileUtils.changeFileModeRecursively(context, file);
//        }
//        L.d(TAG, filePath + ", rwx = " + file.canRead() + " " + file.canWrite() + " " + file.canExecute());
//
//        Intent intent = new Intent();
//        intent.setAction(INTENT_ACTION_PACKAGE);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());
//        try {
//            startAty(context, AppUtils
//                    .getFileUriIntent(context, intent, "application/vnd.android.package-archive",
//                            filePath, false), true);
//        } catch (Exception e) {
//            L.w(TAG, "installNormal：跳转系统安装异常 \n" + Log.getStackTraceString(e));
//            return false;
//        }
        return true;
    }

    /**
     * 安装 APK 文件
     * @param context 上下文
     * @param apkPath APK 源路径（可以是 SD 卡或 U 盘）
     */
    public static boolean installApk3(Context context, String apkPath) {
        File srcFile = new File(apkPath.trim());
        if (!srcFile.exists()) return false;

        // 获取公共下载目录
        File publicDir = new File(Environment.getExternalStorageDirectory(), "Download");
        if (!publicDir.exists()) publicDir.mkdirs();
        File destFile = new File(publicDir, srcFile.getName());

        // 拷贝文件到公共目录
        copyFile(srcFile, destFile);

        // 创建安装 Intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider",
                    destFile);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            apkUri = Uri.fromFile(destFile);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                openAppDetails(context, context.getPackageName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }
    public static boolean installApkWithoutPermissionCheck(Context context, String apkPath) {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("EXTRA_CALLING_PACKAGE", context.getPackageName());
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider",
                        apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }

            // 关键：添加这个flag，告诉系统我们来自已知来源
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            // 如果没有安装程序，尝试使用PackageInstaller
//            return installWithPackageInstaller(context, apkFile);
            return false;
        } catch (SecurityException e) {
            // 权限问题，尝试修复
            LogUtils.d("wyltest", "安装失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            LogUtils.e("wyltest", "未知错误: " + e.getMessage());
            return false;
        }
    }
    private static boolean copyFile(File src, File dst) {
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = new java.io.FileInputStream(src);
            out = new java.io.FileOutputStream(dst);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private static void openAppDetails(Context context, String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Android 8+ 检查是否可以安装未知来源应用
     */
    public static boolean canRequestPackageInstalls(Context context) {
//        checkInstallSource(context);
//        debugPermissionStatus(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean b = context.getPackageManager().canRequestPackageInstalls();
            LogUtils.d("wyltest", "b = " + b);
            return b;
        }
        return true;
    }
    public static void debugPermissionStatus(Context context) {
        LogUtils.d("wyltest", "=== 权限状态诊断 ===");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. 检查标准方法
            boolean canInstall = context.getPackageManager().canRequestPackageInstalls();
            LogUtils.d("wyltest", "canRequestPackageInstalls: " + canInstall);

            // 2. 检查AppOps权限（需要系统权限）
            try {
                AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow("android:request_install_packages",
                        android.os.Process.myUid(),
                        context.getPackageName());
                LogUtils.d("wyltest", "AppOps检查结果: " + mode + " (0=允许, 1=拒绝, 2=默认)");
            } catch (Exception e) {
                LogUtils.d("wyltest", "AppOps检查异常: " + e.getMessage());
            }

            // 3. 检查安装源设置
            try {
                ContentResolver resolver = context.getContentResolver();
                int installSources = Settings.Secure.getInt(resolver,
                        Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
                LogUtils.d("wyltest", "全局未知来源设置: " + installSources);
            } catch (Exception e) {
                // Android 8+ 已废弃此设置
            }

            // 4. 尝试通过反射获取更详细信息
            try {
                Class<?> pmClass = Class.forName("android.content.pm.PackageManager");
                Method method = pmClass.getDeclaredMethod("getInstallReason", String.class);
                int installReason = (int) method.invoke(context.getPackageManager(),
                        context.getPackageName());
                LogUtils.d("wyltest", "安装原因: " + installReason);
            } catch (Exception e) {
                LogUtils.d("wyltest", "反射检查失败: " + e.getMessage());
            }
        }
    }
    public static void checkInstallSource(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);

            LogUtils.d("wyltest", "=== 应用安装信息 ===");
            LogUtils.d("wyltest", "安装来源: " + info.applicationInfo);
            LogUtils.d("wyltest", "首次安装时间: " + new Date(info.firstInstallTime));
            LogUtils.d("wyltest", "最后更新时间: " + new Date(info.lastUpdateTime));
            LogUtils.d("wyltest", "签名信息: " + info.signatures[0].hashCode());

            // 检查是否是系统应用
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                LogUtils.d("wyltest", "这是一个系统应用");
            }
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                LogUtils.d("wyltest", "这是一个更新的系统应用");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Android 8+ 请求安装未知来源应用权限
     */
    public static void requestPackageInstallsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
