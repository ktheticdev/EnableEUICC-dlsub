package com.github.sigr.EnableEUICC;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;

public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "xposedModule";
    private static Application appInstance;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (appInstance == null) {
                    appInstance = (Application) param.thisObject;
                    XposedBridge.log(TAG + ": Captured Application context from " + lpparam.packageName);
                }
            }
        });

        findAndHookMethod(EuiccManager.class, "isEnabled", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log(TAG + ": Called isEnabled()");
                return true;
            }
        });

        findAndHookMethod(
                DownloadableSubscription.class,
                "forActivationCode",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (appInstance == null) return;

                        Object result = param.getResult();
                        if (result instanceof DownloadableSubscription) {
                            DownloadableSubscription sub = (DownloadableSubscription) result;
                            String code = sub.getEncodedActivationCode();
                            if (code != null && !code.isEmpty()) {
                                ClipboardManager clipboard = (ClipboardManager)
                                        appInstance.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (clipboard != null) {
                                    ClipData clip = ClipData.newPlainText("Encoded eSIM activation code", code);
                                    clipboard.setPrimaryClip(clip);
                                    XposedBridge.log(TAG + ": Copied activation code to clipboard: " + code);
                                }
                            }
                        }
                    }
                }
        );

        XposedBridge.log(TAG + ": UICC privileges bypass + activation code copier installed on " + lpparam.packageName);
    }
}}
