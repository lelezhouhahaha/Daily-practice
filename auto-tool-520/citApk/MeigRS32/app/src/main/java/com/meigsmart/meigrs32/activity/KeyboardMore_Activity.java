package com.meigsmart.meigrs32.activity;

import android.content.ContentResolver;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.SystemProperties;
import android.provider.Settings;
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
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import butterknife.BindView;

public class KeyboardMore_Activity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private KeyboardMore_Activity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    private String mFatherName = "";
    @BindView(R.id.key_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private Map<String, TestKey> mTestKeyMap = new HashMap<String, TestKey>();
    private String TAG = "KeyBoardTest";
    private String TAG_CONFIG = "keyboard_config";
    private String TAG_LINE = "test_line";
    private String TAG_KEY_TEST = "key_test";
    private String TAG_KEY_NAME = "keyName";
    private String TAG_KEY_CODE = "keyCode";
    private String TAG_WIDTH = "width";
    private String TAG_HEIGHT = "height";
    private final float FONT_SIZE = 12;
    private int mConfigTime = 0;
    private long prelongTim = 0;
    private long curTime = 0;
    private int keyCode1 = 0;
    private int keyCode2 = 0;
    private String KEYPAD_NODE = "/sys/bus/i2c/devices/8-005b/device_id";
    private String COMMON_KEY_ACCESSIBILITY = "common_key_accessibility";
    private boolean needKeyAccessibility;
    private ContentResolver contentResolver;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_keyboard_more;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.pcba_keyboard);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        contentResolver = mContext.getContentResolver();
        needKeyAccessibility = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, COMMON_KEY_ACCESSIBILITY).contains("true");
        if(needKeyAccessibility){
            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
        }

        Intent Intent_KeyTest = new Intent("com.cipherlab.keymappingmanager.KEY_TEST_MODE");
        Intent_KeyTest.putExtra("KeyTest_Data", true);
        sendBroadcast(Intent_KeyTest);

        initLayout();
        refreshTestResult();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SEND_RECENT_BUTTON");
        registerReceiver(RecentApp_Broadcast, filter);
    }


    /**
     * 根据XML动态创建按键测试布局
     */
    private void initLayout() {
        String keypadNode = DataUtil.readLineFromFile(KEYPAD_NODE);
        int keypadMode = 38;
        if(!keypadNode.isEmpty()) {
            String [] spString = keypadNode.split("\\s+");
            LogUtil.d("citapk spString[0]:" + spString[0]);
            LogUtil.d("citapk spString[1]:" + spString[1]);
            LogUtil.d("citapk spString[2]:" + spString[2]);
            keypadMode = Integer.parseInt(spString[1]);
        }
        String keypadConfigPath = "";
        if(keypadMode == 38)
            keypadConfigPath = Const.KEYTEST_CONFIG_XML_PATH;
        else if(keypadMode == 52)
            keypadConfigPath = Const.KEYTEST_DEPUTY_CONFIG_XML_PATH;

        LogUtil.d("citapk keypadConfigPath:" + keypadConfigPath);
        File configFile = new File(keypadConfigPath);
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.KEYTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(keypadConfigPath);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String keyName = null;
            String keyCode = null;
            String width = null;
            String height = null;
            LinearLayout testLineLayout = null;

            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {

                switch (type) {
                    case XmlPullParser.START_TAG:
                        String startTagName = xmlPullParser.getName();
                        if (TAG_LINE.equals(startTagName)) {
                            testLineLayout = new LinearLayout(getApplicationContext());
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            layoutParams.gravity = Gravity.CENTER;
                            testLineLayout.setOrientation(LinearLayout.HORIZONTAL);
                            testLineLayout.setLayoutParams(layoutParams);
                            testLineLayout.setGravity(Gravity.CENTER);
                        } else if (TAG_KEY_NAME.equals(startTagName)) {
                            String str = xmlPullParser.nextText();
                            int resId = mContext.getResources().getIdentifier(str, "string", getPackageName());
                            if (resId == 0) {
                                keyName = str;
                            } else keyName = mContext.getResources().getString(resId);
                        } else if (TAG_KEY_CODE.equals(startTagName)) {
                            keyCode = xmlPullParser.nextText();
                        } else if (TAG_WIDTH.equals(startTagName)) {
                            width = xmlPullParser.nextText();
                        } else if (TAG_HEIGHT.equals(startTagName)) {
                            height = xmlPullParser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTagName = xmlPullParser.getName();
                        if (TAG_CONFIG.equals(endTagName)) {
                            break;
                        }
                        if (TAG_LINE.equals(endTagName)) {
                            if (mRootView != null) {
                                try {
                                    mRootView.addView(testLineLayout);
                                } catch (Exception e) {

                                }
                                testLineLayout = null;
                            }
                        } else if (TAG_KEY_TEST.equals(endTagName)) {
                            if (keyName != null && keyCode != null) {
                                Button testButton = new Button(getApplicationContext());
                                testButton.setText(keyName);
                                testButton.setTextColor(Color.rgb(59, 64, 60));
                                testButton.setTextSize(FONT_SIZE);
                                testButton.setTag(keyCode);
                                testButton.setFocusableInTouchMode(false);
                                testButton.setBackgroundResource(R.drawable.keytest_bg_normal);
                                testButton.setEnabled(false);
                                if (width != null) {
                                    testButton.setWidth(Integer.parseInt(width));
                                }
                                if (height != null) {
                                    testButton.setHeight(Integer.parseInt(height));
                                }
                                if (testLineLayout != null) {
                                    if (mTestKeyMap != null) {
                                        mTestKeyMap.put(keyCode, new TestKey(keyName, keyCode, testButton));
                                    }
                                    testLineLayout.addView(testButton);
                                }
                            }
                            keyName = null;
                            keyCode = null;
                            width = null;
                            height = null;
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "====>error = " + e.toString());
            e.printStackTrace();
        }
    }
    BroadcastReceiver RecentApp_Broadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SEND_RECENT_BUTTON")) {
                    TestKey key = mTestKeyMap.get(95 + "");
                    Button pressBtn = key.getTestButton();
                    if (pressBtn != null) {
                        pressBtn.setTextColor(Color.WHITE);
                        pressBtn.setBackgroundResource(R.drawable.keytest_bg_pressed);
                        key.setPressed(true);
                        refreshTestResult();
                    }
            }
        }
    };

    /**
     * 刷新按键按下的状态，如果都按下则认为测试成功，直接pass退出
     */
    private void refreshTestResult() {
        Iterator<Map.Entry<String, TestKey>> iterator = mTestKeyMap.entrySet().iterator();
        boolean allpass = true;
        while (iterator.hasNext()) {
            Map.Entry<String, TestKey> entry = iterator.next();
            if (!entry.getValue().isPressed()) {
                allpass = false;
            }
            Log.w("xcode", "====>getKey=" + entry.getKey() + ",button text is " + entry.getValue().getKeyName() + ",and is pressed =" + entry.getValue().isPressed());
        }
        if (mTestKeyMap.size() == 0) {
            allpass = false;
        }
        if (allpass) {
            Log.w("xcode", "====>test keyboard pass");
            deInit(mFatherName, SUCCESS);
        }
    }

    /**
     * 监听按键事件，刷新图标和状态。
     *
     * @param keyCode
     * @param event
     * @return
     */
    private int keyName = 0;
    private int keyCode = 0;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //add by wangxing@20191231 for task3516 start
        if(event.getKeyCode() == KeyEvent.KEYCODE_POWER && event.getAction() != KeyEvent.ACTION_UP){
            return true;
        }
        //add by wangxing@20191231 for task3516 end
        try {
            Log.d("Meig","event:"+event);
            keyName = event.getKeyCode();
            Log.d("Meig","keyName:"+keyName);
            TestKey key = mTestKeyMap.get(keyName + "");
            if(event.getScanCode()==249){
                keyCode=267;
                 key = mTestKeyMap.get(keyCode + "");
            }else if(event.getScanCode()==250){
                keyCode=265;
                key = mTestKeyMap.get(keyCode + "");
            }
            Button pressBtn = key.getTestButton();
            if (pressBtn != null) {
                pressBtn.setTextColor(Color.WHITE);
                pressBtn.setBackgroundResource(R.drawable.keytest_bg_pressed);
                key.setPressed(true);
            }
           if (event.getAction() == KeyEvent.ACTION_DOWN){
               if (prelongTim == 0) {
                   prelongTim = (new Date()).getTime();
                   Log.d("MMH_01",prelongTim+"");
                   keyCode1 = event.getKeyCode();
               } else {
                   curTime = (new Date()).getTime();
                   Log.d("MMH_02",curTime+"");
                   long mm = curTime - prelongTim;
                   Log.d("MMH_03",mm+"");
                   prelongTim = curTime;
                   keyCode2 = event.getKeyCode();
                   if ((mm < 150 )&& (keyCode1 != keyCode2)) {
                       keyCode1 = keyCode2;
                       mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                       deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                       return true;
                   }
                   keyCode1 = keyCode2;
               }
           }
            refreshTestResult();
        } catch (Exception e) {
            Log.e(TAG, "====>find unknow key,keycode = " + keyName + ",please add in key test!");
        }
        return true;

    }

    @Override
    public void onClick(View v) {
        if (v == mSuccess) {
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail) {
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);

        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }


    /**
     * 按键对象
     */
    class TestKey extends Object {
        protected Button _button = null;
        protected boolean _pressed = false;
        protected String _keyName = null;
        protected String _keyCode = null;

        TestKey(String keyName, String keyCode, Button button) {
            _keyName = keyName;
            _keyCode = keyCode;
            _button = button;
        }

        public boolean isPressed() {
            return _pressed;
        }

        public void setPressed(boolean press) {
            _pressed = press;
        }

        public String getKeyName() {
            return _keyName;
        }

        public String getKeyCode() {
            return _keyCode;
        }

        public Button getTestButton() {
            return _button;
        }
    }

    @Override
    protected void onDestroy() {
        Intent Intent_KeyTest = new Intent("com.cipherlab.keymappingmanager.KEY_TEST_MODE");
        Intent_KeyTest.putExtra("KeyTest_Data", false);
        sendBroadcast(Intent_KeyTest);

        if(needKeyAccessibility){
            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "com.customzied.keymapping/com.customzied.keymapping.KeyAccessibilityService" /* on */);
        }
        if(null != RecentApp_Broadcast){
            unregisterReceiver(RecentApp_Broadcast);
        }

        super.onDestroy();
    }
}
