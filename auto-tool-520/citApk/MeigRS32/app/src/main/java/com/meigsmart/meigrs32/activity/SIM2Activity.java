package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class SIM2Activity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SIM2Activity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.sim1)
    public TextView mSim1;
    @BindView(R.id.signal)
    public TextView mSignal;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private int phoneCount = TelephonyManager.getDefault().getSimCount();
    private TelephonyManager telMgr = null;
    private SimStateReceive mReceive = null;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static int SIM_VALID = 0;
    private final static int SIM_INVALID = 1;
    private boolean isInsert = false;

    private String fine = "sim state fine";
    private String imeishownumber = "common_imei_show_tag_value";
    private MyPhoneStateListener myphonelister;
    private TelephonyManager Tel;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sim;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.SIM2Activity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mLayout.setVisibility(View.VISIBLE);
        telMgr = TelephonyManager.from(mContext);
        myphonelister=new MyPhoneStateListener(this);
        //Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telMgr.listen(myphonelister, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    if (isInsert)
                        deInit(mFatherName, SUCCESS);
                    //else deInit(mFatherName, FAILURE, Const.RESULT_TIMEOUT);
                }

                if (isStartTest) mHandler.sendEmptyMessage(1003);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        LogUtil.d("initData end.");

    }

    public class MyPhoneStateListener extends PhoneStateListener {
        private Context context;

        public MyPhoneStateListener(Context context) {
            this.context = context;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            int lv = signalStrength.getGsmSignalStrength();
            mSignal.setText(Html.fromHtml(
                    getResources().getString(R.string.sim_signalStrength) +
                            "&nbsp;" + "<font color='#0000FF'>" + lv + "</font>"
            ));

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceive = new SimStateReceive();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SIM_STATE_CHANGED);
        registerReceiver(mReceive, filter);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1002:
                    int state = (int) msg.obj;
                    if (state == SIM_VALID) {
                        showDevice();
                        isStartTest = true;
                    } else if (state == SIM_INVALID) {
                        mSim1.setText(R.string.sim_insert_sim);
                        mSignal.setText(Html.fromHtml(
                                getResources().getString(R.string.sim_signalStrength) +
                                        "&nbsp;" + "<font color='#0000FF'>" + "0" + "</font>"
                        ));
                        LogUtil.d("ojellyooooooooo");

                    } else {
                        mSim1.setText(R.string.sim_state_unknown);
                        mSignal.setText(Html.fromHtml(
                                getResources().getString(R.string.sim_signalStrength) +
                                        "&nbsp;" + "<font color='#0000FF'>" + "0" + "</font>"
                        ));
                        LogUtil.d("ojellyoobboo");
                    }
                    break;
                case 1003:
                    if (isInsert) {
                        if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME))
                            deInit(mFatherName, SUCCESS);
                        else mSuccess.setVisibility(View.VISIBLE);
                    }
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mReceive != null)
        unregisterReceiver(mReceive);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(mReceive != null) {
            unregisterReceiver(mReceive);
            mReceive = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
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

    private void showDevice() {
        LogUtil.d("sim card number :" + phoneCount);
        mSim1.setText("");
        int i;

        List<String> keyList = getKeyList();
        List<String> resultList1 = getResultList(1);
        LogUtil.d("list 1 size:" + resultList1.size());

        for (i = 0; i < keyList.size(); i++) {
            mSim1.append(keyList.get(i) + resultList1.get(i) + "\n");
        }
        // add by maohaojie on 2019.02.18 for bug 23962 start
        //String hardware = SystemProperties.get(getResources().getString(R.string.version_default_config_hardware_version_persist), getString(R.string.empty));
        if (SystemProperties.get("ro.product.name", "").contains("Hera51")) {
            return;
        }
     // add by maohaojie on 2019.02.18 for bug 23962 end
        LogUtil.d("phoneCount:" + phoneCount);
    }

    public static int getSubIdbySlot(int slot) {
        int subid[] = SubscriptionManager.getSubId(slot);
        if (subid != null) {
            return subid[0];
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    @SuppressLint("NewApi")
    private List<String> getResultList(int simId) {
        List<String> resultList = new ArrayList<String>();
        int subId = getSubIdbySlot(simId);

        if (telMgr == null) {
            sendErrorMsgDelayed(mHandler, "TelephonyManager is null");
            return null;
        }

        if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_READY) {
            resultList.add("sim state fine");
            isInsert = true;
        } else if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_ABSENT) {
            resultList.add("sim state no sim");
            mSignal.setText(Html.fromHtml(
                    getResources().getString(R.string.sim_signalStrength) +
                            "&nbsp;" + "<font color='#0000FF'>" + "0" + "</font>"
            ));
            LogUtil.d("ojellyoooooooooooooooooooooooooooooo");

        } else {
            resultList.add("sim state unknown");
            mSignal.setText(Html.fromHtml(
                    getResources().getString(R.string.sim_signalStrength) +
                            "&nbsp;" + "<font color='#0000FF'>" + "0" + "</font>"
            ));
            LogUtil.d("ojellyoooo");

        }

        if (telMgr.getSimCountryIsoForPhone(simId).equals("")) {
            resultList.add("can not get country");
        } else {
            resultList.add(telMgr.getSimCountryIsoForPhone(simId));
        }

        if (telMgr.getSimOperatorNumericForPhone(simId).equals("")) {
            resultList.add("can not get operator");
        } else {
            resultList.add(telMgr.getSimOperatorNumericForPhone(simId));
        }
        if (telMgr.getSimOperatorNameForPhone(simId).equals("")) {
            resultList.add("can not get operator name");
        } else {
            resultList.add(telMgr.getSimOperatorNameForPhone(simId));
        }

        if (!TextUtils.isEmpty(telMgr.getSimSerialNumber(subId))) {
            resultList.add(telMgr.getSimSerialNumber(subId));
        } else {
            resultList.add("can not get serial number");
        }

        if (telMgr.getSubscriberId(subId) != null) {
            resultList.add(telMgr.getSubscriberId(subId));
        } else {
            resultList.add("can not get subscriber id");
        }

        if (telMgr.getDeviceId(simId) != null) {
            resultList.add(telMgr.getDeviceId(simId));
        } else {
            resultList.add("can not get device id");
        }

        if (telMgr.getLine1Number(subId) != null) {
            resultList.add(telMgr.getLine1Number(subId));
        } else {
            resultList.add("can not get phone number");
        }

        if (telMgr.getPhoneType(simId) == 0) {
            resultList.add("NONE");
        } else if (telMgr.getPhoneType(simId) == 1) {
            resultList.add("GSM");
        } else if (telMgr.getPhoneType(simId) == 2) {
            resultList.add("CDMA");
        } else if (telMgr.getPhoneType(simId) == 3) {
            resultList.add("SIP");
        }

        if (telMgr.getDataState() == 0) {
            resultList.add("disconnected");
        } else if (telMgr.getDataState() == 1) {
            resultList.add("connecting");
        } else if (telMgr.getDataState() == 2) {
            resultList.add("connected");
        } else if (telMgr.getDataState() == 3) {
            resultList.add("suspended");
        }

        if (telMgr.getDataActivity() == 0) {
            resultList.add("none");
        } else if (telMgr.getDataActivity() == 1) {
            resultList.add("in");
        } else if (telMgr.getDataActivity() == 2) {
            resultList.add("out");
        } else if (telMgr.getDataActivity() == 3) {
            resultList.add("in/out");
        } else if (telMgr.getDataActivity() == 4) {
            resultList.add("dormant");
        }

        if (!telMgr.getNetworkCountryIsoForPhone(simId).equals("")) {
            resultList.add(telMgr.getNetworkCountryIsoForPhone(simId));
        } else {
            resultList.add("can not get network country");
        }

        if (telMgr.getNetworkType(simId) == 0) {
            resultList.add("unknown");
        } else if (telMgr.getNetworkType(simId) == 1) {
            resultList.add("gprs");
        } else if (telMgr.getNetworkType(simId) == 2) {
            resultList.add("edge");
        } else if (telMgr.getNetworkType(simId) == 3) {
            resultList.add("umts");
        } else if (telMgr.getNetworkType(simId) == 4) {
            resultList.add("hsdpa");
        } else if (telMgr.getNetworkType(simId) == 5) {
            resultList.add("hsupa");
        } else if (telMgr.getNetworkType(simId) == 6) {
            resultList.add("hspa");
        } else if (telMgr.getNetworkType(simId) == 7) {
            resultList.add("cdma");
        } else if (telMgr.getNetworkType(simId) == 8) {
            resultList.add("evdo 0");
        } else if (telMgr.getNetworkType(simId) == 9) {
            resultList.add("evdo a");
        } else if (telMgr.getNetworkType(simId) == 10) {
            resultList.add("evdo b");
        } else if (telMgr.getNetworkType(simId) == 11) {
            resultList.add("1xrtt");
        } else if (telMgr.getNetworkType(simId) == 12) {
            resultList.add("iden");
        } else if (telMgr.getNetworkType(simId) == 13) {
            resultList.add("lte");
        } else if (telMgr.getNetworkType(simId) == 14) {
            resultList.add("ehrpd");
        } else if (telMgr.getNetworkType(simId) == 15) {
            resultList.add("hspap");
        }

        return resultList;
    }

    private List<String> getKeyList() {
        List<String> keyList = new ArrayList<String>();
        keyList.add("Sim Status:  ");

        keyList.add("Sim Country:  ");
        keyList.add("Sim Operator:  ");
        keyList.add("Sim Operator Name:  ");
        keyList.add("Sim Serial Number:  ");
        keyList.add("Subscriber Id:  ");
        keyList.add("Device Id:  ");
        //keyList.add("Line 1 Number:  ");
        keyList.add("Phone Type:  ");
        keyList.add("Data State:  ");
        keyList.add("Data Activity:  ");
        keyList.add("Network Country:  ");
        //keyList.add("Network Operator:  ");
        keyList.add("Network Type:  ");

        return keyList;
    }

    public class SimStateReceive extends BroadcastReceiver {

        private int simState = SIM_INVALID;

        public int getSimState() {
            return simState;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
                int state = telMgr.getSimState();
                switch (state) {
                    case TelephonyManager.SIM_STATE_READY:
                        simState = SIM_VALID;
                        break;
                    case TelephonyManager.SIM_STATE_UNKNOWN:
                    case TelephonyManager.SIM_STATE_ABSENT:
                    case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                    case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                    case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                    default:
                        simState = SIM_INVALID;
                        break;
                }

                Message msg = mHandler.obtainMessage();
                msg.what = 1002;
                msg.obj = simState;
                mHandler.sendMessage(msg);
            }

        }
    }
}
