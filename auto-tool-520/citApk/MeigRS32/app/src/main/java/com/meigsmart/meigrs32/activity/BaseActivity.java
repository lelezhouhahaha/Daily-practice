package com.meigsmart.meigrs32.activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.db.FunctionDao;
import com.meigsmart.meigrs32.db.FunctionDao_New;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.OdmCustomedProp;
import com.meigsmart.meigrs32.util.SystemManagerUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final String TAG_ESC_ACTIVITY = "com.broader.esc";
    private MyBroaderEsc receiver;//广播
    private Unbinder butterKnife;//取消绑定
    protected boolean startBlockKeys = true;
    protected boolean isStartTest = false;//start test
    private PowerManager.WakeLock wakeLock = null;
    protected PromptDialog mDialog;
    protected String mName = "";
    protected String mFatherName = "";
    protected List<FunctionBean> mList = new ArrayList<>();
    protected int SUCCESS = 2;
    protected int FAILURE = 1;
    protected int NOTEST = 0;
    private boolean cit1_or_cit2 =false;

    //add float view
    WindowManager mWindowManager;
    WindowManager.LayoutParams wmParams;
    RelativeLayout mFloatLayout;
    TextView mFloatTextView_total;
    TextView mFloatTextView_current;
    TextView mFloatTextView_voltage;
    TextView mFloatTextView_status;
    String mCpuTemperaturePath = "";
    String mScreenOrientationPortraitKey = "common_screen_orientationmn_portrait_config_bool";
    String mScreenSystemConfigKey = "common_screen_follow_system_config_bool";
    String mDefaultSystemLanguageKey = "common_cit_system_default_language";
    private final String mLcdInfoPathKeyword = "common_SoftwareVersionActivity_lcd_version_path";
    private String mLcdInfoStr = "";
    private String mLcdFwVersionStr = "";
    protected String mFailReason="";
    public String WIFI_mt535 = "common_device_wifi_only";
    boolean mScreenOrientationPortraitBool = false;
    boolean mScreenSystemConfig = false;
    boolean mDefaultSystemLanguage = true;
    private boolean isMT537_version =false;
    public boolean isMT535_version =false;
    public boolean WIFI_BUILD = false;
    public boolean WIFI_BUILD1 = false;
    public boolean ERO_BUILD = false;
    public boolean LA_BUILD = false;
    private boolean AIRPRESS_TEST = true;

    private String projectName = "";
    private int m520HardWareVersion = -1;

    boolean isSLB783 = "SLB783".equals(DataUtil.getDeviceName());

    static boolean SAVE_EN_LOG;
    static {
         SAVE_EN_LOG = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_result_default_language"));
    }

    public void setTestFailReason(String reason){
        mFailReason = reason;
    }

    public String getTestFailReason(){
        return mFailReason;
    }

    public void showDialog(String failItemInfo, String testItemInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String info = failItemInfo + getResources().getString(R.string.not_pass) + ", " + getResources().getString(R.string.not_test) + " "  + testItemInfo;
        LogUtil.d("showDialog info；" + info);
        builder.setMessage(info);
        builder.setCancelable(false);
        builder.setNegativeButton(getResources().getString(R.string.main_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i(this.getLocalClassName() + " start test");
        OdmCustomedProp.init();
        // 设置无标题
        ActivityManager manager = (ActivityManager)   getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = manager .getRunningTasks(1);
        ActivityManager.RunningTaskInfo cinfo = runningTasks.get(0);
        ComponentName component = cinfo.topActivity;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if(SystemProperties.get("persist.sys.db_name_cit").equals("cit2_test")){
            cit1_or_cit2=true;
        }
        projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
        isMT537_version = "MT537".equals(projectName);
        isMT535_version = "MT535".equals(projectName);

        if(projectName.equals("MC520") || projectName.equals("MC520_GMS")){
            String hardware_value = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_hardware_version_path_value");
            if (null != hardware_value && !hardware_value.isEmpty()) {
                try {
                    int value_hw = Integer.parseInt(DataUtil.readLineFromFile(hardware_value).trim());
                    m520HardWareVersion = value_hw;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(isMT535_version && (component.getClassName().contains("TouchTestActivity") || component.getClassName().contains("MutiTouchTestActivity"))) {
            hideBottomUIMenu();
        }

        String languageConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mDefaultSystemLanguageKey);
        if(!languageConfig.isEmpty())
            mDefaultSystemLanguage = languageConfig.equals("true");
        else mDefaultSystemLanguage = true;

        if(!mDefaultSystemLanguage) setdefaultLanguage(this);

        {
            if(MyApplication.RuninTestNAME.isEmpty())
                MyApplication.RuninTestNAME = getResources().getString(R.string.function_run_in);
            if(MyApplication.PreSignalNAME.isEmpty())
                MyApplication.PreSignalNAME = getResources().getString(R.string.function_pre_function_signal);
            if(MyApplication.PreNAME.isEmpty())
                MyApplication.PreNAME = getResources().getString(R.string.function_pre_function);
            if(MyApplication.MMI1_PreName.isEmpty())
                MyApplication.MMI1_PreName = getResources().getString(R.string.mmi_one_test_auto);
            if(MyApplication.MMI1_PreSignalName.isEmpty())
                MyApplication.MMI1_PreSignalName = getResources().getString(R.string.mmi_one_test_manual);
            if(MyApplication.MMI2_PreName.isEmpty())
                MyApplication.MMI2_PreName = getResources().getString(R.string.mmi_two_test_auto);
            if(MyApplication.MMI2_PreSignalName.isEmpty())
                MyApplication.MMI2_PreSignalName = getResources().getString(R.string.mmi_two_test_manual);
            if(MyApplication.InformationCheckName.isEmpty())
                MyApplication.InformationCheckName = getResources().getString(R.string.function_information_check);
            if (SAVE_EN_LOG) {
                if(MyApplication.PCBASignalNAME.isEmpty())
                    MyApplication.PCBASignalNAME = PCBASignalActivity.class.getSimpleName();
                if(MyApplication.PCBANAME.isEmpty())
                    MyApplication.PCBANAME = PCBAActivity.class.getSimpleName();
            } else {
                if(MyApplication.PCBASignalNAME.isEmpty())
                    MyApplication.PCBASignalNAME = getResources().getString(R.string.function_pcba_signal);
                if(MyApplication.PCBANAME.isEmpty())
                    MyApplication.PCBANAME = getResources().getString(R.string.function_pcba);
            }

        }

        setContentView(getLayoutId());
        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mScreenOrientationPortraitKey);
        if(!tmpStr.isEmpty())
            mScreenOrientationPortraitBool = tmpStr.equals("true");
        else mScreenOrientationPortraitBool = true;

        String defaultStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mScreenSystemConfigKey);
        if(!defaultStr.isEmpty())
            mScreenSystemConfig = defaultStr.equals("true");
        else mScreenSystemConfig = false;
        LogUtil.d("citapk mScreenSystemConfig:" + mScreenSystemConfig);
        LogUtil.d("citapk mScreenOrientationPortraitBool:" + mScreenOrientationPortraitBool);
        if(!mScreenSystemConfig) {
            if (mScreenOrientationPortraitBool) {
                if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {//设置为竖屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else {
                if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {//设置为横屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        }
        acquireWakeLock();
        if(isMT537_version){
            mCpuTemperaturePath = getResources().getString(R.string.cpu_default_config_cpu_temperature_path_new);
        }else {
            mCpuTemperaturePath = getResources().getString(R.string.cpu_default_config_cpu_temperature_path);
        }
        // 注册广播
        receiver = new MyBroaderEsc();
        registerReceiver(receiver, new IntentFilter(TAG_ESC_ACTIVITY));
        mDialog = new PromptDialog(this,R.style.MyDialogStyle);
        // 反射注解机制初始化
        butterKnife = ButterKnife.bind(this);
        initFloatView();
        initData();
        LogUtil.i(this.getLocalClassName() + " end test");
    }

    /**
     * Hide virtual keys
     */
    protected void hideBottomUIMenu() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            Window _window = getWindow();
            WindowManager.LayoutParams params = _window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE;
            _window.setAttributes(params);
        }
    }

    protected void initFloatView() {
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
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.float_view, null);
        mWindowManager.addView(mFloatLayout, wmParams);
        mFloatTextView_total= (TextView) mFloatLayout.findViewById(R.id.float_textView_total);
        mFloatTextView_current= (TextView) mFloatLayout.findViewById(R.id.float_textView_current);
        mFloatTextView_voltage= (TextView) mFloatLayout.findViewById(R.id.float_textView_voltage);
        mFloatTextView_status= (TextView) mFloatLayout.findViewById(R.id.float_textView_status);
    }

    protected void updateFloatView(Context context, int currentLeftTime) {

        if(MyApplication.RuninTestNAME.equals(getIntent().getStringExtra("fatherName"))) {
            mFloatTextView_total.setText(context.getResources().getString(R.string.total_left_time) + DataUtil.formatTime(RuninConfig.getLeftTotalRuninTime(context)/1000));
            mFloatTextView_current.setText(context.getResources().getString(R.string.current_left_time) + DataUtil.formatTime(currentLeftTime));
            if(!isMT535_version) {
                if (isMT537_version) {
                    mFloatTextView_voltage.setText(context.getResources().getString(R.string.version_default_config_software_version_battery_capacity) + DataUtil.readLineFromFile("/sys/class/power_supply/bq27xxx_battery/capacity") + "%");
                } else {
                    mFloatTextView_voltage.setText(context.getResources().getString(R.string.version_default_config_software_version_battery_capacity) + DataUtil.readLineFromFile("/sys/class/power_supply/battery/capacity") + "%");
                }
                String charge_status = "";
                if (DataUtil.readLineFromFile("/sys/class/power_supply/battery/status").contains("Charging")) {
                    charge_status = context.getResources().getString(R.string.Charging);
                } else {
                    charge_status = context.getResources().getString(R.string.Discharging);
                }
                mFloatTextView_status.setText(context.getResources().getString(R.string.change_now) + charge_status);
            }
        } else {
            mFloatTextView_total.setText("");
            mFloatTextView_current.setText("");
            mFloatTextView_voltage.setText("");
            mFloatTextView_status.setText("");
        }
    }

    protected abstract int getLayoutId();//获取布局layout

    protected abstract void initData();//初始化数据

    class MyBroaderEsc extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                //butterKnife.unbind(); //no need
                finish();
            }
        }
    }

    protected List<TypeModel> getData(String[] array, int[] ids, Class[] cls,List<FunctionBean> f){
        List<TypeModel> list = new ArrayList<>();
        for (int i=0;i<array.length;i++){
            if (ids[i] == 1){
                TypeModel model = new TypeModel();
                model.setId(i);
                model.setName(array[i]);
                model.setCls(cls[i]);
                if ( f!=null && f.size()>0){
                    for (FunctionBean bean : f){
                        if (array[i].equals(bean.getSubclassName())){
                            model.setType(bean.getResults());
                            break;
                        }
                    }
                }else{
                    model.setType(0);
                }
                list.add(model);
            }
        }
        return list;
    }

    protected String getStringFromName(Context context, String str){
        String name = "";
        int resId = context.getResources().getIdentifier(str, "string", getPackageName() );
        if(resId==0){
            name=str;
        }else name = context.getResources().getString(resId);
        return name;
    }

    protected boolean isCameraExist(){

        String cameraValue = "";
        try {
            FileReader file = new FileReader("/sys/camera_choose/camera_value");
            char[] buffer = new char[1024];
            int len = file.read(buffer, 0, 1024);
            cameraValue = new String(buffer, 0, len);
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("citapk isCameraExist Exception");
            return true;
        }
        LogUtil.d("citapk cameraValue:[" + cameraValue +"].");
        return cameraValue.trim().equals("A");
    }
    

    protected boolean isRkNFCExist(){
        String nfcModel = SystemProperties.get("ro.boot.nfcmodel","1");
        return "1".equals(nfcModel);
    }

    protected String getSubstring(String str, String key){

        String name = "";
        String[] subStr = str.split(key);
        name = subStr[subStr.length-1];
        return name;
    }

    /*protected List<TypeModel> getDatas(Context context, int[] ids, Class[] cls,List<FunctionBean> f){
        List<TypeModel> list = new ArrayList<>();
        for (int i=0;i<cls.length;i++){
            if (ids[i] == 1){
                TypeModel model = new TypeModel();
                model.setId(i);
                if(cls[i].equals(Class.class))
                    continue;

                String title = getStringFromName(context, getSubstring(cls[i].getName(), "\\."));
                model.setName(title);
                model.setCls(cls[i]);
                if ( f!=null && f.size()>0){
                    for (FunctionBean bean : f){
                        if (title.equals(bean.getSubclassName())){
                            model.setType(bean.getResults());
                            break;
                        }
                    }
                }else{
                    model.setType(0);
                }
                list.add(model);
            }
        }
        return list;
    }*/

    protected List<TypeModel> getDatas(Context context, List<String> ids,List<FunctionBean> f){
        List<TypeModel> list = new ArrayList<>();
        int i = 0;
        String title = "";
        /*boolean WIFI_BUILD = false;
        boolean WIFI_BUILD1 = false;
        boolean ERO_BUILD = false;
        boolean LA_BUILD = false;*/
        String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RIL_STATE);
        String wifi_path1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
        if(((null != wifi_path) && !wifi_path.isEmpty())||((null != wifi_path1) && !wifi_path1.isEmpty())){
            WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
            WIFI_BUILD1 = FileUtil.readFromFile(wifi_path1).contains("0");
            ERO_BUILD = FileUtil.readFromFile(wifi_path1).contains("1");
            LA_BUILD = FileUtil.readFromFile(wifi_path1).contains("2");
        }
        LogUtil.d("citapk mFatherName:" + mFatherName);
        String scanType = DataUtil.readLineFromFile("/dev/sunmi/scan/scan_head_type");
        for (String className : ids){
            if((WIFI_BUILD || SystemProperties.get("ro.radio.noril", "").equals("true")|| SystemProperties.get("ro.radio.noril", "").equals("yes"))
               && (className.contains("SIM") || className.contains("Sim") || className.contains("Gps"))) {
                LogUtil.d("current version is wifi version, className:" + className);
                continue;
            }
			//please set new nopmi cit_config.xml for no pmi project,this modify cause problems
            /*if((SystemProperties.get("ro.boot.pmi_mode", "").equals("nopmi")) &&
                    (className.contains("Otg") || className.contains("Vibrator") || className.contains("Battery") || className.contains("FlashLight"))){
                LogUtil.d("current version is nopmi version, className:" + className);
                continue;
            }*/

            //wifi配置 WIFI_BUILD1
            if(WIFI_BUILD1 && (className.contains("SIM") || className.contains("Sim") || className.contains("Gps")
            || className.contains("Rear2CameraAutoActivity")||className.contains("FillLightActivity")
            || className.contains("GSensorActivity")||className.contains("RecordActivity")
                    ||className.contains("StorageCardActivity")||className.contains("_MagEncActivity")
                    ||className.contains("TaxTestActivity")||className.contains("UsbOtgActivity")||className.contains("POETestActivity")
                    ||className.equals("RunIn_FrontCameraAutoActivity")||className.equals("SIM1Activity")||className.equals("SIM2Activity"))){
                LogUtil.d("current version is wifi version1, className:" + className);
                continue;
            }
            //欧洲配置 ERO_BUILD
            if(ERO_BUILD && (className.contains("FillLightActivity")||className.contains("_MagEncActivity")
                    ||className.equals("RunIn_FrontCameraAutoActivity")||className.equals("SIMActivity")
                    ||className.equals("SimHotPlugActivity")||className.equals("RecordActivity"))){
                LogUtil.d("current version is Europe version, className:" + className);
                continue;
            }
           //拉美配置 LA_BUILD
            if(LA_BUILD && (className.equals("MagEncActivity")||className.equals("SIM1Activity")
                    ||className.equals("SIM2Activity")||className.equals("Sim1HotPlugActivity")
            || (className.equals("FrontCameraAutoActivity") && MyApplication.RuninTestNAME.equals(mName)))){
                Log.d("citapk","current version is LA version, className:" + className);
                continue;
            }
            if(isMT537_version){
                String mLcdInfoPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mLcdInfoPathKeyword);
                if(!mLcdInfoPath.isEmpty()){
                    String mLcdInfoAll =  FileUtil.readFile(mLcdInfoPath);
                    String[] mLcdInfoArrays=mLcdInfoAll.split("\n");
                    for(int j = 0; j< mLcdInfoArrays.length; j++) {
                        if(mLcdInfoArrays[j].contains("Lcd_Driver_Ic :")){
                            mLcdFwVersionStr = FileUtil.replaceBlank(mLcdInfoArrays[j].substring(mLcdInfoArrays[j].lastIndexOf(":") + 1) );
                        }else if(mLcdInfoArrays[j].contains("Lcd_Manufacturer :")){
                            mLcdInfoStr = FileUtil.replaceBlank(mLcdInfoArrays[j].substring(mLcdInfoArrays[j].lastIndexOf(":") + 1) );
                        }
                    }
                    if(mLcdInfoStr.equals("fangxing")){
                        if(className.equals("TpCapacity_New_Activity")) {
                            continue;
                        }
                    }else{
                        if(className.equals("TpCapacity_Fangxing_Activity")) {
                            continue;
                        }
                    }
                }
            }

            if(isSLB783) {
                if (!isCameraExist() && (className.contains("FrontCameraAutoActivity") || className.contains("FlashLightActivity") || className.contains("RearCameraAutoActivity") || className.contains("Rear2CameraAutoActivity")) && !(MyApplication.PCBANAME.equals(mName) || MyApplication.PCBASignalNAME.equals(mName)))
                    continue;
            }

            if(!isRkNFCExist() && (className.contains("NFCActivity")) && !(MyApplication.PCBANAME.equals(mName) || MyApplication.PCBASignalNAME.equals(mName)) ){
                String nfcName = context.getResources().getString(R.string.NFCActivity);
                if(cit1_or_cit2) {
                    new FunctionDao_New(getApplicationContext()).delete(mName,nfcName);
                }else {
                    new FunctionDao(getApplicationContext()).delete(mName, nfcName);
                }
                LogUtil.d("mName:"+mName+", nfcName:" + nfcName);
                continue;
            }

            if (!className.isEmpty() && !className.equals("")){
                Class cls = null;
                String name = "";
                TypeModel model = new TypeModel();
                model.setId(i);

                /*if ((className.contains("zebra") || className.contains("oemscandemo"))) {
                    String scanType = DataUtil.readLineFromFile("/dev/sunmi/scan/scan_head_type");
                    LogUtil.d("citapk scanType:" + scanType);
                    if (!scanType.isEmpty() && scanType.contains("HONEYWELL")) {
                        className = "com.example.oemscandemo/.MainActivity";
                    } else if (!scanType.isEmpty() && scanType.contains("SS1100")) {
                        className = "com.meigsmart.meigrs32/.activity.ScanActivitySS1100";
                    } else {
                        className = "com.zebra.sdl/.SDLguiActivity";
                    }
                    LogUtil.d("citapk L2s className:" + className);
                }*/

                if (!scanType.isEmpty() && scanType.contains("SS1100")) {
                    if(className.contains("ScanActivityFarFocus")){
                        className = "ScanActivitySS1100FarFocus";
                    }else if(className.contains("ScanActivityNearFocus")) {
                        className = "ScanActivitySS1100NearFocus";
                    }
                }
                try{
                    AIRPRESS_TEST = SystemProperties.getBoolean("persist.vendor.cit.airpresstest", true);
                    LogUtil.d("citapk AIRPRESS_TEST:" + AIRPRESS_TEST);
                }catch (Exception e){
                }

                if(className.equals("AirPresActivity") && !AIRPRESS_TEST){
                    LogUtil.d("citapk C continue className:" + className);
                    continue;
                }

                if(SystemProperties.get("ro.boot.yyversion").equals("yes")/*mFatherName.equals(getResources().getString(R.string.C_YY))*/){
                    if(className.equals("HallActivity") || className.equals("AirPresActivity") || className.equals("BatterySwitchActivity")
                            || className.equals("PSAMActivity") || className.equals("PogoPin8ChargeActivity")
                            || className.equals("PogopinOtgActivity") || className.equals("TemperatureActivity")
                            || className.equals("SARSensorActivity") || className.equals("I2CActivity")
                            || className.equals("UARTActivity") || className.equals("GpioSunmiActivity")){
                        LogUtil.d("citapk C continue className:" + className);
                        continue;
                    }
                }else if(( m520HardWareVersion < 2000) && ( m520HardWareVersion != -1 ) ){
                    if( className.equals("SARSensorActivity") ){
                        LogUtil.d("citapk C continue className:" + className);
                        continue;
                    }
                }

                if(className.contains("/") || className.contains("*")) {
                    String pkgName = "";
                    String clsName = "";
                    if(className.contains("/")) {
                        pkgName = className.substring(0, className.indexOf("/"));
                        clsName = pkgName + className.substring(className.indexOf("/") + 1);
                        title = getStringFromName(context, className.replace("/", ""));
                    }else if(className.contains("*")){
                        pkgName = className.substring(0, className.indexOf("*"));
                        clsName = className.substring(className.indexOf("*")+1);
                        title = getStringFromName(context, clsName);
                    }
                    LogUtil.d("citapk packageName:" + pkgName);
                    LogUtil.d("citapk clsName:" + clsName);
                    model.setPackageName(pkgName);
                    model.setClassName(clsName);
                    model.setStartType(1);
                    model.setName(title);
                    name = "com.meigsmart.meigrs32.activity." + "StartSingleActivity";
                }else {
                    model.setStartType(0);
                    title = getStringFromName(context, className);
                    model.setName(title);
                    name = "com.meigsmart.meigrs32.activity." + className;
                }
                LogUtil.d("citapk  name:" + name);

                String checkStr = title;
                try {
                    cls = Class.forName(name);
                    model.setCls(cls);
                    if (SAVE_EN_LOG)  checkStr = cls.getSimpleName();
                } catch (ClassNotFoundException e) {
                    LogUtil.d("not found class " + name);
                }


                if ( f!=null && f.size()>0){
                    for (FunctionBean bean : f){
                        if (checkStr.equals(bean.getSubclassName())){
                            model.setType(bean.getResults());
                            break;
                        }
                    }
                }else{
                    model.setType(0);
                }
                i++;
                list.add(model);
            }
        }
        return list;
    }

    protected void startActivity(TypeModel model){
        if (model.getCls().equals(Class.class)){
            ToastUtil.showBottomShort(getResources().getString(R.string.to_be_developed));
            return;
        }
        if (model.getCls() != null){
            Intent intent = new Intent(this,model.getCls());
            if(!(this.mName==null)){
                intent.putExtra("fatherName",this.mName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }else{
                intent.putExtra("fatherName",mFatherName);
                intent.putExtra("name", SAVE_EN_LOG ? model.getCls().getSimpleName() : model.getName());
            }
            if(model.getStartType() == 1){
                LogUtil.d("citapk 1");
                intent.putExtra("packageName", model.getPackageName());
                intent.putExtra("className", model.getClassName());
            }
            startActivityForResult(intent,1111);
        }
    }

    protected void sendErrorMsg(Handler handler,String error){
        Message msg = handler.obtainMessage();
        msg.what = 9999;
        msg.obj = error;
        handler.sendMessage(msg);
    }

    protected void sendErrorMsgDelayed(Handler handler,String error){
        Message msg = handler.obtainMessage();
        msg.what = 9999;
        msg.obj = error;
        handler.sendMessageDelayed(msg,2000);
    }


    public String getDbName(String fatherName){//手动测试和自动测试使用同一份数据库

        if(MyApplication.PCBASignalNAME.equals(fatherName)){
            return MyApplication.PCBANAME;
        }else if(MyApplication.PreSignalNAME.equals(fatherName)){
            return MyApplication.PreNAME;
        }else if(MyApplication.MMI1_PreSignalName.equals(fatherName)){
            return MyApplication.MMI1_PreName;
        }else if(MyApplication.MMI2_PreSignalName.equals(fatherName)){
            return MyApplication.MMI2_PreName;
        }
        return fatherName;

    }

    protected void addData(String fatherName,String subName){
        if( (fatherName == null) || fatherName.isEmpty() || (subName == null) || (subName.isEmpty())){
            LogUtil.d("addData parameter == null");
            return;
        }
        FunctionBean sb = getSubData(fatherName, subName);
        if (sb!=null && !TextUtils.isEmpty(sb.getSubclassName()) && !TextUtils.isEmpty(sb.getFatherName()))return;

        FunctionBean bean = new FunctionBean();
        bean.setFatherName(getDbName(fatherName));
        bean.setSubclassName(subName);
        bean.setResults(0);
        bean.setReason("NOTEST");
        if(cit1_or_cit2){
            new FunctionDao_New(getApplicationContext()).addData(bean);
        }else{
            new FunctionDao(getApplicationContext()).addData(bean);
        }
    }

    protected void updateData(String fatherName, String subName, int result){
        if(cit1_or_cit2) {
            new FunctionDao_New(getApplicationContext()).update(getDbName(fatherName), subName, result, "");
        }else {
            new FunctionDao(getApplicationContext()).update(getDbName(fatherName), subName, result, "");
        }
    }

    protected void updateData(String fatherName, String subName, int result,String reason){
        if(cit1_or_cit2) {
            new FunctionDao_New(getApplicationContext()).update(getDbName(fatherName),subName,result,reason);
        }else {
            new FunctionDao(getApplicationContext()).update(getDbName(fatherName), subName, result, reason);
        }
    }

    protected FunctionBean getSubData(String fatherName, String subName){
        if(cit1_or_cit2) {
            return new FunctionDao_New(getApplicationContext()).getSubData(getDbName(fatherName), subName);
        }else {
        return new FunctionDao(getApplicationContext()).getSubData(getDbName(fatherName), subName);
        }
    }

    protected List<FunctionBean> getFatherData(String fatherName){
        if(cit1_or_cit2) {
            return new FunctionDao_New(getApplicationContext()).getFatherData(getDbName(fatherName));
        }else {
            return new FunctionDao(getApplicationContext()).getFatherData(getDbName(fatherName));
        }
    }

    protected List<FunctionBean> getAllData(){
        if(cit1_or_cit2) {
            return new FunctionDao_New(getApplicationContext()).getAllData();
        }else {
            return new FunctionDao(getApplicationContext()).getAllData();
        }
    }

    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, this.getClass() .getCanonicalName());
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    private void releaseWakeLock() {
        if (null != wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtil.d("BaseAcitivity", "onConfigurationChanged ");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        mFloatLayout.setVisibility(View.VISIBLE);
        StatusBarManager mStatusBarManager = (StatusBarManager) getApplicationContext().getSystemService(Context.STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mFloatLayout.setVisibility(View.GONE);
        StatusBarManager mStatusBarManager = (StatusBarManager) getApplicationContext().getSystemService(Context.STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseWakeLock();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if(mWindowManager != null && mFloatLayout != null ) {
            mWindowManager.removeViewImmediate(mFloatLayout);
        }
        LogUtil.i(this.getLocalClassName() + " onDestroy");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
                if (startBlockKeys) return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void deInit(String fatherName, int results) {
        deInit(fatherName, results, "");
    }

    protected void deInit(String fatherName, int results,String reason){
        if (mDialog.isShowing())mDialog.dismiss();
        if (!(MyApplication.PCBASignalNAME.equals(fatherName))
                && !(MyApplication.PreSignalNAME.equals(fatherName))
                && !(MyApplication.MMI1_PreSignalName.equals(fatherName))
                && !(MyApplication.MMI2_PreSignalName.equals(fatherName))) {
            LogUtil.d("citapk fatherName:" + fatherName);
            LogUtil.d("citapk mName:" + mName);
            updateData(fatherName, mName, results, reason);
        }
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        finish();
    }

    public void setdefaultLanguage(Context context) {
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        config.locale = Locale.SIMPLIFIED_CHINESE;
        context.getResources().updateConfiguration(config, metrics);
    }
}
