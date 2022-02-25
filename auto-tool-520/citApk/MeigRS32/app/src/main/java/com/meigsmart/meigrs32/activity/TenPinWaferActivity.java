package com.meigsmart.meigrs32.activity;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.input.InputManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Log;
import android.text.format.Formatter;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

public class TenPinWaferActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private TenPinWaferActivity mContext;
    @BindView(R.id.key_root_view)
    public LinearLayout mRootView;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    private Map<String, TenPinWaferActivity.TestKey> mTestKeyMap = new HashMap<String, TenPinWaferActivity.TestKey>();
    private String TAG = "TenPinWaferActivity";
    private String TAG_CONFIG = "keyboard_config";
    private String TAG_LINE = "test_line";
    private String TAG_KEY_TEST = "key_test";
    private String TAG_KEY_NAME = "keyName";
    private String TAG_KEY_CODE = "keyCode";
    private String TAG_WIDTH = "width";
    private String TAG_HEIGHT = "height";
    private final float FONT_SIZE = 12;
    private USBFinder mUsbFinder;
    TextView textone;
    TextView texttwo;
    TextView textthree;
    boolean ledstate = false;
    boolean usbotg = false;
    boolean keyborad = false;



    @Override
    protected int getLayoutId() {
        return R.layout.activity_ten_pin_wafer;
    }
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.run_in_tenpintest);

        textone = (TextView) findViewById(R.id.usbii_msg);
        texttwo = (TextView) findViewById(R.id.mouseii_msg);
        textthree= (TextView) findViewById(R.id.equipment_msg);

        textone.setText(R.string.usbii_unconnected);
//        texttwo.setText(R.string.mouseii_unconnected);
//        textthree.setText(R.string.equipmenti_unconnected);


        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        getDeviceList();
        showDialog();
        initLayout();
    }
    private void getDeviceList(){
        UsbManager usbManager= (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbFinder= new USBFinder(usbManager);
        findDeviceList();
    }
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                // Textfive.setText(intent.getAction());
                LogUtil.d("拔出usb了");
                textone.setText(R.string.usbii_unconnected);
//                texttwo.setText(R.string.mouseii_unconnected);
//                textthree.setText(R.string.equipmenti_unconnected);

                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    LogUtil.d("设备的ProductId值为：" + device.getProductId());
                    LogUtil.d("设备的VendorId值为：" + device.getVendorId());
                }
                getDeviceList();
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                LogUtil.d( "插入usb了");
                getDeviceList();
            }
        }
    };

    public class USBFinder {
        private UsbManager mUsbManager;
        public USBFinder(UsbManager usbManager) {
            this.mUsbManager = usbManager;
        }
        public HashMap<String, UsbDevice> findUsbMap() {
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            LogUtil.d("happy:"+mUsbManager.getDeviceList());
            Iterator<UsbDevice> deviceIterator = deviceMap.values().iterator();
            String buf=deviceMap.size()+"";
            while (deviceIterator.hasNext()){
                UsbDevice usbDevice = deviceIterator.next();
                int deviceClass = usbDevice.getDeviceClass();
                if(deviceClass==0) {
                    UsbInterface anInterface = usbDevice.getInterface(0);
                    if (anInterface.getInterfaceClass() == 8) {
                        textone.setText(R.string.usbi_connected);
                        usbotg = true;
                        update ();
                     //   mSuccess.setVisibility(View.VISIBLE);
                        LogUtil.d("Finger device Type",anInterface + "");
                    } else if (anInterface.getInterfaceClass() == 3) {
                        texttwo.setText(R.string.mousei_connected);
                        usbotg = true;
                        update ();
                      //  mSuccess.setVisibility(View.VISIBLE);
                        LogUtil.d("Finger device Type",anInterface + "");
                    } else {
                        textthree.setText(R.string.equipment_connected);
                     //   mSuccess.setVisibility(View.VISIBLE);
                        usbotg = true;
                        update ();
                        LogUtil.d("Finger device Type",anInterface + "");
                    }
                }
            }


            return deviceMap;
        }


        public UsbDevice findUsbDevice(int vendorId, int productId) {
            HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
            Iterator iterator = deviceMap.values().iterator();
            UsbDevice localUsbDevice;
            while (iterator.hasNext()) {
                localUsbDevice = (UsbDevice) iterator.next();
                if (localUsbDevice.getVendorId() == vendorId || localUsbDevice.getProductId() == productId) {
                    return localUsbDevice;
                }
            }
            return null;
        }
    }
    public HashMap<String, UsbDevice> findDeviceList() {
        LogUtil.d("happyhappy ---1");
        return mUsbFinder.findUsbMap();
    }
    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            mFail.setBackgroundColor(getResources().getColor(com.meigsmart.meigrs32.R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 11 && data != null){
            deInit(mFatherName, data.getIntExtra("result", FAILURE));
        }
    }
    private void showDialog() {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(TenPinWaferActivity.this);
        dialog.setMessage(R.string.power_led_on);
        dialog.setPositiveButton(R.string.str_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ledstate = true;
                        update ();

                    }
                });
        dialog.setNegativeButton(R.string.str_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ledstate = false;
                        update ();

                    }
                });
        dialog.setCancelable(false);
        dialog.show();
    }
    /**
     * 根据XML动态创建按键测试布局
     */
    private void initLayout() {
        File configFile = new File(Const.TENPIN_CONFIG_XML_PATH);
        if (!configFile.exists()) {
            ToastUtil.showCenterLong(getString(R.string.config_xml_not_found, Const.TENPIN_CONFIG_XML_PATH));
            deInit(mFatherName, NOTEST);//update state to no test
            finish();
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(Const.TENPIN_CONFIG_XML_PATH);
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
                                LogUtil.d("ooooooooooo"+keyName);
                                LogUtil.d("ooooooooo"+keyCode);
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
                                        mTestKeyMap.put(keyCode, new TenPinWaferActivity.TestKey(keyName, keyCode, testButton));
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
            TenPinWaferActivity.TestKey key = mTestKeyMap.get(mkeycode + "");
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

    /**
     * 刷新按键按下的状态，如果都按下则认为测试成功，直接pass退出
     */
    private void refreshTestResult() {
        Iterator<Map.Entry<String, TenPinWaferActivity.TestKey>> iterator = mTestKeyMap.entrySet().iterator();
        boolean allpass = true;
        while (iterator.hasNext()) {
            Map.Entry<String, TenPinWaferActivity.TestKey> entry = iterator.next();
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

            keyborad = true;
            update ();
        }
    }
    private  void update ()
    {
        if (ledstate && usbotg && keyborad){
            mSuccess.setVisibility(View.VISIBLE);
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
}
