package com.meigsmart.meigrs32.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Log;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.BindView;

public class SixPinWaferActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SixPinWaferActivity mContext;
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
    private String TAG = "SixPinWaferActivity";
    private String TAG_CONFIG = "keyboard_config";
    private String TAG_LINE = "test_line";
    private String TAG_KEY_TEST = "key_test";
    private String TAG_KEY_NAME = "keyName";
    private String TAG_KEY_CODE = "keyCode";
    private String TAG_WIDTH = "width";
    private String TAG_HEIGHT = "height";
    private final float FONT_SIZE = 12;
    private int mConfigTime = 0;
    boolean ledstate = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_six_pin_wafer;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mTitle.setText(R.string.run_in_sixpintest);
        mSuccess.setOnClickListener(this);
        mSuccess.setVisibility(View.GONE);
        mFail.setOnClickListener(this);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        writeToFile("/sys/class/leds/gpio0-led/brightness","255");
        writeToFile("/sys/class/leds/gpio1-led/brightness","255");

        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        initLayout();
        showDialog();
     //   refreshTestResult();
    }

    public void writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            LogUtil.d("MM1105","path:"+path);
            fRed.write(value.getBytes());
            LogUtil.d("MM1105","value.getBytes():"+value.getBytes());
            fRed.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("MM1105","e:"+e.toString());

        }
    }

    @Override
    protected void onDestroy() {
        writeToFile("/sys/class/leds/gpio0-led/brightness","0");
        writeToFile("/sys/class/leds/gpio1-led/brightness","0");
        super.onDestroy();
    }
    /**
     * 根据XML动态创建按键测试布局
     */
    private void initLayout() {
        File configFile = new File(Const.SIXPIN_CONFIG_XML_PATH);
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.SIXPIN_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.SIXPIN_CONFIG_XML_PATH);
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
            LogUtil.d("====>error = " + e.toString());
            e.printStackTrace();
        }
    }



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
            LogUtil.w("xcode", "====>getKey=" + entry.getKey() + ",button text is " + entry.getValue().getKeyName() + ",and is pressed =" + entry.getValue().isPressed());
        }
      if (mTestKeyMap.size() == 0) {
           allpass = false;
        }
       if (allpass) {
           LogUtil.w("xcode", "====>test keyboard pass");
         //  deInit(mFatherName, SUCCESS);
           if (ledstate ){
               mSuccess.setVisibility(View.VISIBLE);
           }
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
            LogUtil.d("ooooo"+keyCode);
            int mkeycode = event.getScanCode();
            TestKey key = mTestKeyMap.get(mkeycode + "");
            Button pressBtn = key.getTestButton();
            if (pressBtn != null) {
                pressBtn.setTextColor(Color.WHITE);
                pressBtn.setBackgroundResource(R.drawable.keytest_bg_pressed);
                key.setPressed(true);
            }

            // add by maohaojie on 2019.01.25 for bug 23539 end
           refreshTestResult();
        } catch (Exception e) {
            LogUtil.e("====>find unknow key,keycode = " + keyCode + ",please add in key test!");
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
    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(SixPinWaferActivity.this);
        dialog.setMessage(R.string.power_led_on);
        dialog.setPositiveButton(R.string.str_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ledstate =true;

                    }
                });
        dialog.setNegativeButton(R.string.str_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ledstate = false;

                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }

}