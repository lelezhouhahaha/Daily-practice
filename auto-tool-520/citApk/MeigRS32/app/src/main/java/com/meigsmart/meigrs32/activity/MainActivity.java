package com.meigsmart.meigrs32.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.content.DialogInterface;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.FunctionListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.SoftDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executors;

import butterknife.BindArray;
import butterknife.BindView;

public class MainActivity extends BaseActivity implements FunctionListAdapter.OnFunctionItemClick,SoftDialog.OnSoftCallBack,SensorEventListener {
    private MainActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    private FunctionListAdapter mAdapter;
    private String mBtConfig;
    private String mFt1Config;
    private String mFt2Config;
    private String mWifiConfig;
    private WifiManager mWifimanager = null;
    private String TAG_LED_PATH = "ledPath";
    private Sensor defaultSensor;
    private SensorManager mSensorManager;
    private final String FIRST_BOOT = "sys.first.boot.cit";
    private Boolean firstboot = false;
    private String mWifiDefaultStausKey = "common_wifi_test_default_status_bool_config";
    private boolean mWifiDefaultStaus = false;
    private String mBtDefaultStausKey = "common_bt_test_default_status_bool_config";
    private boolean mBtDefaultStaus = false;
    private String projectName = "";
    private  boolean isMC520_version = false;
    private boolean isNfcClose = false;

    //for MC511 psam icc picc msr buzzer printer test
    private String mCastlesTestKey = "common_castle_test_enable";
    private final  String CONFIG_LED_STATE = "common_led_state_test";

    public static final String START_TEST_COMMAND = "castles enter_test";
    public static final String LEAVE_TEST_COMMAND = "castles leave_test";
    private String p_msg,p_command;
    private Handler mHandler = new Handler();
    public static final String ACTION_ENTER_MIDTEST = "com.sunmi.cit.action.ACTION_OPEN";// cit enter
    public static final String ACTION_EXIT_MIDTEST = "com.sunmi.cit.action.ACTION_CLOSE"; // cit exit
    private static final String ACTION_GETIMEI="com.android.action.GET_IMEI";
    private final String TAG = MainActivity.class.getSimpleName();
    private String scanType = "";
    public boolean AIRPRESS_TEST = true;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    private void closeNfc(){
       try{
           NfcAdapter mDefaultAdapter = NfcAdapter.getDefaultAdapter(mContext);
           if(mDefaultAdapter!=null){
               mDefaultAdapter.disable();
               Log.e(TAG, "closeNfc normal");
               mDefaultAdapter = null;
           }

       }catch (Exception e){
           Log.e(TAG, "closeNfc abnormal");
       }
   }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OdmCustomedProp.init();
        try{
            SystemProperties.set("persist.vendor.cit.flag", "true");
        }catch (Exception e){
            Log.e(TAG,"onCreate " + Log.getStackTraceString(e));
        }
        Log.d(TAG, "persist.vendor.cit.flag:" + SystemProperties.get("persist.vendor.cit.flag"));

        //init log path
        LogUtil.initialize(this,true);
        //turn off led for ledtest
        Intent enterCit = new Intent(ACTION_ENTER_MIDTEST);
        enterCit.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(enterCit);
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMC520_version = "MC520".equals(projectName);
        if(isMC520_version){
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            intent.setAction(ACTION_GETIMEI);
            sendBroadcast(intent);

            Intent action = getIntent();
            AIRPRESS_TEST = action.getBooleanExtra("airpresstest",true);
            SystemProperties.set("persist.vendor.cit.airpresstest", AIRPRESS_TEST+"");
            LogUtil.d("AIRPRESS_TEST: " + AIRPRESS_TEST);
        }
        LogUtil.d("sendBroadcast ACTION_ENTER_MIDTEST:" + ACTION_ENTER_MIDTEST);
        if("MT537".equals(projectName)) {
            String scan_type = FileUtil.readFromFile("/dev/sunmi/scan/scan_head_type");
            //SystemProperties.set("persist.custmized.scan_version",scan_type);
            SystemProperties.set(OdmCustomedProp.getScanVersionProp(),scan_type);
            Executors.newCachedThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    if (!MyApplication.getInstance().isConnectPaySDK()) {
                        MyApplication.getInstance().bindPaySDKService();
                        return;
                    }
                }
            });
            String face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
            if (face_select == null || face_select.equals("") || face_select.length() != 8) {
                showDialog();
            }
        }

        turnOffLed();
        //active ORIENTATION sensor for EComPass first test
        activeSensor();

        super.onCreate(savedInstanceState);
    }

    public void showDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        String info = getResources().getString(R.string.no_set_sku);
        builder.setMessage(info);
        builder.setCancelable(false);
        builder.setNegativeButton(getResources().getString(R.string.set_sku_go), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mTitle.setText(R.string.main_title);
        MyApplication.RuninTestNAME = getResources().getString(R.string.function_run_in);
        Intent intent = getIntent();
        if (SAVE_EN_LOG) {
            MyApplication.PCBASignalNAME = PCBASignalActivity.class.getSimpleName();
            MyApplication.PCBANAME = PCBAActivity.class.getSimpleName();
        } else {
            MyApplication.PCBASignalNAME = getResources().getString(R.string.function_pcba_signal);
            MyApplication.PCBANAME = getResources().getString(R.string.function_pcba);
        }
        MyApplication.PreSignalNAME = getResources().getString(R.string.function_pre_function_signal);
        MyApplication.PreNAME = getResources().getString(R.string.function_pre_function);
        MyApplication.MMI1_PreName =getResources().getString(R.string.mmi_one_test_auto);
        MyApplication.MMI1_PreSignalName =getResources().getString(R.string.mmi_one_test_manual);
        MyApplication.MMI2_PreName =getResources().getString(R.string.mmi_two_test_auto);
        MyApplication.MMI2_PreSignalName =getResources().getString(R.string.mmi_two_test_manual);
        MyApplication.InformationCheckName = getResources().getString(R.string.function_information_check);
        if(isMC520_version && !isNfcClose){
            closeNfc();
            isNfcClose = true;
        }

        String strWifi = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mWifiDefaultStausKey);
        if(!strWifi.isEmpty())
            mWifiDefaultStaus = strWifi.equals("true");

        String strBt = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mBtDefaultStausKey);
        if(!strBt.isEmpty())
            mBtDefaultStaus = strBt.equals("true");
        String PASS = getResources().getString(R.string.test_flag_default_config);
        boolean isTest = getResources().getBoolean(R.bool.main_default_config_is_test);
        //read NV6854
        mBtConfig = SystemProperties.get(getResources().getString(R.string.main_default_config_bt_persist), getString(R.string.unknown));
		//read NV6855
        mFt1Config = SystemProperties.get(getResources().getString(R.string.main_default_config_ft1_persist), getString(R.string.unknown));
		//read NV6856
        mFt2Config = SystemProperties.get(getResources().getString(R.string.main_default_config_ft2_persist), getString(R.string.unknown));
		//read NV6857
        mWifiConfig = SystemProperties.get(getResources().getString(R.string.main_default_config_wifi_persist), getString(R.string.unknown));
        setdefaultLanguage(MainActivity.this);


		if( isTest && ( !mBtConfig.equals( PASS ) || !mFt1Config.equals( PASS ) || !mFt2Config.equals( PASS ) || !mWifiConfig.equals( PASS ) ) ) {
			//start dailog.
            SoftDialog dialog = new SoftDialog(this,R.style.MyDialogStyle);
            dialog.setmCallBack(this);
            if (!dialog.isShowing())dialog.show();
            StringBuffer sb = new StringBuffer();
            sb.append(getString( R.string.test_bt )).append(" ").append(mBtConfig).append("\n");
            sb.append(getString( R.string.test_ft1 )).append(" ").append(mFt1Config).append("\n");
            sb.append(getString( R.string.test_ft2 )).append(" ").append(mFt2Config).append("\n");
            sb.append(getString( R.string.test_wifi )).append(" ").append(mWifiConfig).append("\n");

            dialog.setContentTitle(sb.toString());

		} else {
			mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
			mAdapter = new FunctionListAdapter(this,mContext);
			mRecyclerView.setAdapter(mAdapter);

            List<String> config = Const.getXmlConfig(this,Const.CONFIG_FUNCTION);
            List<TypeModel> list = getDatas(mContext, config, super.mList);
            mAdapter.setData(list);
		}
        if("true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCastlesTestKey))){
            try {
                runShellCommand(START_TEST_COMMAND);
                LogUtil.i("START_TEST_COMMAND");
            }catch (Exception e) {
                LogUtil.e("START_TEST_COMMAND: " +e.getMessage());
            }
        }
    }

    @Override
    public void onClickItem(int position) {
        if(!DataUtil.isFastClick()) {
            LogUtil.d(this.getLocalClassName() + " currPosition: " + position);
            startActivity(mAdapter.getData().get(position));
        }else LogUtil.d(this.getLocalClassName() + " MainActivity click too fast.");
    }



    final Runnable test_runnable = new Runnable() {
        public void run() {
            new Thread(new Runnable(){
                public void run()
                {
                    try {
                        String line;
                        Process process = Runtime.getRuntime().exec(p_command);
                        process.waitFor();
                        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while ((line = input.readLine()) != null) {
                            p_msg = p_msg + line + "\n";
                        }
                        LogUtil.i("p_msg="+p_msg);
                        input.close();
                    }
                    catch (Exception e) {
                        LogUtil.d("DebugMsg", e.getMessage());
                    }
                }
            }).start();
        }};

    private void runShellCommand(String command) throws Exception {
        p_command = command;
        p_msg = "";
        mHandler.post(test_runnable);

    }



    @Override
    protected void onDestroy() {
        Intent exitCit = new Intent(ACTION_EXIT_MIDTEST);
        exitCit.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(exitCit);
        LogUtil.d("sendBroadcast ACTION_EXIT_MIDTEST:" + ACTION_EXIT_MIDTEST);

//        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (bluetoothAdapter.isEnabled() && !mBtDefaultStaus) {
//            bluetoothAdapter.disable();
//        }

        if("true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mCastlesTestKey))){
            try {
                runShellCommand(LEAVE_TEST_COMMAND);
                LogUtil.i("LEAVE_TEST_COMMAND");
            }catch (Exception e) {
                LogUtil.e("LEAVE_TEST_COMMAND: " +e.getMessage());
            }
        }

        mWifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifimanager.isWifiEnabled() && !mWifiDefaultStaus) {
            mWifimanager.setWifiEnabled(false);
        }
        Intent i = new Intent(BaseActivity.TAG_ESC_ACTIVITY);
        mContext.sendBroadcast(i);
        try {
            SystemProperties.set("persist.vendor.cit.flag", "false");
        }catch (Exception e){
            Log.e(TAG,"onDestroy " + Log.getStackTraceString(e));
        }
        super.onDestroy();
    }


    @Override
    protected void onStart() {
        //active bt for display MAC address normal
        activeBT();
        //active wifi for display MAC address normal
        activeWifi();
        super.onStart();
    }

    @Override
    public void onClickSure() {
        mContext.finish();
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void activeSensor(){
        try {
            firstboot = SystemProperties.getBoolean(FIRST_BOOT,true);
            if(firstboot) {
                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                defaultSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
                mSensorManager.registerListener(MainActivity.this, defaultSensor, SensorManager.SENSOR_DELAY_GAME);
                SystemProperties.set(FIRST_BOOT, "false");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    protected void onResume() {
        initData();
        super.onResume();
    }

    @Override
    protected void onStop() {
        if(mSensorManager != null){
            mSensorManager.unregisterListener(this);
        }
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void activeBT(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
    }

    private void activeWifi(){
        mWifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(true);
        }
    }

    private void turnOffLed(){
        try {
            boolean isSkip = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CONFIG_LED_STATE).contains("true");
            if(isSkip){
                return;
            }
            InputStream inputStream = new FileInputStream(Const.LEDTEST_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String ledPath = null;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(TAG_LED_PATH.equals(startTagName)){
                            ledPath = xmlPullParser.nextText();
                            if(ledPath != null){
                                writeToFile(ledPath, "0");
                            }else{
                                Log.d("led_demo","no path");
                            }

                        }
                        ledPath = null;
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
