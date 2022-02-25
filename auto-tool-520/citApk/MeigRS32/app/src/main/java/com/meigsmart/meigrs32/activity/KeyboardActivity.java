package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.SystemProperties;
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

import butterknife.BindView;

public class KeyboardActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private KeyboardActivity mContext;
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
    private Map<String, TestKey> mScancodeKeyMap = new HashMap<String, TestKey>();
    private String TAG = "KeyBoardTest";
    private String TAG_CONFIG = "keyboard_config";
    private String TAG_LINE = "test_line";
    private String TAG_KEY_TEST = "key_test";
    private String TAG_KEY_NAME = "keyName";
    private String TAG_KEY_CODE = "keyCode";
    private String TAG_SCAN_CODE = "scanCode";
    private String TAG_WIDTH = "width";
    private String TAG_HEIGHT = "height";
    private String TAG_MC510 = "common_device_name_test";
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    private boolean is_MC510 = false;
    private boolean is_mt535 = false;
    private final float FONT_SIZE = 12;
    private int mConfigTime = 0;



    @Override
    protected int getLayoutId() {
        return R.layout.activity_keyboard;
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
        //add for MC510
        is_MC510 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_MC510).equals("MC510");
        //add for mt535
        is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
        if(is_mt535){
            if (!MyApplication.getInstance().isConnectPaySDK()) {
                MyApplication.getInstance().bindPaySDKService();
                ToastUtil.showCenterShort(getString(R.string.connect_loading));
                return;
            }
        }
        initLayout();
        refreshTestResult();

    }


    /**
     * 根据XML动态创建按键测试布局
     */
    private void initLayout() {
        File configFile = new File(Const.KEYTEST_CONFIG_XML_PATH);
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.KEYTEST_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.KEYTEST_CONFIG_XML_PATH);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");

            String keyName = null;
            String keyCode = null;
            String scanCode = null;
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
                        } else if (TAG_SCAN_CODE.equals(startTagName)) {
                            scanCode = xmlPullParser.nextText();
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
                            if (keyName != null && (keyCode != null || scanCode != null)) {
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
                                        if (keyCode != null) {
                                            mTestKeyMap.put(keyCode, new TestKey(keyName, keyCode, scanCode, testButton));
                                        } else {
                                            mScancodeKeyMap.put(scanCode, new TestKey(keyName, keyCode, scanCode, testButton));
                                        }
                                    }
                                    testLineLayout.addView(testButton);
                                }
                            }
                            keyName = null;
                            keyCode = null;
                            scanCode = null;
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



    /**
     * 刷新按键按下的状态，如果都按下则认为测试成功，直接pass退出
     */
    private void refreshTestResult() {
        Iterator<Map.Entry<String, TestKey>> iterator = mTestKeyMap.entrySet().iterator();
        Iterator<Map.Entry<String, TestKey>> iterator2 = mScancodeKeyMap.entrySet().iterator();
        boolean allpass = true;
        while (true) {
            Map.Entry<String, TestKey> entry = null;
            if (iterator.hasNext()) {
                entry = iterator.next();
            } else if (iterator2.hasNext()) {
                entry = iterator2.next();
            } else {
                break;
            }
            if (entry != null && !entry.getValue().isPressed()) {
                    allpass = false;
            }
            Log.w("xcode", "====>getKey=" + entry.getKey() + ",button text is " + entry.getValue().getKeyName() + ",and is pressed =" + entry.getValue().isPressed());
        }
        if (mTestKeyMap.size() == 0 && mScancodeKeyMap.size() == 0) {
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
    // add by maohaojie on 2019.01.25 for bug 23539 start
    private long prelongTim = 0;
    private long curTime = 0;
    private int keyCode1 = 0;
    private int keyCode2 = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            Log.d("MM0501","event:"+event);
            if(is_MC510){
				if(event.getScanCode()==111){
                    keyCode=111;
                }
                if(event.getScanCode()==190){
                    keyCode=190;
                }
                if(event.getScanCode()==191){
                    keyCode=191;
                }
                if(event.getScanCode()==192){
                    keyCode=192;
                }
                if(event.getScanCode()==193){
                    keyCode=193;
                }
            }
            TestKey key = mTestKeyMap.get(keyCode + "");
            if (key == null)
                key = mScancodeKeyMap.get(event.getScanCode() + "");
            Button pressBtn = key.getTestButton();
            if (pressBtn != null) {
                pressBtn.setTextColor(Color.WHITE);
                pressBtn.setBackgroundResource(R.drawable.keytest_bg_pressed);
                key.setPressed(true);
            }
            if (prelongTim == 0) {
                prelongTim = (new Date()).getTime();
                keyCode1 = keyCode;
            } else {
                curTime = (new Date()).getTime();
                long mm = curTime - prelongTim;
                prelongTim = 0;
                keyCode2 = keyCode;
                if ((mm < 50 )&& (keyCode1 != keyCode2)&&(!is_MC510)) {
                    mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    return true;
                }
            }
            // add by maohaojie on 2019.01.25 for bug 23539 end
            refreshTestResult();
        } catch (Exception e) {
            Log.e(TAG, "====>find unknow key,keycode = " + keyCode + ",please add in key test!");
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
        protected String _scanCode = null;

        TestKey(String keyName, String keyCode, String scanCode, Button button) {
            _keyName = keyName;
            _keyCode = keyCode;
            _scanCode = scanCode;
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

        public String getScanCode() {
            return _scanCode;
        }

        public Button getTestButton() {
            return _button;
        }
    }


}
