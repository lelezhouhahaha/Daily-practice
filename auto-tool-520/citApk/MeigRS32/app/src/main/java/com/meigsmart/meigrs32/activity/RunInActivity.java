package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.android.internal.widget.LockPatternUtils;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.BatteryVolume;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.PreferencesUtil;
import com.meigsmart.meigrs32.util.TamperUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.sunmi.pay.hardware.aidl.AidlConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import butterknife.BindView;

import static com.meigsmart.meigrs32.activity.TrigerSunmiTestActivity.TRIGER_TEST_Fail;
import static com.meigsmart.meigrs32.util.TamperUtil.CONTACT_TRIGGER;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_DYNAMIC1;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_DYNAMIC2;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_STAIC1;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_STAIC2;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_STAIC3;
import static com.meigsmart.meigrs32.util.TamperUtil.CT_STAIC4;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_DYNAMIC1;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_DYNAMIC2;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_ROOTKEY_ERROR;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_ROOTKEY_LOST;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_STAIC1;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_STAIC2;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_STAIC3;
import static com.meigsmart.meigrs32.util.TamperUtil.MASK_STAIC4;

public class RunInActivity extends BaseActivity implements View.OnClickListener ,CheckListAdapter.OnCallBackCheckFunction {
    private final String TAG = "RunInActivity";
    private RunInActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private CheckListAdapter mAdapter;
    private TextView RunInTestStatus;
    private TextView RunInTestFailNameInfo;
    private TextView RunInTestFailReasonInfo;
    private TextView RunInTestFailName;
    private TextView RunInTestFailReason;
    private TextView RunInTestRemainTimeInfo;
    private TextView RunInTestRemainTime;
    private Button RunInTestBtnClose;
    private int currPosition = 0;

    private int DELAY_TIME = 2000;
    private int AllTestTime = 0;
    private String mDefaultPath;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mFileName ;
    private int mTotalRuninTime;
    private String mcharge_usb = "/sys/class/power_supply/usb/ovp_usb_en";
    private String mcharge_dc = "/sys/class/power_supply/usb/ovp_dc_en";
    private String mcharge_rk="/sys/class/power_supply/battery/set_chg_status";
    private String TAG_firstreboot = "common_keyboard_test_bool_config";
    private String FIRSTREBOOTCONFIGKEY = "common_runin_first_reboot_config";
    private boolean is_firstreboot = false;
    private boolean mFirstTime = false;
    SharedPreferences userSettings;
    SharedPreferences.Editor editor;

    private boolean stop_charge = false;
    private boolean reset_charge = false;
    private String projectName = "";
    private LockPatternUtils mLockPatternUtils;
    private selfDialogObj resultInfo;
    private final int HANDLER_UPDATE_RUNIN_RESULT = 1010;
    private final int HANDLER_CHECK_BATTERY_CAPACITY = 1011;
    private final int HANDLER_CHECK_TAMPER= 1012;
    private final int  HADLER_DELAY_TIME = 1000; //unit:ms
    private int gResultWindowsStartPointWidth = 0;
    private int gResultWindowsStartPointHeight= 0;
    private int gWindowsWidth = 0;
    private int gWindowsHeight = 0;
    private int gLooperNum = 0;
    private Boolean gLooperStopEnable = false;
    private final String ACTION_ENTER_MIDTEST = "com.sunmi.cit.action.ACTION_OPEN";// cit enter
    private final String ACTION_EXIT_MIDTEST = "com.sunmi.cit.action.ACTION_CLOSE"; // cit exit

    private String log = "";
    private List<String> tamper = new ArrayList<>();
    private String scanType = "";
    private int mBatteryLevelMin = 60;
    private int mBatteryLevelMax = 80;
    private int mCurrentCapacityValue = 0;
    private static final String PLUGGED = "plugged";
    private Boolean mUsbPluggedChangedStatus = false;
    private int mUsbPluggedChangedSave = 0;
    private boolean isStabilityTest = false;
    private String[] items = {"4h", "8h", "12h", "24h", "240h"};
    private String[] items_value = {"4", "8", "12", "24", "240"};
    private boolean[] checkedItems = new boolean[] { false, false, false, false, false };

	private class selfDialogObj extends Object{
        private String mTestResult;
        private String mFailTestName;
        private String mFailTestReason;
        private String mTestTotalTime;
        private String mRemainTime;

        public void setTestResult(String result){
            mTestResult = result;
        }
        public String getTestResult(){
            return mTestResult;
        }

        public void setFailTestName(String name){
            mFailTestName = name;
        }
        public String getFailTestName(){
            return mFailTestName;
        }

        public void setFailTestNameReason(String reason){
            mFailTestReason = reason;
        }
        public String getFailTestNameReason(){
            return mFailTestReason;
        }

        public void setTestTotalTime(String totaltime){
            mTestTotalTime = totaltime;
        }
        public String getTestTotalTime(){
            return mTestTotalTime;
        }

        public void setRemainTime(String time){
            mRemainTime = time;
        }
        public String getRemainTime(){
            return mRemainTime;
        }
    }

    private void activeBT(){
	    new EnableBT().execute();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_run_in;
    }
    private void showListDialog(){
        if(isFirst()) {
            //final String[] items = {"4h", "8h", "12h", "24h", "240h"};
            //final String[] items_value = {"4", "8", "12", "24", "240"};
            //boolean[] checkedItems = new boolean[] { false, false, false, false, false };
            AlertDialog.Builder listDialog = new AlertDialog.Builder(mContext);
            listDialog.setIcon(R.drawable.ic_dialog);
            listDialog.setTitle(R.string.total_time_setting);
            listDialog.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (int i = 0; i < items.length; i++) {
                        checkedItems[i] = false;
                    }
                    checkedItems[which] = true;
                }
            });

            listDialog.setNegativeButton(getApplicationContext().getText(R.string.cancle), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    /*mFirstTime = false;
                    PreferencesUtil.isFristLogin(mContext,"onClickStart", false);
                    PreferencesUtil.setStringData(mContext,"first","true");*/
                    stopRuninTest();
                    mContext.finish();
                }
            });
            listDialog.setPositiveButton(getApplicationContext().getText(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    int time = 0;
                    int i = 0;
                    for (i = 0; i < items.length; i++) {
                         if (checkedItems[i]) {
                             time = Integer.parseInt(items_value[i]);
                             break;
                         }
                     }
                    if(i == items.length ) {
                        Log.d(TAG,"not select time!");
                        /*mFirstTime = false;
                        PreferencesUtil.isFristLogin(mContext,"onClickStart", false);
                        PreferencesUtil.setStringData(mContext,"first","true");*/
                        stopRuninTest();
                        mContext.finish();
                        return;
                    }
                    if (time != 0)
                        RuninConfig.putRunTime("total", time * 3600);
                    Log.d(TAG,"time:" + time + "totaltime: " + RuninConfig.getRunTime(mContext, "total"));
                    PreferencesUtil.setStringData(mContext, "SetTime", String.valueOf(RuninConfig.getRunTime(mContext, "total")));
                    PreferencesUtil.setStringData(mContext, "TotalTime", String.valueOf(items[i])); //provides information to dialog display
                    // add by maohaojie on 2019.10.30 for bug P_RK95_E-676
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG,"start_test_time:" + df.format(new Date()));
                    PreferencesUtil.setStringData(mContext, "first", "false");
                    PreferencesUtil.isFristLogin(mContext, "onClickStart", true);
                    checkSetStabilityTestProp(true);
                    if("MT537".equals(projectName)){
                        StartRuninDependOnBatteryCapacity();
                    }else {
                        mHandler.sendEmptyMessageDelayed(1001, DELAY_TIME);
                    }
                }
            });
            listDialog.setCancelable(false);
            listDialog.show();
        }else {
            mFirstTime = true;
            mHandler.sendEmptyMessageDelayed(1001, DELAY_TIME);
        }
    }

    private void StartRuninDependOnBatteryCapacity(){
        int battery = mCurrentCapacityValue;
        if(battery == 0) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        Log.d(TAG, "battery:" + battery);
        if(battery >= 30){
            mHandler.sendEmptyMessage(1001);
        }else{
            ToastUtil.showCenterLong(getString(R.string.runin_wait_test_due_to_low_power));
            mHandler.sendEmptyMessageDelayed(HANDLER_CHECK_BATTERY_CAPACITY, DELAY_TIME*5);
        }
    }

    @Override
    protected void initData() {
	    try {
            SystemProperties.set("persist.vendor.cit.flag", "true");
        }catch (Exception e){
            Log.e(TAG,"initData persist.vendor.cit.flag:" + Log.getStackTraceString(e));
        }

        try {
            SystemProperties.set("persist.vendor.cit.runin.flag", "true");
        }catch (Exception e){
            Log.e(TAG,"initData persist.vendor.cit.runin.flag:" + Log.getStackTraceString(e));
        }
        RuninConfig.initdata(mContext);
        Intent enterCit = new Intent(ACTION_ENTER_MIDTEST);
        enterCit.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(enterCit);
        LogUtil.initialize(this,true);
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.function_run_in);
        resultInfo = null;
        gWindowsWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        gWindowsHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        setLockNone();
        activeBT();
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        Log.d(TAG,"cit PreferencesUtil.getFristLogin(this,\"onClickStart\"):" +PreferencesUtil.getFristLogin(this,"onClickStart"));
        Log.d(TAG,"cit isFirst:" +isFirst());
        Log.d(TAG,"cit " +OdmCustomedProp.getCit1ResultProp() + ":" +SystemProperties.get(OdmCustomedProp.getCit1ResultProp(), "unknown"));
        is_firstreboot = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_firstreboot).equals("true") || DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, FIRSTREBOOTCONFIGKEY).equals("true");
        //if(isFirst() && !SystemProperties.get("persist.custmized.cit1_result", "unknown").equals("true")) {
		if(isFirst() && !SystemProperties.get(OdmCustomedProp.getCit1ResultProp(), "unknown").equals("true")) {
            if(!"MT537".equals(projectName)){
                super.showDialog(MyApplication.PreNAME, MyApplication.RuninTestNAME);
            }else{
                super.showDialog(MyApplication.MMI1_PreName, MyApplication.RuninTestNAME);
            }
        }else {
        userSettings = getSharedPreferences("citsetting", 0);
        editor = userSettings.edit();
        int totalTime = userSettings.getInt("TotalRunniTime",0);
        if(totalTime == 0) {
            editor.putInt("TotalRunniTime", RuninConfig.getRunTime(mContext, "total"));
            editor.commit();
        }else RuninConfig.putRunTime("total", totalTime);

        mTotalRuninTime = RuninConfig.getRunTime(mContext, "total");
        mDefaultPath = getResources().getString(R.string.run_in_save_log_default_path);
        mFileName = getResources().getString(R.string.run_in_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.run_in_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.run_in_save_log_custom_path);
            Log.d(TAG,"mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " mTotalRuninTime:" + mTotalRuninTime+ "s" +
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getResources().getString(R.string.function_run_in);
        super.mFatherName = getIntent().getStringExtra("fatherName");

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
        }

        List<String> config = Const.getXmlConfig(this,Const.CONFIG_RUNIN);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
            if("MT537".equals(projectName)) {
                if ((face_select != null) && (!face_select.equals("")) && (face_select.length()==8)) {

                    Iterator<TypeModel> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        TypeModel item = iterator.next();
                        if (face_select.substring(2, 3).equals("1")) {
                            if (item.getName().equals(getResources().getString(R.string.FrontCameraAutoActivity))) {
                                iterator.remove();
                                continue;
                            }
                        } else if (face_select.substring(2, 3).equals("0")) {
                            if (item.getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))) {
                                iterator.remove();
                                continue;
                            }
                        }

                        if (!face_select.substring(6, 7).equals("1")) {
                            if (item.getName().equals(getResources().getString(R.string.AutoFingerTestActivity))) {
                                iterator.remove();
                                continue;
                            }
                        }

                        if (face_select.substring(3, 4).equals("1")) {
                            if (item.getName().equals(getResources().getString(R.string.AutoScanActivity))
                                    || item.getName().equals(getResources().getString(R.string.SunMi_FlashLightActivity))) {
                                iterator.remove();
                            }
                        }

                    }

                    /**
                    if (face_select.substring(2, 3).equals("1")) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getName().equals(getResources().getString(R.string.FrontCameraAutoActivity))) {
                                list.remove(i);
                            }
                        }
                    }
                    if (face_select.substring(2, 3).equals("0")) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))) {
                                list.remove(i);
                            }
                        }
                    }
                    if(!face_select.substring(6, 7).equals("1")){
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getName().equals(getResources().getString(R.string.AutoFingerTestActivity))) {
                                list.remove(i);
                            }
                        }
                    }
                    if(face_select.substring(3, 4).equals("1")){
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getName().equals(getResources().getString(R.string.AutoScanActivity))||
                                    list.get(i).getName().equals(getResources().getString(R.string.SunMi_FlashLightActivity)) ) {
                                list.remove(i);
                            }
                        }
                    }
                     */
                }else{
                    Iterator<TypeModel> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        TypeModel item = iterator.next();
                        if (item.getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))
                                || item.getName().equals(getResources().getString(R.string.AutoFingerTestActivity))) {
                            iterator.remove();
                        }
                    }

                    /**
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getName().equals(getResources().getString(R.string.ThreeM_CameraAutoActivity))||
                                list.get(i).getName().equals(getResources().getString(R.string.AutoFingerTestActivity))){
                            list.remove(i);
                        }
                    }
                     */
                }
            }
        mAdapter.setData(list);
            if(isFirst()) {
                if("MT537".equals(projectName)) {
                    mHandler.sendEmptyMessage(HANDLER_CHECK_TAMPER);
                    //new readLog().execute();
                }else{
                    checkIsStabilityTest();
                    showListDialog();
                }
            }else {
                String TimeStr = PreferencesUtil.getStringData(this, "SetTime", "3600");
                String TotalTime = PreferencesUtil.getStringData(this, "TestedTime", "0");
                Log.d(TAG,"TimeStr:" + TimeStr + " TotalTime" + TotalTime);
                int time = 0;
                if (TimeStr.isEmpty() || (TimeStr == null)) {
                    Log.d(TAG,"time = 3600");
                    time = 3600;
                } else {
                    int timestr = Integer.parseInt(TimeStr);
                    int totaltime = Integer.parseInt(TotalTime);
                    Log.d(TAG,"timestr:" + timestr + " totaltime" + totaltime);
                   /* if (PreferencesUtil.getFristLogin(this, "RebootTime")) {
                        Log.d(TAG,"PreferencesUtil.getStringData(this, \"RestTime\"):" + PreferencesUtil.getStringData(this, "RestTime"));
                        time = Integer.parseInt(PreferencesUtil.getStringData(this, "RestTime")) - totaltime;
                    } else {
                        time = timestr - totaltime;
                    }*/
                    time = timestr - totaltime;
                    Log.d(TAG,"time:" + time);
                    AllTestTime = timestr - time;
                    Log.d(TAG,"AllTestTime:" + AllTestTime);
                }
                RuninConfig.putRunTime("total", time);
                //Total time remaining before restart
                //PreferencesUtil.setStringData(mContext, "RestTime", String.valueOf(time));
                PreferencesUtil.setStringData(mContext, "SetTime", String.valueOf(time));
                mFirstTime = true;
                mHandler.sendEmptyMessage(1001);
            }
            RuninConfig.setRuninStartTime();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        activeWifi();
    }

    private void activeWifi(){
        WifiManager mWifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(true);
        }
    }

    private Boolean isFirst(){
		String loginStatusStr = PreferencesUtil.getStringData(mContext, "first", "true");
		Log.d(TAG,"loginStatusStr:" +loginStatusStr);
		return new Boolean(loginStatusStr);
    }

    private void stopRuninTest(){
		Log.d(TAG, "stopRuninTest");
        mFirstTime = false;
        PreferencesUtil.isFristLogin(mContext,"onClickStart", false);
        PreferencesUtil.setStringData(mContext,"first","true");
        //mContext.finish();
    }

    private selfDialogObj createSelfDialogObj(Boolean testResult){
        selfDialogObj mRuninTestResult = new selfDialogObj();
        if(testResult) {
            mRuninTestResult.setTestResult(getResources().getString(R.string.success));
            mRuninTestResult.setFailTestName("");
            mRuninTestResult.setFailTestNameReason("");
        }else {
            mRuninTestResult.setTestResult(getResources().getString(R.string.fail));
            mRuninTestResult.setFailTestName(mAdapter.getData().get(currPosition).getName());
            FunctionBean mResult= getSubData(mName, mAdapter.getData().get(currPosition).getName());
            Log.d("RunInActivity", "mName:" + mName);
            Log.d("RunInActivity", "mResult.getReason():" + mResult.getReason());
            mRuninTestResult.setFailTestNameReason(mResult.getReason());
        }
        mRuninTestResult.setTestTotalTime(PreferencesUtil.getStringData(mContext, "TotalTime"));
        if(!testResult) {
            String TotalTime = PreferencesUtil.getStringData(this, "SetTime", "3600");
            String TestedTime = PreferencesUtil.getStringData(this, "TestedTime", "0");
            LogUtil.d("TotalTime:" + TotalTime);
            LogUtil.d("TestedTime:" + TestedTime);
            if (!TotalTime.equals("") && !TestedTime.equals("")) {
                int ITotalTime = Integer.parseInt(TotalTime);
                int ITestedTime = Integer.parseInt(TestedTime);
                int IRemainTime = 0;
                IRemainTime = (ITotalTime <= ITestedTime) ? 0 : (ITotalTime - ITestedTime);
                LogUtil.d("IRemainTime:" + IRemainTime);
                mRuninTestResult.setRemainTime(DataUtil.formatTime(mContext, IRemainTime));
            } else mRuninTestResult.setRemainTime("");
        }else mRuninTestResult.setRemainTime("00:00:00");

        return mRuninTestResult;
    }

    protected void initRunInResultFloatView() {
        wmParams = new WindowManager.LayoutParams();
        mWindowManager = this.getWindowManager();
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = this.getLayoutInflater();
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.runintestfinishview, null);
        mWindowManager.addView(mFloatLayout, wmParams);
        RunInTestStatus = (TextView)mFloatLayout.findViewById(R.id.test_status);
        RunInTestFailNameInfo = (TextView)mFloatLayout.findViewById(R.id.test_fail_item_info);
        RunInTestFailReasonInfo = (TextView)mFloatLayout.findViewById(R.id.test_fail_reason_info);
        RunInTestFailName = (TextView)mFloatLayout.findViewById(R.id.test_fail_name);
        RunInTestFailReason = (TextView)mFloatLayout.findViewById(R.id.test_fail_reason);
        RunInTestRemainTimeInfo = (TextView)mFloatLayout.findViewById(R.id.remain_time_info);
        RunInTestRemainTime = (TextView)mFloatLayout.findViewById(R.id.remain_time);
        RunInTestBtnClose = (Button)mFloatLayout.findViewById(R.id.btn_close);
        RunInTestBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gLooperStopEnable = true;
                mFloatLayout.setVisibility(View.GONE);
            }
        });
    }
    protected void updateRunInResultFloatView(int width, int height, selfDialogObj result, boolean testResult) {
        wmParams.x = width;
        wmParams.y = height;
        Log.d(TAG, "wmParams.x:" + wmParams.x);
        Log.d(TAG, "wmParams.y:" + wmParams.y);
        if(testResult) {
            mFloatLayout.setBackgroundResource(R.color.green);
            RunInTestBtnClose.setBackgroundResource(R.color.green);
        }else{
            mFloatLayout.setBackgroundResource(R.color.red);
            RunInTestBtnClose.setBackgroundResource(R.color.red);
        }
        mWindowManager.updateViewLayout(mFloatLayout, wmParams);
        RunInTestStatus.setText(result.getTestResult());
        if(testResult) {
            RunInTestFailName.setVisibility(View.GONE);
            RunInTestFailReason.setVisibility(View.GONE);
            RunInTestFailNameInfo.setVisibility(View.GONE);
            RunInTestFailReasonInfo.setVisibility(View.GONE);
        }else {
            RunInTestFailName.setText(result.getFailTestName());
            RunInTestFailReason.setText(result.getFailTestNameReason());
        }
        RunInTestRemainTimeInfo.setText(String.format(getResources().getString(R.string.runing_dialog_remain_info), result.getTestTotalTime()));
        RunInTestRemainTime.setText(result.getRemainTime());
    }
    /*private void showRunInFinishAlertDialog( Boolean testResult){
        selfDialogObj result = createSelfDialogObj(testResult);
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        final View runInFinishDialogView = layoutInflater.inflate(R.layout.runintestfinishview, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AlertDialog);
        TextView testStatus = (TextView)runInFinishDialogView.findViewById(R.id.test_status);
        TextView testFailNameInfo = (TextView)runInFinishDialogView.findViewById(R.id.test_fail_item_info);
        TextView testFailReasonInfo = (TextView)runInFinishDialogView.findViewById(R.id.test_fail_reason_info);
        TextView testFailName = (TextView)runInFinishDialogView.findViewById(R.id.test_fail_name);
        TextView testFailReason = (TextView)runInFinishDialogView.findViewById(R.id.test_fail_reason);
        TextView testRemainTimeInfo = (TextView)runInFinishDialogView.findViewById(R.id.remain_time_info);
        TextView testRemainTime = (TextView)runInFinishDialogView.findViewById(R.id.remain_time);

        testStatus.setText(result.getTestResult());
        if(testResult) {
            testFailName.setVisibility(View.GONE);
            testFailReason.setVisibility(View.GONE);
            testFailNameInfo.setVisibility(View.GONE);
            testFailReasonInfo.setVisibility(View.GONE);
        }else {
            testFailName.setText(result.getFailTestName());
            testFailReason.setText(result.getFailTestNameReason());
        }
        testRemainTimeInfo.setText(String.format(getResources().getString(R.string.runing_dialog_remain_info), result.getTestTotalTime()));
        testRemainTime.setText(result.getRemainTime());
        builder.setView(runInFinishDialogView);
        builder.setPositiveButton(getResources().getText(R.string.close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(false);
        dialog.show();

        Button mPositiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);;
        LinearLayout.LayoutParams positiveButtonLL =(LinearLayout.LayoutParams)mPositiveButton.getLayoutParams();
        positiveButtonLL.gravity= Gravity.CENTER;
        positiveButtonLL.width= ViewGroup.LayoutParams.MATCH_PARENT;
        mPositiveButton.setLayoutParams(positiveButtonLL);
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = mContext.getResources().getDisplayMetrics().widthPixels*7/8;
        params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
        if(testResult) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.green)));
        }else {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.red)));
        }
    }*/
	
	private int getNum(int startNum,int endNum){
        if(endNum > startNum){
            Random random = new Random();
            return random.nextInt(endNum - startNum) + startNum;
        }
        return 0;
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    Log.d(TAG,"  mAdapter.getData().get(currPosition).getName():"+mAdapter.getData().get(currPosition).getName());
                    if(is_firstreboot && mFirstTime ){
                        if(mAdapter.getData().get(currPosition).getName().equals(getString(R.string.RebootTestActivity))){
                            currPosition++;
                            if ( currPosition == mAdapter.getItemCount() ) {
                                currPosition = 0;
//                                mHandler.sendEmptyMessageDelayed(1003,10);
                                return;
                            }
                            mFirstTime = false;
                        }
                    }
                    //Saves the time that items have been tested
                    PreferencesUtil.setStringData(mContext, "TestedTime", String.valueOf(RuninConfig.getTotalRuninTime(mContext)/1000));
                    startActivity(mAdapter.getData().get(currPosition));
                    break;
                case 1002://test finish
                    ToastUtil.showBottomShort(getResources().getString(R.string.run_in_test_finish));
                    //initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    saveLog(true);
                    checkSetStabilityTestProp(false);
                    // add by maohaojie on 2019.10.30 for bug P_RK95_E-676
                    SimpleDateFormat df= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG,"end_test_time1111:"+df.format(new Date()));
                    /*mContext.finish();
                    mFirstTime = false;
                    PreferencesUtil.isFristLogin(mContext,"onClickStart", false);
                    PreferencesUtil.setStringData(mContext,"first","true");*/
					//showRunInFinishAlertDialog(true);
                    initRunInResultFloatView();
                    if(resultInfo == null) {
                        resultInfo = createSelfDialogObj(true);
                    }else{
                       Log.d(TAG, "resultInfo != null");
                    }
                    updateRunInResultFloatView(0, 0, resultInfo, true);
	                {
	                    Message msg1 = mHandler.obtainMessage();
	                    msg1.what = HANDLER_UPDATE_RUNIN_RESULT;
	                    msg1.obj = true;
	                    mHandler.sendMessageDelayed(msg1, HADLER_DELAY_TIME);
	                }
                    //stopRuninTest();
                    break;
                /*case 1003://test finish once
                    //initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    saveLog(true);
                    mFirstTime = false;
                    PreferencesUtil.isFristLogin(mContext,"onClickStart", false);
                    PreferencesUtil.setStringData(mContext,"first","true");
                    break;*/
                case HANDLER_UPDATE_RUNIN_RESULT:
                    Log.d(TAG, "HANDLER_UPDATE_RUNIN_RESULT gLooperStopEnable:" + gLooperStopEnable);
                    if(!gLooperStopEnable) {
                        boolean result = (boolean) msg.obj;
                        gResultWindowsStartPointWidth = getNum(0, (gWindowsWidth-mFloatLayout.getWidth()));
                        gResultWindowsStartPointHeight = getNum(0, (gWindowsHeight-mFloatLayout.getHeight()));
                        updateRunInResultFloatView(gResultWindowsStartPointWidth, gResultWindowsStartPointHeight, resultInfo, result);
                        {
                            Message msg1 = mHandler.obtainMessage();
                            msg1.what = HANDLER_UPDATE_RUNIN_RESULT;
                            msg1.obj = result;
                            if(!gLooperStopEnable) {
                                mHandler.sendMessageDelayed(msg1, HADLER_DELAY_TIME);
                            }
                        }
                    }else {
                        stopRuninTest();
                        Log.d(TAG, "stop update RunIn Result FlocatView!");
                    }
                    break;
                case HANDLER_CHECK_BATTERY_CAPACITY:
                    StartRuninDependOnBatteryCapacity();
                    break;
                case HANDLER_CHECK_TAMPER:
                    showRuninDialog();
                    break;
            }
        }
    };

    /*private String initPath(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)){
            File f = FileUtil.createRootDirectory(path);
            File file = FileUtil.mkDir(f);
            return FileUtil.writeFile(file,fileName,result);
        }
        return "";
    }*/

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if(persistPath.exists() && persistPath.isDirectory()){
            File dir = FileUtil.mkDir(new File(path));
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            return !"".equals(FileUtil.writeFile(dir, fileName,result));
        }
        return false;
    }

    private void saveLog(boolean savePersist) {
        List<FunctionBean> list = getFatherData(RunInActivity.super.mName);

        List<ResultModel> resultList = new ArrayList<>();
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()){
                case 0:
                    model.setResult(Const.RESULT_NOTEST);
                    persistModel.setResult(Const.RESULT_NOTEST);
                    isAllSuccess = false;
                    break;
                case 1:
                    model.setResult(Const.RESULT_FAILURE);
                    persistModel.setResult(Const.RESULT_FAILURE);
                    isAllSuccess = false;
                    break;
                case 2:
                    model.setResult(Const.RESULT_SUCCESS);
                    persistModel.setResult(Const.RESULT_SUCCESS);
                    break;
            }
            if(isAllSuccess){
                Log.d(TAG,"MeigTest agingtest_result_flags set success 1:"+isAllSuccess);
                Settings.System.putInt(mContext.getContentResolver(),"agingtest_result_flags",1);
            }else{
                Log.d(TAG,"MeigTest agingtest_result_flags set success 0:"+isAllSuccess);
                Settings.System.putInt(mContext.getContentResolver(),"agingtest_result_flags",0);
            }


            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());

            persistModel.setName(bean.getSubclassName());

            resultList.add(model);
            persistResultList.add(persistModel);
        }

        PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setTime(PreferencesUtil.getStringData(mContext, "TotalTime", "unknown")/*DataUtil.formatTime(AllTestTime)*/);
        allResultModel.setName("RUN_IN_TEST_RESULT");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);

        //save RunIn single start time
		SimpleDateFormat df= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PersistResultModel mSingleStartTimeModel = new PersistResultModel();
        mSingleStartTimeModel.setName("START_TIME:");
        long mSingleStartTime = RuninConfig.getRuninStartTime();
        Log.d(TAG, "mSingleStartTime:" + mSingleStartTime);
        String mRunInSingleStartTimeStr = df.format(new Date(mSingleStartTime));
        Log.d(TAG, "saveLog mRunInSingleStartTimeStr:<" + mRunInSingleStartTimeStr + ">.");
        mSingleStartTimeModel.setTime(mRunInSingleStartTimeStr);
        persistResultList.add(mSingleStartTimeModel);

        //save RunIn finished time
        PersistResultModel mFinishTimeModel = new PersistResultModel();
        mFinishTimeModel.setName("FINISH_TIME:");
        String mRunInFinishTimeStr = df.format(new Date());
        mFinishTimeModel.setTime(mRunInFinishTimeStr);
        Log.d(TAG, "saveLog FINISH_TIME:<" + mRunInFinishTimeStr + ">.");
        persistResultList.add(mFinishTimeModel);

        writeSDCardResult(isCustomPath ? mCustomPath : mDefaultPath, mFileName,  JSON.toJSONString(resultList));
        if(savePersist) {
            boolean result = writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE), Const.RUN_IN_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
            Log.d(TAG,"save persist result:" + result);
        }
        String runin_result = isAllSuccess?"true":"false";
		SystemProperties.set(OdmCustomedProp.getRuninResultProp(), runin_result);
    }

    private String createJsonResult(){
        List<FunctionBean> list = getFatherData(super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            if (bean.getResults() == 0){
                model.setResult(Const.RESULT_NOTEST);
            } else if (bean.getResults() == 1){
                model.setResult(Const.RESULT_FAILURE);
            } else if (bean.getResults() == 2){
                model.setResult(Const.RESULT_SUCCESS);
            }
            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());
            resultList.add(model);
        }
        return JSON.toJSONString(resultList);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            stopRuninTest();
            mContext.finish();
        }
    }

    @Override
    public void onItemClick(int position) {
//        currPosition = position;
//        startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111 || resultCode == 1000){
            if (data!=null){
                PreferencesUtil.setStringData(mContext, "TestedTime", String.valueOf(RuninConfig.getTotalRuninTime(mContext)/1000));
                int results = data.getIntExtra("results",0);
                Log.d(TAG,"onActivityResult test results:" + results);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();
                String mClassName = mAdapter.getData().get(currPosition).getCls().getSimpleName();
                Log.d(TAG, "onActivityResult mClassName:" + mClassName);
                if((!mClassName.contains("ScanActivitySS1100")
                        && !mClassName.contains("ScanActivityFarFocus")
                        && !mClassName.contains("ScanActivityNearFocus")) && (results != SUCCESS) && !"true".equals(SystemProperties.get("persist.vendor.cit.stabilitytest"))){
                    if(mAdapter.getData().get(currPosition).getName().equals(getString(R.string.HeatingTestActivity))){
                        mHandler.sendEmptyMessageDelayed(1002,10);
                        return;
                    }
					//showRunInFinishAlertDialog(false);
                    initRunInResultFloatView();
                    if(resultInfo == null) {
                        resultInfo = createSelfDialogObj(false);
                    }else{
                        Log.d(TAG, "resultInfo != null");
                    }
                    updateRunInResultFloatView(20, 20,  resultInfo, false);
                    Message msg = mHandler.obtainMessage();
                    msg.what = HANDLER_UPDATE_RUNIN_RESULT;
                    msg.obj = false;
                    mHandler.sendMessageDelayed(msg, HADLER_DELAY_TIME);
                    //stopRuninTest();
					return;
                }

                currPosition++;
                if ( currPosition == mAdapter.getItemCount() ){
                    currPosition = 0;
                    //mFirstTime = false;
//                    mHandler.sendEmptyMessageDelayed(1003,DELAY_TIME);
                }
                if (RuninConfig.isOverTotalRuninTime(mContext)){
                    //PreferencesUtil.isFristLogin(this,"first",true);
                    mHandler.sendEmptyMessageDelayed(1002,DELAY_TIME);
                } else {
                    mHandler.sendEmptyMessageDelayed(1001, DELAY_TIME);
                }
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra("level", -1);
                int plugged = intent.getIntExtra(PLUGGED, 0);
                mCurrentCapacityValue = level;
                Log.d(TAG, " plugged:" + plugged + " mUsbPluggedChangedSave:" + mUsbPluggedChangedSave);
                if(mUsbPluggedChangedSave != plugged){
                    mUsbPluggedChangedSave = plugged;
                    mUsbPluggedChangedStatus = true;
                }else mUsbPluggedChangedStatus = false;

                Log.d(TAG, "level:" + level + " mUsbPluggedChangedStatus:" + mUsbPluggedChangedStatus + " mBatteryLevelMin:" + mBatteryLevelMin + " mBatteryLevelMax:" + mBatteryLevelMax + " reset_charge:" + reset_charge + " stop_charge:" + stop_charge);
                if(mUsbPluggedChangedStatus){
                    if (level <= mBatteryLevelMin) {
                        startCharge();
                    } else if (level >= mBatteryLevelMax) {
                        stopCharge();
                    }
                    mUsbPluggedChangedStatus = false;
                }else {
                    if (!reset_charge && level <= mBatteryLevelMin) {
                        startCharge();
                    } else if (!stop_charge && level >= mBatteryLevelMax) {
                        stopCharge();
                    }
                }
            }
        }
    };

    private void startCharge(){
        FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend","0");
        FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled","1");
        reset_charge = true;
        stop_charge = false;
    }
    private void stopCharge(){
        FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend","1");
        FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled","0");
        stop_charge = true;
        reset_charge = false;
    }

    private boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            Log.d(TAG,"write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        writeToFile(mcharge_usb,"0");
        writeToFile(mcharge_dc,"0");
        FileUtil.writeToFile("sys/class/power_supply/battery/input_suspend", "0");
        FileUtil.writeToFile("sys/class/power_supply/battery/battery_charging_enabled", "1");
        // add by maohaojie on 2019.10.30 for bug P_RK95_E-676
        SimpleDateFormat df= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(TAG,"end_test_time2222:"+df.format(new Date()));
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeMessages(HANDLER_UPDATE_RUNIN_RESULT);
        mHandler.removeMessages(HANDLER_CHECK_BATTERY_CAPACITY);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        //mHandler.removeMessages(1003);
        try {
            SystemProperties.set("persist.vendor.cit.flag", "false");
        }catch (Exception e){
            Log.e(TAG,"onDestroy persist.vendor.cit.flag:" + Log.getStackTraceString(e));
        }

        try {
            SystemProperties.set("persist.vendor.cit.runin.flag", "false");
        }catch (Exception e){
            Log.e(TAG,"onDestroy persist.vendor.cit.runin.flag:" + Log.getStackTraceString(e));
        }
      
        WifiManager mWifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(false);
        }

        Intent exitRunin = new Intent(ACTION_EXIT_MIDTEST);
        exitRunin.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(exitRunin);

        super.onDestroy();
    }

    public void setLockNone(){
        try {
            mLockPatternUtils = new LockPatternUtils(mContext);
            mLockPatternUtils.clearEncryptionPassword();
            mLockPatternUtils.setLockScreenDisabled(true, 0);
        }catch (Exception e){
            Log.d(TAG,"setLockNone fail: "+e.toString());
        }
     }

    void showRuninDialog(){
        Log.d(TAG,"showRuninDialog start");
        log = TamperUtil.getTamperlog();
        if(log==null || log.isEmpty()){
            showListDialog();
        }else{
            Log.d(TAG,"SplitString");
            tamper = TamperUtil.SplitString(TamperUtil.convert(log));
            showTamperDialog();
        }
        Log.d(TAG,"showRuninDialog end");
    }
    private class readLog extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            log = TamperUtil.getTamperlog();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(log==null || log.isEmpty()){
                showListDialog();
            }else{
                tamper = TamperUtil.SplitString(TamperUtil.convert(log));
                showTamperDialog();
            }
            super.onPostExecute(unused);
        }
    }

    private TextView mTestColor1,mTestColor2,mTestColor3,mTestColor4,mTestColor5,trigger_type,mTestColor6,mTestMask1,mTestMask2,mTestMask3,mTestMask4,mTestMask5,mTestMask6,mask_type;
    private List<String> tamper_new = new ArrayList<String>();
    private List<String> tamper_type = new ArrayList<String>();

    private void showTamperDialog(){
        AlertDialog.Builder TamperDialog = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.dialog_tamper_log,null);
        mTestColor1 =(TextView) v.findViewById(R.id.dialog_test_color1);
        mTestColor2 =(TextView)v.findViewById(R.id.dialog_test_color2);
        mTestColor3 =(TextView)v.findViewById(R.id.dialog_test_color3);
        mTestColor4 =(TextView)v.findViewById(R.id.dialog_test_color4);
        mTestColor5 =(TextView)v.findViewById(R.id.dialog_test_color5);
        trigger_type =(TextView)v.findViewById(R.id.dialog_trigger_type);
        mTestColor6 =(TextView)v.findViewById(R.id.dialog_test_color6);
        mTestMask1 =(TextView)v.findViewById(R.id.dialog_test_mask1);
        mTestMask2 =(TextView)v.findViewById(R.id.dialog_test_mask2);
        mTestMask3 =(TextView)v.findViewById(R.id.dialog_test_mask3);
        mTestMask4 =(TextView)v.findViewById(R.id.dialog_test_mask4);
        mTestMask5 =(TextView)v.findViewById(R.id.dialog_test_mask5);
        mTestMask6 =(TextView)v.findViewById(R.id.dialog_test_mask6);
        mask_type =(TextView)v.findViewById(R.id.dialog_mask_type);
        analysisLog(tamper);
        Dialog dialog = TamperDialog.create();
        dialog.show();
        dialog.getWindow().setContentView(v);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }

    public void analysisLog(List<String> ppp) {
        for (int j=0;j<ppp.size();j++) {
            tamper_new.add(ppp.get(j).substring(22,30));
            tamper_type.add(ppp.get(j).substring(14,22));

            if(CONTACT_TRIGGER == Long.parseLong(tamper_type.get(j),16)){
                if((CT_DYNAMIC1 == Long.parseLong(tamper_new.get(j),16)) || (CT_DYNAMIC1 == (Long.parseLong(tamper_new.get(j),16) & CT_DYNAMIC1))){
                    mTestColor1.setVisibility(View.VISIBLE);
                    mTestColor1.setText(getString(R.string.status_dyanmic1));
                }
                if((CT_DYNAMIC2 == Long.parseLong(tamper_new.get(j),16)) || (CT_DYNAMIC2 == (Long.parseLong(tamper_new.get(j),16) & CT_DYNAMIC2))){
                    mTestColor2.setVisibility(View.VISIBLE);
                    mTestColor2.setText(getString(R.string.status_dyanmic2));
                }
                if((CT_STAIC1 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC1 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC1))){
                    mTestColor3.setVisibility(View.VISIBLE);
                    mTestColor3.setText(getString(R.string.status_static1));
                }
                if((CT_STAIC2 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC2 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC2))){
                    mTestColor4.setVisibility(View.VISIBLE);
                    mTestColor4.setText(getString(R.string.status_static2));
                }
                if((CT_STAIC3 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC3 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC3))){
                    mTestColor5.setVisibility(View.VISIBLE);
                    mTestColor5.setText(getString(R.string.status_static3));
                }
                if((CT_STAIC4 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC4 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC4))){
                    mTestColor6.setVisibility(View.VISIBLE);
                    mTestColor6.setText(getString(R.string.status_static4));
                }
                trigger_type.setVisibility(View.VISIBLE);
                trigger_type.setText(getString(R.string.status_trigger));
            }else{
                if((MASK_DYNAMIC1 == Long.parseLong(tamper_new.get(j),16)) || (MASK_DYNAMIC1 == (Long.parseLong(tamper_new.get(j),16) & MASK_DYNAMIC1))){
                    mTestMask1.setVisibility(View.VISIBLE);
                    mTestMask1.setText(getString(R.string.status_dyanmic1));
                }
                if((MASK_DYNAMIC2 == Long.parseLong(tamper_new.get(j),16)) || (MASK_DYNAMIC2 == (Long.parseLong(tamper_new.get(j),16) & MASK_DYNAMIC2))){
                    mTestMask2.setVisibility(View.VISIBLE);
                    mTestMask2.setText(getString(R.string.status_dyanmic2));
                }
                if((MASK_STAIC1 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC1 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC1))){
                    mTestMask3.setVisibility(View.VISIBLE);
                    mTestMask3.setText(getString(R.string.status_static1));
                }
                if((MASK_STAIC2 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC2 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC2))){
                    mTestMask4.setVisibility(View.VISIBLE);
                    mTestMask4.setText(getString(R.string.status_static2));
                }
                if((MASK_STAIC3 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC3 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC3))){
                    mTestMask5.setVisibility(View.VISIBLE);
                    mTestMask5.setText(getString(R.string.status_static3));
                }
                if((MASK_STAIC4 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC4 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC4))){
                    mTestMask6.setVisibility(View.VISIBLE);
                    mTestMask6.setText(getString(R.string.status_static4));
                }
                if(MASK_ROOTKEY_ERROR == Long.parseLong(tamper_type.get(j),16)){
                    mask_type.setVisibility(View.VISIBLE);
                    mask_type.setText(getString(R.string.status_rootkey_error));
                }
                if(MASK_ROOTKEY_LOST == Long.parseLong(tamper_type.get(j),16)){
                    mask_type.setVisibility(View.VISIBLE);
                    mask_type.setText(getString(R.string.status_rootkey_lost));
                }

            }

        }
    }


    public static void setDiscoverableTimeout() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode =BluetoothAdapter.class.getMethod("setScanMode", int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 0);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            LogUtil.d("open Bluetooth search");
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("setDiscoverableTimeout failure:" + e.getMessage());
        }
    }

    class EnableBT extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
            }
            SystemClock.sleep(20000);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            setDiscoverableTimeout();
            super.onPostExecute(o);
        }
    }

    private void checkIsStabilityTest(){
        Intent mIntent = getIntent();
        boolean stability = mIntent.getBooleanExtra("stability",false);
        Log.d("stability test","stability: "+stability);
        if(stability){
            items = new String[]{"26h", "74h", "122h", "242h"};
            items_value = new String[]{"26", "74", "122", "242"};
            checkedItems = new boolean[] { false, false, false, false};
            isStabilityTest = true;
        }else {
            isStabilityTest = false;
        }
    }
    private void checkSetStabilityTestProp(boolean enable) {
        if (isStabilityTest || "true".equals(SystemProperties.get("persist.vendor.cit.stabilitytest"))) {
            try {
                SystemProperties.set("persist.vendor.cit.stabilitytest", ""+enable);
            } catch (Exception e) {
                Log.e(TAG, "checkSetStabilityTestProp " + Log.getStackTraceString(e));
            }
        }
    }

}
