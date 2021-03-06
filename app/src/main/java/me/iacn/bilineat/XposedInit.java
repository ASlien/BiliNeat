package me.iacn.bilineat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.iacn.bilineat.hook.HookHandler;
import me.iacn.bilineat.net.UpdateConfigTask;
import me.iacn.bilineat.util.HookBuilder;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by iAcn on 2016/10/5
 * Emali iAcn0301@foxmail.com
 */

public class XposedInit implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    public static XSharedPreferences xSharedPref;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        xSharedPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, "setting");
        xSharedPref.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam loadParam) throws Throwable {
        if (!Constant.biliPackageName.equals(loadParam.packageName)) return;

        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context context = (Context) callMethod(activityThread, "getSystemContext");

        PackageManager manager = context.getPackageManager();
        final String version = manager.getPackageInfo(Constant.biliPackageName, 0).versionName;
        final String configPath = isSupport(version, loadParam.appInfo.dataDir + "/files/bilineat");

        final boolean hasConfig = configPath != null;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            // Android 5.0 以上
            if (hasConfig) {
                HookHandler.methodHook(loadParam.classLoader, configPath);
            } else {
                downloadConfigFile(loadParam.classLoader, version);
            }
        } else {
            // Android 4.4 以下在 MultiDex 加载完成后再进行 Hook
            HookBuilder.create(loadParam.classLoader)
                    .setClass("tv.danmaku.bili.MainApplication")
                    .setMethod("onCreate")
                    .setHookCallBack(new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (hasConfig) {
                                HookHandler.methodHook(loadParam.classLoader, configPath);
                            } else {
                                downloadConfigFile(loadParam.classLoader, version);
                            }
                        }
                    }).hook();
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        if (!Constant.biliPackageName.equals(resParam.packageName)) return;

        xSharedPref.reload();
        HookHandler.layoutHook(resParam.res);
    }

    /**
     * 判断净化是否支持当前 B 站版本
     *
     * @return 配置文件路径
     */
    private String isSupport(String version, String filesDir) {
        File file = new File(filesDir);
        String[] listDir = file.list();

        if (listDir != null) {
            Set<String> set = new HashSet<>();
            Collections.addAll(set, listDir);

            if (set.contains(version)) {
                return filesDir + "/" + version;
            }
        }

        return null;
    }

    /**
     * 不支持当前哔哩哔哩版本,弹出 Toast 提示并更新配置文件
     */
    private void downloadConfigFile(ClassLoader classLoader, final String version) {
        HookBuilder.create(classLoader)
                .setClass("tv.danmaku.bili.ui.splash.SplashActivity")
                .setMethod("onCreate")
                .setParamTypes(Bundle.class)
                .setHookCallBack(new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        Context context = (Context) param.thisObject;
                        Toast.makeText(context, "哔哩净化暂不支持你的版本哦~", Toast.LENGTH_LONG).show();

                        boolean ignoreUpgradeHint = xSharedPref.getBoolean("ignore_upgrade_hint", false);
                        new UpdateConfigTask(context).execute(ignoreUpgradeHint, version);
                    }
                }).hook();
    }
}