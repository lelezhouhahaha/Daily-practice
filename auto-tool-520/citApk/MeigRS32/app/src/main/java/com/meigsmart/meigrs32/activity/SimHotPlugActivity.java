package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class SimHotPlugActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SimHotPlugActivity mContext;
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
    @BindView(R.id.remind_info)
    public TextView mSimReminInfo;
    @BindView(R.id.sim)
    public TextView mSim;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private int phoneCount = TelephonyManager.getDefault().getSimCount();
    private TelephonyManager telMgr;
    private SimStateReceive mReceive;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static int SIM_VALID = 0;
    private final static int SIM_INVALID = 1;

    private String fine = "sim state fine";
    private boolean simInsertStatus = false;
    private boolean simPullOutStatus = false;
    private boolean finishedResultTest = false;
    private boolean simNotInsertAction = false;
    private boolean simInsertAction = false;
    private boolean simPulloutAction = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sim_hot_plug;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
//        mTitle.setText(R.string.pcba_sim1_hot_plug);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mTitle.setText(super.mName);
        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mReceive = new SimStateReceive();

        mLayout.setVisibility(View.VISIBLE);
        telMgr = TelephonyManager.from(mContext);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (mConfigTime == 0 ||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    if (finishedResultTest)
                        deInit(mFatherName, SUCCESS);
                   // else deInit(mFatherName, FAILURE, Const.RESULT_TIMEOUT);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
        if (telMgr != null) {
            if (telMgr.getSimState(0) == TelephonyManager.SIM_STATE_READY) {
                showDevice();
                mSim.setVisibility(View.VISIBLE);
                mSimReminInfo.setText(R.string.sim_hot_plug_after_insert_info);
            } else {
                mSim.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1002:
                    setdefaultLanguage(SimHotPlugActivity.this);
                    int state = (int) msg.obj;
                    if (state == SIM_VALID) {
                        showDevice();
                        mSim.setVisibility(View.VISIBLE);
                    }else mSim.setVisibility(View.GONE);
                    LogUtil.d("citapk state:" + state);
                    LogUtil.d("citapk simNotInsertAction:" + simNotInsertAction);
                    LogUtil.d("citapk simInsertAction:" + simInsertAction);
                    LogUtil.d("citapk simPulloutAction:" + simPulloutAction);

                    if(simNotInsertAction && !simInsertAction && !simPulloutAction)
                        mSimReminInfo.setText(R.string.sim_hot_plug_insert_info);
                    else if(simInsertAction && (state == SIM_VALID))
                        mSimReminInfo.setText(R.string.sim_hot_plug_after_insert_info);
                    else if(simPulloutAction && (state == SIM_INVALID) )
                        mSimReminInfo.setText(R.string.sim_hot_plug_insert_info);
                    else if(state == SIM_VALID)
                        mSimReminInfo.setText(R.string.sim_hot_plug_after_insert_info);

                    if(simNotInsertAction && simInsertAction && simPulloutAction) {
                        simNotInsertAction = false;
                        simInsertAction = false;
                        simPulloutAction = false;
                        simInsertStatus = false;
                        simPullOutStatus = false;
                        finishedResultTest = true;
                        //mSimReminInfo.setText(R.string.sim_test_finish);
                        mSim.setVisibility(View.GONE);
                        mHandler.sendEmptyMessage(1003);
                    }
                    LogUtil.d("citapk 1 state:" + state);
                    LogUtil.d("citapk 1 simNotInsertAction:" + simNotInsertAction);
                    LogUtil.d("citapk 1 simInsertAction:" + simInsertAction);
                    LogUtil.d("citapk 1 simPulloutAction:" + simPulloutAction);


                    break;
                case 1003:
                    if (finishedResultTest) {
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
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SIM_STATE_CHANGED);
        registerReceiver(mReceive, filter);

    }
    @Override
    protected void onPause() {
        super.onPause();
        //if(mReceive != null) {
           // unregisterReceiver(mReceive);
           // mReceive = null;
        //}
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
        mSim.setText("");
        int i;

        List<String> keyList = getKeyList();
        List<String> resultList0 = getResultList(0);
        List<String> resultList1 = getResultList(1);
        LogUtil.d("list 0 size:" + resultList0.size());
        LogUtil.d("list 1 size:" + resultList1.size());

        for (i = 0; i < keyList.size(); i++) {
            mSim.append(keyList.get(i) + resultList0.get(i) + "\n");
        }
        // add by maohaojie on 2019.02.18 for bug 23962 start
       // String hardware = SystemProperties.get(getResources().getString(R.string.version_default_config_hardware_version_persist), getString(R.string.empty));
        if (SystemProperties.get("ro.product.name", "").contains("Hera51")) {
            return;
        }
     // add by maohaojie on 2019.02.18 for bug 23962 end
        LogUtil.d("phoneCount:" + phoneCount);
        LogUtil.d("resultList0.get(0):" + resultList0.get(0));
        LogUtil.d("resultList1.get(0):" + resultList1.get(0));
        /*if ((phoneCount == 2 && resultList1.get(0).equals(fine) && resultList0.get(0).equals(fine)) || (phoneCount == 1 && resultList0.get(0).equals(fine))) {
            isInsert = true;
        } else {
            isInsert = false;
        }*/
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
            //isInsert = true;
        } else if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_ABSENT) {
            resultList.add("sim state no sim");
        } else {
            resultList.add("sim state unknown");
        }

        //add by wangjinfeng to adapt to Android 11 start
        try {//Modified to reflect acquisition to adapt to Android 11 interface update
            Class<?> class_TPM = Class.forName("android.telephony.TelephonyManager");
            Method method_getSimCountryIsoForPhone = class_TPM.getMethod("getSimCountryIsoForPhone", new Class[]{int.class});

            String result = method_getSimCountryIsoForPhone.invoke(null, simId).toString();
            if("".equals(result)){
                resultList.add("can not get country");
            }else{
                resultList.add(result);
            }
        }catch (Exception e) {
            resultList.add("can not get country");
            e.printStackTrace();
        }
        //add by wangjinfeng to adapt to Android 11 end
//        if (telMgr.getSimCountryIsoForPhone(simId).equals("")) {
//            resultList.add("can not get country");
//        } else {
//            resultList.add(telMgr.getSimCountryIsoForPhone(simId));
//        }

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
                //int state = telMgr.getSimState();
               // LogUtil.d("citapk onReceive state:" + state);
                final int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                final int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                LogUtil.d("chenyong citapk onReceive phoneId:"+ phoneId + ",slotId:" + slotId  );
                if(phoneId!=0)
                    return;
                else{
                    final int state =  TelephonyManager.getDefault().getSimState( phoneId );
                    LogUtil.d("chenyong citapk onReceive simState:" + state );
                switch (state) {
                    case TelephonyManager.SIM_STATE_READY:
                        simState = SIM_VALID;
                        simInsertStatus = true;
                        if(simNotInsertAction)
                            simInsertAction = true;
                        LogUtil.d("citapk simInsert");
                        break;
                    case TelephonyManager.SIM_STATE_UNKNOWN:
                    case TelephonyManager.SIM_STATE_ABSENT:
                    case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                    case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                    case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                    default:
                        simState = SIM_INVALID;
                        if(simInsertStatus) {
                            //simPullOutStatus = true;
                            simPulloutAction = true;
                            simInsertStatus = false;  //reset sim insert status.
                            LogUtil.d("citapk simPullOut");
                        }
                        simNotInsertAction = true;
                        LogUtil.d("citapk simState == SIM_INVALID");
                        break;
                }
                Message msg = mHandler.obtainMessage();
                msg.what = 1002;
                msg.obj = simState;
                mHandler.sendMessage(msg);}
            }

        }
    }
}
