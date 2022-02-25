package com.meigsmart.meigrs32.activity;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Xml;
import android.view.Gravity;
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


public class BreathLightActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private BreathLightActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.led_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.content_show)
    public TextView textView;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private Runnable mRun;
    private int mConfigTime = 0;
    private int mConfigResult = 0;
    private float FONT_SIZE = 12;
    private boolean led_auto_show_flag = false;
    private int DELAY_TIME = 500;
    private Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator0;
    private Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator1;
    private Map<String, BreathLightActivity.TestLed> mTestLedMap0 = new TreeMap<String, BreathLightActivity.TestLed>();
    private Map<String, BreathLightActivity.TestLed> mTestLedMap1 = new TreeMap<String, BreathLightActivity.TestLed>();
    private String TAG =  "LedTest";
    private String TAG_CONFIG = "led_config";
    private String TAG_LINE = "breath_test_line";
    private String TAG_LED_TEST = "breath_led_test";
    private String TAG_LED_NAME = "breathledName";
    private String TAG_LED_PATH = "breathledPath";
    private String TAG_LED_ON_NAME = "breathledOnName";
    private String TAG_LED_ON_VALUE = "breathledOnValue";
    private String TAG_LED_ON_WIDTH = "breathledOnWidth";
    private String TAG_LED_ON_HEIGHT = "breathledOnHeight";
    private String TAG_LED_OFF_NAME = "breathledOffName";
    private String TAG_LED_OFF_VALUE = "breathledOffValue";
    private String TAG_LED_OFF_WIDTH = "breathledOffWidth";
    private String TAG_LED_OFF_HEIGHT = "breathledOffHeight";
    private String TAG_LED_GROUP = "breathledGroup";
    @Override
    protected int getLayoutId() {
        return R.layout.activity_demo;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_breathlight);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mConfigResult = getResources().getInteger(R.integer.breath_leds_default_config_standard_result);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);
        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext,mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
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
        LogUtil.d(TAG, "path: " + path + "value: " + value);
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + "abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
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
                        deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1002:
                    if (iterator0.hasNext() || iterator1.hasNext()) {
                        textView.setText("");
                        if(iterator0.hasNext()) {
                            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator0.next();
                            LogUtil.w("xcode", "====>group 0: led text is " + entry.getValue().getTestLedName() + ",and is tested =" + entry.getValue().getTestButtonOn().isTested());
                            LogUtil.w("xcode", "====>group 0: led text led_auto_show_flag is " + led_auto_show_flag);
                            textView.setText(entry.getValue().getTestLedName());
                            if (!led_auto_show_flag) {
                                entry.getValue().getTestButtonOn().getTestButton().performClick();
                            } else break;
                        }
                        if(iterator1.hasNext()) {
                            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator1.next();
                            LogUtil.w("xcode", "====>group 1: led text is " + entry.getValue().getTestLedName() + ",and is tested =" + entry.getValue().getTestButtonOn().isTested());
                            LogUtil.w("xcode", "====>group 1: led text led_auto_show_flag is " + led_auto_show_flag);
                            textView.setText(textView.getText() + ",  "+entry.getValue().getTestLedName());
                            if (!led_auto_show_flag) {
                                entry.getValue().getTestButtonOn().getTestButton().performClick();
                            } else break;
                        }
                        if(!led_auto_show_flag) {
                            try {
                                Thread.currentThread().sleep(DELAY_TIME);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else break;
                        mHandler.sendEmptyMessageDelayed(1003,DELAY_TIME*6);
                    }else{
                        mSuccess.setVisibility(View.VISIBLE);//modify by wangjingfeng for bug 17541;REASON:Set success as visible when all led tests are completed
                        mHandler.sendEmptyMessageDelayed(1002,DELAY_TIME*6);
                        iterator0 = mTestLedMap0.entrySet().iterator();
                        iterator1 = mTestLedMap1.entrySet().iterator();
                        //mFail.setVisibility(View.VISIBLE);
                        //mSuccess.setVisibility(View.VISIBLE);
                        LogUtil.w("xcode", "====>led text is end. and restart.");
                    }
                    break;
                case 1003:
                    clearAllTestLed();
                    mHandler.sendEmptyMessage(1002);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

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
            String ledOnWidth = null;
            String ledOnHeight = null;
            String ledOffName = null;
            String ledOffValue = null;
            String ledOffWidth = null;
            String ledOffHeight = null;
            String ledGroup = null;
            LinearLayout testLineLayout = null;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if(TAG_LINE.equals(startTagName)) {
                            testLineLayout = new LinearLayout(getApplicationContext());
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            layoutParams.gravity = Gravity.CENTER;
                            testLineLayout.setOrientation(LinearLayout.HORIZONTAL);
                            testLineLayout.setLayoutParams(layoutParams);
                            testLineLayout.setGravity(Gravity.CENTER);
                        }else if(TAG_LED_NAME.equals(startTagName)){
                            //add by zhaohairuo for bug 20746 @2018-11-15 start
                            String str = xmlPullParser.nextText();
                            int resId = mContext.getResources().getIdentifier(str, "string", getPackageName() );
                            if(resId==0){
                                ledName=str;
                            }else ledName = mContext.getResources().getString(resId);
                            //add by zhaohairuo for bug 20746 @2018-11-15 end
                        }else if(TAG_LED_PATH.equals(startTagName)){
                            ledPath = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_NAME.equals(startTagName)){
                            ledOnName = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_VALUE.equals(startTagName)){
                            ledOnValue = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_WIDTH.equals(startTagName)){
                            ledOnWidth = xmlPullParser.nextText();
                        }else if(TAG_LED_ON_HEIGHT.equals(startTagName)){
                            ledOnHeight = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_NAME.equals(startTagName)){
                            ledOffName = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_VALUE.equals(startTagName)){
                            ledOffValue = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_WIDTH.equals(startTagName)){
                            ledOffWidth = xmlPullParser.nextText();
                        }else if(TAG_LED_OFF_HEIGHT.equals(startTagName)){
                            ledOffHeight = xmlPullParser.nextText();
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
                                try {
                                    mRootView.addView(testLineLayout);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                ledName = null;
                                ledPath = null;
                                ledOnName = null;
                                ledOnValue = null;
                                ledOnWidth = null;
                                ledOnHeight = null;
                                ledOffName = null;
                                ledOffValue = null;
                                ledOffWidth = null;
                                ledOffHeight = null;
                                testLineLayout = null;
                                ledGroup = null;
                            }
                        }else if(TAG_LED_TEST.equals(endTagName)){
                            BreathLightActivity.TestButton testOnButton = null;
                            BreathLightActivity.TestButton testOffButton = null;
                            if(ledOnName!=null && ledOnValue!=null && ledPath!=null){
                                Button testButton = new Button(getApplicationContext());
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
                                }
                                if(testLineLayout!=null) {
                                    testLineLayout.addView(testButton);
                                    testOnButton = new BreathLightActivity.TestButton(ledPath, ledOnValue, testButton);
                                }
                            }

                            if(ledOffName!=null && ledOffValue!=null && ledPath!=null){
                                Button testButton = new Button(getApplicationContext());
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
                                }
                                if(testLineLayout!=null) {
                                    testLineLayout.addView(testButton);
                                    testOffButton = new BreathLightActivity.TestButton(ledPath, ledOffValue, testButton);
                                }
                            }
                            LogUtil.w("xcode","===>"+ledName+" group"+ledGroup);
                            if(ledGroup != null && ledGroup.equals("0")) {
                                LogUtil.w("xcode", "group 0");
                                if (mTestLedMap0 != null) {
                                    mTestLedMap0.put(ledName, new BreathLightActivity.TestLed(ledName, testOnButton, testOffButton));
                                }
                            } else if (ledGroup != null && ledGroup.equals("1")) {
                                LogUtil.w("xcode", "group 1");
                                if (mTestLedMap1 != null) {
                                    mTestLedMap1.put(ledName, new BreathLightActivity.TestLed(ledName, testOnButton, testOffButton));
                                }
                            }
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    BreathLightActivity.TestButton getTestLedButton(View v ){
        Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator0 = mTestLedMap0.entrySet().iterator();
        while (iterator0.hasNext()) {
            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator0.next();
            if(v ==  entry.getValue().getTestButtonOn().getTestButton())
                return entry.getValue().getTestButtonOn();
            else if(v ==  entry.getValue().getTestButtonOff().getTestButton())
                return entry.getValue().getTestButtonOff();
        }
        Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator1 = mTestLedMap1.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator1.next();
            if(v ==  entry.getValue().getTestButtonOn().getTestButton())
                return entry.getValue().getTestButtonOn();
            else if(v ==  entry.getValue().getTestButtonOff().getTestButton())
                return entry.getValue().getTestButtonOff();
        }
        return null;
    }

    void clearAllTestLed(){
        Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator0 = mTestLedMap0.entrySet().iterator();
        while (iterator0.hasNext()) {
            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator0.next();
            LogUtil.w("xcode", "====>led text is " + entry.getValue().getTestLedName() + ",and is tested =" + entry.getValue().getTestButtonOff().isTested());
            entry.getValue().getTestButtonOff().getTestButton().performClick();
        }
        Iterator<Map.Entry<String, BreathLightActivity.TestLed>> iterator1 = mTestLedMap1.entrySet().iterator();
        while (iterator1.hasNext()) {
            Map.Entry<String, BreathLightActivity.TestLed> entry = iterator1.next();
            LogUtil.w("xcode", "====>led text is " + entry.getValue().getTestLedName() + ",and is tested =" + entry.getValue().getTestButtonOff().isTested());
            entry.getValue().getTestButtonOff().getTestButton().performClick();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearAllTestLed();
        mHandler.removeCallbacks(mRun);
        //mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }else {
            BreathLightActivity.TestButton bt = getTestLedButton(v);
            if( bt != null )
                writeToFile(bt.getTestLedPath(), bt.getTestLedValues());
        }
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
        protected Button _button = null;
        protected boolean _tested = false;
        protected String _path = null;
        protected String _value = null;

        TestButton(String path,String val,Button button){
            _path = path;
            _value = val;
            _button = button;
            _button.setSoundEffectsEnabled(false);
        }

        public Button getTestButton() {
            return _button;
        }

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
        protected BreathLightActivity.TestButton _button_on = null;
        protected BreathLightActivity.TestButton _button_off = null;
        protected String _name = null;

        TestLed(String name, BreathLightActivity.TestButton buttonOn, BreathLightActivity.TestButton buttonOff){
            _name = name;
            _button_on = buttonOn;
            _button_off = buttonOff;
        }

        public BreathLightActivity.TestButton getTestButtonOn() {
            return _button_on;
        }

        public BreathLightActivity.TestButton getTestButtonOff() {
            return _button_off;
        }

        public String getTestLedName() {
            return _name;
        }
    }




}
