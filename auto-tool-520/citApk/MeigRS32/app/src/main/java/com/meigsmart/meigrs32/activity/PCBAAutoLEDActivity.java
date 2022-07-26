package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.CitTestJni;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import butterknife.BindView;

public class PCBAAutoLEDActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack{
    private PCBAAutoLEDActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.led_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.content_show)
    public TextView textView;
    private Map<String, TestLed> mTestLedMap0 = new TreeMap<String, TestLed>();
    private Map<String, TestLed> mTestLedMap1 = new TreeMap<String, TestLed>();

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;
    private String TAG =  "LedTest";
    private String TAG_CONFIG = "led_config";
    private String TAG_LINE = "test_line";
    private String TAG_LED_TEST = "led_test";
    private String TAG_LED_NAME = "ledName";
    private String TAG_LED_PATH = "ledPath";
    private String TAG_LED_ON_NAME = "ledOnName";
    private String TAG_LED_ON_VALUE = "ledOnValue";
    private String TAG_LED_OFF_NAME = "ledOffName";
    private String TAG_LED_OFF_VALUE = "ledOffValue";
    private String TAG_LED_GROUP = "ledGroup";

    private Handler mDelayHandler= null;
    private Runnable mDelayRunnable= null;
    private int DELAY_TIME = 500;
    private Iterator<Map.Entry<String, TestLed>> iterator0;
    private Iterator<Map.Entry<String, TestLed>> iterator1;
    private String mFatherName = "";
    private boolean isSLB783 = DataUtil.getDeviceName().equals("SLB783");
    private Boolean ledState = false;
    private final String CONFIG_LED_STATE = "common_led_state_test";
    private int mRspStatus = -1;
    private String mLedName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_led;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_led);
        writeToFile("/sys/class/leds/cit_mode/enable","1");

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        mLedName = getIntent().getStringExtra("ledname");
        LogUtil.d(TAG, "mLedName:" + mLedName);
        addData(mFatherName, super.mName);
		
		if(mFatherName.equals(MyApplication.RuninTestNAME)){
            mSuccess.setVisibility(View.GONE);
            mFail.setVisibility(View.GONE);
        }else {
            mSuccess.setVisibility(View.GONE);
            mSuccess.setOnClickListener(this);
            mFail.setOnClickListener(this);
        }

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        String Number = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CONFIG_LED_STATE);
        if (Number != null && !Number.isEmpty() && Number.contains("true")){
            ledState = true;
        }
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else if (mFatherName.equals(MyApplication.PCBAAutoTestNAME)) {
            mConfigTime  = 2;
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if(( mConfigTime == 0 ) && ( mFatherName.equals(MyApplication.PCBAAutoTestNAME) )){
                    deInit(mFatherName, NOTEST);
                    return;
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

        initLayout();
        iterator0 = mTestLedMap0.entrySet().iterator();
        iterator1 = mTestLedMap1.entrySet().iterator();
        mHandler.sendEmptyMessageDelayed(1002,DELAY_TIME);
    }

    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
            setTestFailReason(String.format(getResources().getString(R.string.fail_reason_write_file), path));
            Log.d(TAG, getTestFailReason());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * led show depend on xml.
     */
    private void initLayout() {
        File configFile = new File(Const.LEDTEST_CONFIG_XML_PATH);
        if(!configFile.exists()){
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found,Const.LEDTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.LEDTEST_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String ledName = null;
            String ledPath = null;
            String ledOnName = null;
            String ledOnValue = null;
            String ledOffName = null;
            String ledOffValue = null;
            String ledGroup = null;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(TAG_LINE.equals(startTagName)) {
                            /*testLineLayout = new LinearLayout(getApplicationContext());
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            layoutParams.gravity = Gravity.CENTER;
                            testLineLayout.setOrientation(LinearLayout.HORIZONTAL);
                            testLineLayout.setLayoutParams(layoutParams);
                            testLineLayout.setGravity(Gravity.CENTER);*/
                        }else if(TAG_LED_NAME.equals(startTagName)){
                            //add by zhaohairuo for bug 20746 @2018-11-15 start
                            ledName = xmlPullParser.nextText();
							//add by zhaohairuo for bug 20746 @2018-11-15 end
                        }else if(TAG_LED_PATH.equals(startTagName)){
                            ledPath = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_NAME.equals(startTagName)){
                            ledOnName = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_VALUE.equals(startTagName)){
                            ledOnValue = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_NAME.equals(startTagName)){
                            ledOffName = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_VALUE.equals(startTagName)){
                            ledOffValue = xmlPullParser.nextText();
                        }else if(TAG_LED_GROUP.equals(startTagName)) {
                            ledGroup = xmlPullParser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if(TAG_CONFIG.equals(endTagName)){
                            break;
                        }
                        if(TAG_LINE.equals(endTagName)){
                            if(mRootView!=null){
                                ledName = null;
                                ledPath = null;
                                ledOnName = null;
                                ledOnValue = null;
                                ledOffName = null;
                                ledOffValue = null;
                                ledGroup = null;
                            }
                        }else if(TAG_LED_TEST.equals(endTagName)){
                            TestButton testOnButton = null;
                            TestButton testOffButton = null;
                            if(ledOnName!=null && ledOnValue!=null && ledPath!=null){
                                /*Button testButton = new Button(getApplicationContext());
                                testButton.setText(ledOnName);
                                testButton.setTextColor(Color.rgb(59,64,60));
                                testButton.setTextSize(FONT_SIZE);
                                testButton.setTag(ledOnName);
                                testButton.setFocusableInTouchMode(false);
                                testButton.setBackgroundResource(R.drawable.keytest_bg_normal);
                                testButton.setEnabled(true);
                                testButton.setVisibility(View.INVISIBLE);
                                testButton.setOnClickListener(this);
                                if(ledOnWidth!=null){
                                    testButton.setWidth(Integer.parseInt(ledOnWidth));
                                }
                                if(ledOnHeight!=null){
                                    testButton.setHeight(Integer.parseInt(ledOnHeight));
                                }*/
                                //if(testLineLayout!=null) {
                                    //testLineLayout.addView(testButton);
                                    //testOnButton = new TestButton(ledPath, ledOnValue, null);
                                testOnButton = new TestButton(ledPath, ledOnValue);
                                //}
                            }

                            if(ledOffName!=null && ledOffValue!=null && ledPath!=null){
                                /*Button testButton = new Button(getApplicationContext());
                                testButton.setText(ledOffName);
                                testButton.setTextColor(Color.rgb(59,64,60));
                                testButton.setTextSize(FONT_SIZE);
                                testButton.setTag(ledOffName);
                                testButton.setFocusableInTouchMode(false);
                                testButton.setBackgroundResource(R.drawable.keytest_bg_normal);
                                testButton.setEnabled(true);
                                testButton.setVisibility(View.INVISIBLE);
                                testButton.setOnClickListener(this);
                                if(ledOffWidth!=null){
                                    testButton.setWidth(Integer.parseInt(ledOffWidth));
                                }
                                if(ledOffHeight!=null){
                                    testButton.setHeight(Integer.parseInt(ledOffHeight));
                                }*/
                                //if(testLineLayout!=null) {
                                //    testLineLayout.addView(testButton);
                                   // testOffButton = new TestButton(ledPath, ledOffValue, null);
                                testOffButton = new TestButton(ledPath, ledOffValue);
                                //}
                            }
                            LogUtil.w("xcode","===>"+ledName+" group"+ledGroup);
                            if(ledGroup != null && ledGroup.equals("0")) {
                                LogUtil.w("xcode", "group 0");
                                if (mTestLedMap0 != null) {
                                    mTestLedMap0.put(ledName, new TestLed(ledName, testOnButton, testOffButton));
                                }
                            } else if (ledGroup != null && ledGroup.equals("1")) {
                                LogUtil.w("xcode", "group 1");
                                if (mTestLedMap1 != null) {
                                    mTestLedMap1.put(ledName, new TestLed(ledName, testOnButton, testOffButton));
                                }
                            }
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            setTestFailReason(String.format(getResources().getString(R.string.fail_reason_read_xml), Const.LEDTEST_CONFIG_XML_PATH));
            Log.d(TAG, getTestFailReason());
            e.printStackTrace();
        }

   }

    TestButton getTestLedOnButton( String ledname ){
        if( ( ledname == null ) || !ledname.isEmpty() ) {
            LogUtil.d(TAG, "getTestLedOnButton ledname is null or empty!");
            return null;
        }
        Iterator<Map.Entry<String, TestLed>> iterator0 = mTestLedMap0.entrySet().iterator();
        while (iterator0.hasNext()) {
            Map.Entry<String, TestLed> entry = iterator0.next();
            if(ledname.equals(entry.getValue().getTestLedName())) {
                return entry.getValue().getTestButtonOn();
            }
        }
        Iterator<Map.Entry<String, TestLed>> iterator1 = mTestLedMap1.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry<String, TestLed> entry = iterator1.next();
            if( ledname.equals(entry.getValue().getTestLedName()) ) {
                return entry.getValue().getTestButtonOn();
            }
        }
        return null;
    }

    TestButton getTestLedOffButton( String ledname ){
        if( ( ledname == null ) || !ledname.isEmpty() ) {
            LogUtil.d(TAG, "getTestLedOffButton ledname is null or empty!");
            return null;
        }
        Iterator<Map.Entry<String, TestLed>> iterator0 = mTestLedMap0.entrySet().iterator();
        while (iterator0.hasNext()) {
            Map.Entry<String, TestLed> entry = iterator0.next();
            if(ledname.equals(entry.getValue().getTestLedName())) {
                return entry.getValue().getTestButtonOff();
            }
        }
        Iterator<Map.Entry<String, TestLed>> iterator1 = mTestLedMap1.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry<String, TestLed> entry = iterator1.next();
            if( ledname.equals(entry.getValue().getTestLedName()) )
                return entry.getValue().getTestButtonOff();
        }
        return null;
    }

    private void doLedDisplay(TestButton mLedInfo){
        if(mLedInfo == null){
            LogUtil.d(TAG, "mLedInfo is null");
            return;
        }
        writeToFile(mLedInfo.getTestLedPath(), mLedInfo.getTestLedValues());
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    if (mConfigResult>=1){
                        deInit(mFatherName, SUCCESS);
                    }else {
                        setTestFailReason(getResources().getString(R.string.fail_reason_led_fail));
                        deInit(mFatherName, FAILURE,getTestFailReason());
                    }
                    break;
                case 1002:
                    TestButton mCurrentBtn = getTestLedOnButton(mLedName);
                    doLedDisplay(mCurrentBtn);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        writeToFile("/sys/class/leds/cit_mode/enable","0");
        TestButton mCurrentBtn = getTestLedOffButton(mLedName);
        doLedDisplay(mCurrentBtn);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
			//led_auto_show_flag = true;
			//clearAllTestLed();
			if(mDelayHandler != null){
	            mDelayHandler.removeCallbacks(mDelayRunnable);
	            mDelayHandler = null;
	        }
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
			//led_auto_show_flag = true;
			//clearAllTestLed();
			if(mDelayHandler != null){
	            mDelayHandler.removeCallbacks(mDelayRunnable);
	            mDelayHandler = null;
	        }
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }/*else {
            TestButton bt = getTestLedButton(v);
            if( bt != null )
                writeToFile(bt.getTestLedPath(), bt.getTestLedValues());
        }*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(mFatherName, result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(mFatherName, result);
        }
    }


    /**
     * led button object
     */
    class  TestButton extends Object{
        //protected Button _button = null;
        protected boolean _tested = false;
        protected String _path = null;
        protected String _value = null;

        TestButton(String path,String val/*,Button button*/){
            _path = path;
            _value = val;
            //_button = button;
            //_button.setSoundEffectsEnabled(false);
        }

        /*public Button getTestButton() {
            return _button;
        }*/

        public String getTestLedPath() {
            return _path;
        }

        public String getTestLedValues() {
            return _value;
        }

        public boolean  isTested() {
            return _tested;
        }

        public void setTested( boolean value ) {
            _tested = value;
        }
    }

    class TestLed extends  Object {
        protected TestButton _button_on = null;
        protected TestButton _button_off = null;
        protected String _name = null;
        protected Boolean _result = false;

        TestLed(String name,TestButton buttonOn,TestButton buttonOff){
            _name = name;
            _button_on = buttonOn;
            _button_off = buttonOff;
        }

        public TestButton getTestButtonOn() {
            return _button_on;
        }

        public TestButton getTestButtonOff() {
            return _button_off;
        }

        public String getTestLedName() {
            return _name;
        }

        public void setResult(Boolean result){
            _result = result;
        }

        public Boolean getResult(){
            return _result;
        }
    }

}
