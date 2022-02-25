package com.meigsmart.meigrs32.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import butterknife.BindView;

public class RJ45TestActivity extends BaseActivity implements View.OnClickListener
        , PromptDialog.OnPromptDialogCallBack {

    private RJ45TestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.RJ45_IP)
    public TextView mIpAdress;
    @BindView(R.id.RJ45_POE)
    public TextView mPOE;
    private String mFatherName = "";

    private String mRouterConfig = "common_router_judge_config";

    ConnectivityManager mConnectivityManager = null;
    TextView textview;

    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
    boolean append = true;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_rj45_test;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        //mFail.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.run_in_rj45test);

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkChangeReceiver();

        textview = (TextView) findViewById(R.id.RJ45_msg);

        textview.setTextSize(20);
        textview.setGravity(Gravity.CENTER);
        showText();
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        setAirPlaneMode(this,true);
    }

    private void setAirPlaneMode(Context context, boolean enable) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable ? 1 : 0);
        } else {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
        }
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enable);
        context.sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setAirPlaneMode(this,false);
        unregisterReceiver(networkReceiver);
    }

    private void showText() {

        boolean isConnected = isNetworkConnected();
        if (isConnected) {
            String netType = getTypeString(GetNetype());
            if ("ethernet".equals(netType)) {
                if(isMT535_version) {
                    mIpAdress.setVisibility(View.VISIBLE);
                    String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RIL_STATE);
                    String wifi_path1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
                    if(((null != wifi_path) && !wifi_path.isEmpty())||((null != wifi_path1) && !wifi_path1.isEmpty())){
                        WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
                        WIFI_BUILD1 = FileUtil.readFromFile(wifi_path1).contains("0");
                        ERO_BUILD = FileUtil.readFromFile(wifi_path1).contains("1");
                        LA_BUILD = FileUtil.readFromFile(wifi_path1).contains("2");
                    }
                    if(!WIFI_BUILD1){
                        mPOE.setVisibility(View.VISIBLE);
                        mPOE.setText(R.string.RJ45_connected1);
                    }
                }
                if(append){
                    mIpAdress.append(getLocalIp());
                    append =false;
                }
                textview.setTextColor(Color.GREEN);
                textview.setText(R.string.RJ45_connected);
                mSuccess.setVisibility(View.VISIBLE);
                if(is_mt535){
                    mSuccess.performClick();
                }
                if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);//auto pass pcba
                }

            }
            else
            {
                textview.setTextColor(Color.BLACK);
                textview.setText(R.string.RJ45_unconnected);
            }
        } else {
            textview.setTextColor(Color.BLACK);
            textview.setText(R.string.RJ45_unconnected);
        }

        String isRouter = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH,mRouterConfig);
        if(isRouter.contains("true")){
            textview.setTextColor(Color.BLACK);
            textview.setText(R.string.RJ45_router);
            mSuccess.setVisibility(View.VISIBLE);
        }

    }

    private void registerNetworkChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, intentFilter);
    }

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            showText();
        }

    };

    private String getTypeString(int type) {
        String typeStr = null;
        switch (type) {
            case 1:
                typeStr = "wifi";
                break;
            case 9:
                typeStr = "ethernet";
                break;
            default:
                typeStr = "unknow type " + type;
        }
        return typeStr;
    }

    private boolean isNetworkConnected() {
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null) {
            return mNetworkInfo.isAvailable();
        }
        return false;
    }

    private int GetNetype() {
        int netType = -1;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        netType = networkInfo.getType();
        return netType;
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> enumerationNi = NetworkInterface
                    .getNetworkInterfaces();
            while (enumerationNi.hasMoreElements()) {
                NetworkInterface networkInterface = enumerationNi.nextElement();
                String interfaceName = networkInterface.getDisplayName();
                LogUtil.i("RJ45TestActivity", "interfaceName:" + interfaceName);
                if (interfaceName.equals("eth0")) {
                    Enumeration<InetAddress> enumIpAddr = networkInterface
                            .getInetAddresses();
                    while (enumIpAddr.hasMoreElements()) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && inetAddress instanceof Inet4Address) {
                            LogUtil.i("RJ45TestActivity", inetAddress.getHostAddress() + "   ");
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
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
}
