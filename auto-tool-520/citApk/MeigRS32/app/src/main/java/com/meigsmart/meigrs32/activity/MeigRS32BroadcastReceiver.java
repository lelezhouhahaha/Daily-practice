package com.meigsmart.meigrs32.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.PreferencesUtil;

public class MeigRS32BroadcastReceiver extends BroadcastReceiver{

    private static String VALIDATIONCODE = "83789";
    private static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
    private static final String WATER_RIPPLE_ACTION = "com.example.activity.waterripple";
    private static final String CUSTOMER_ACTIVITY_ACTION = "com.example.activity.customeractivityresult";
    private static final String START_UP_CONFIG = "common_MeigRS32BroadcastReceiver_start_up_config_bool";
    private static final String REBOOT_TEST = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.d("onRecive intent.getAction():" + intent.getAction());
        String startUpFlag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, START_UP_CONFIG);
        if (intent.getAction().equals(SECRET_CODE_ACTION)) {
            if(startUpFlag != null && !startUpFlag.isEmpty() && startUpFlag.equals("false")){
                LogUtil.d("MeigRS32BroadcastReceiver", "citapk MeigRS32BroadcastReceiver onRecive startUpFlag:" + startUpFlag);
                return;
            }
            Uri uri = intent.getData();
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (VALIDATIONCODE.equals(host)) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setClass(context, MainActivity.class);
                context.startActivity(i);
            }
        }else if(intent.getAction().equals(WATER_RIPPLE_ACTION)){
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setClass(context, LCDWaterRippleActivity.class);
            i.putExtra("fatherName","");
            i.putExtra("name", "waterripple");
            context.startActivity(i);
        }else if(intent.getAction().equals(CUSTOMER_ACTIVITY_ACTION)){

            if(MyApplication.CustomerFatherName.isEmpty() || MyApplication.CustomerFatherName.equals("")) {
                LogUtil.d(" CustomerActivity is not started never.");
                return;
            }
            String result = intent.getStringExtra("result");
            String reason = intent.getStringExtra("reason");
            String pkg = intent.getStringExtra("packageName");
            String cls = intent.getStringExtra("className");
            if (DataUtil.isForegroundPackage(context, pkg)) {
                LogUtil.d(" pkg:" + pkg + " is active.");
                DataUtil.stopActivity(context, pkg);
            }

            if(!DataUtil.checkActivityStatus(context, "com.meigsmart.meigrs32.activity.StartCustomerActivity")){
                LogUtil.d(" StartCustomerActivity is not active and start it.");
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setClass(context, StartCustomerActivity.class);
                i.putExtra("fatherName",MyApplication.CustomerFatherName);
                i.putExtra("name", "StartCustomerActivity");
                i.putExtra("packageName", pkg);
                i.putExtra("className", cls);
                i.putExtra("result", result);
                i.putExtra("reason", reason);
                context.startActivity(i);
            }else {
                LogUtil.d(" sendBroadcast com.example.activity.MY_BROADCAST_RESULT");
                Intent i = new Intent("com.example.activity.MY_BROADCAST_RESULT");
                i.putExtra("packageName", pkg);
                i.putExtra("className", cls);
                i.putExtra("result", result);
                i.putExtra("reason", reason);
                context.sendBroadcast(i);
            }
        }else if(intent.getAction().equals(REBOOT_TEST)){
            Intent intent_startDiagService = new Intent("com.intent.action.meig.autotest");
            intent_startDiagService.setPackage("com.meigsmart.meigrs32");
            context.startService(intent_startDiagService);
            if (PreferencesUtil.getFristLogin(context,"onClickStart")) {
                Intent main = new Intent(context, RunInActivity.class);
                main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(main);
            }
        }
    }
}
