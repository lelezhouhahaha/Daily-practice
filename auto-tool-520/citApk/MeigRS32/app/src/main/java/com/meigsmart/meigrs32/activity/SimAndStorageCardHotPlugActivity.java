package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
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

public class SimAndStorageCardHotPlugActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SimAndStorageCardHotPlugActivity mContext;
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
    @BindView(R.id.remind_info2)
    public TextView mSimReminInfo2;
    @BindView(R.id.sim)
    public TextView mSim;
    @BindView(R.id.sim2)
    public TextView mSim2;
    @BindView(R.id.sdState)
    public TextView mSDState;
    @BindView(R.id.info)
    public TextView mInfo;

    private int mConfigResult;
    private int mConfigTime = 0;
    private Runnable mRun;

    private int phoneCount = TelephonyManager.getDefault().getSimCount();
    private TelephonyManager telMgr;
    private SimStateReceive mSIMReceive;
    private StroageCardStateReceive mStorageCradReceive;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static int SIM_VALID = 0;
    private final static int SIM_INVALID = 1;
    private final static int StorageCard_MOUNTED = 2;
    private final static int StorageCard_UNMOUNTED = 3;

    private boolean simInsertStatus = false;
    private boolean simPullOutStatus = false;
    private int mSlotId = 0;
    private boolean finishedResultTest = false;
    private boolean simNotInsertAction = false;
    private boolean simInsertAction = false;
    private boolean simPulloutAction = false;
    private boolean mStorageInsertAction = false;
    private boolean mStoragePulloutAction = false;
    private boolean finishStorageCradResult = false;
    private boolean sim2InsertStatus = false;
    private boolean sim2PullOutStatus = false;
    private boolean finishedResultTest2 = false;
    private boolean sim2NotInsertAction = false;
    private boolean sim2InsertAction = false;
    private boolean sim2PulloutAction = false;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_sim_and_storage_card_hot_plug;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.pcba_sim_and_storage_card_hot_plug);

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

        mSIMReceive = new SimStateReceive();
        mStorageCradReceive = new StroageCardStateReceive();

        mLayout.setVisibility(View.VISIBLE);
        telMgr = TelephonyManager.from(mContext);

        if(getSDCardMemory() != null) {
            mInfo.setText(R.string.storage_card_hot_plug_after_insert_info);
            StringBuffer sb = new StringBuffer();
            sb.append(getResources().getString(R.string.sd_card_size_info)).append(" ").append(getSDCardMemory()).append("\n");
            mSDState.setText(sb.toString());
        }
        else mInfo.setText(R.string.storage_card_hot_plug_insert_info);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SIM_STATE_CHANGED);
        registerReceiver(mSIMReceive, filter);
        final IntentFilter filter_1 = new IntentFilter();
        filter_1.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter_1.addAction(Intent.ACTION_MEDIA_EJECT);
        filter_1.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter_1.addDataScheme("file");
        registerReceiver(mStorageCradReceive, filter_1);

        if(phoneCount > 1){
            mSimReminInfo2.setVisibility(View.VISIBLE);
        }else{
            mSimReminInfo2.setVisibility(View.GONE);
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (mConfigTime == 0 && mFatherName.equals(MyApplication.RuninTestNAME)||
                        mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext)) {
                    if (finishedResultTest && finishStorageCradResult)
                        deInit(mFatherName, SUCCESS);
                   // else deInit(mFatherName, FAILURE, Const.RESULT_TIMEOUT);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    int storage_card_state = (int) msg.obj;
                    switch (storage_card_state){
                        case StorageCard_MOUNTED:
                            if(getSecondaryStorageState()) {
                                StringBuffer sb = new StringBuffer();
                                sb.append(getResources().getString(R.string.sd_card_size_info)).append(" ").append(getSDCardMemory()).append("\n");
                            /*mSDState.setText(getResult(DataUtil.getRomSpace(mContext),DataUtil.getTotalMemory(mContext,
                                getResources().getString(R.string.version_default_config_software_version_ram_size_path))
                                ,getSDCardMemory()));*/
                                mSDState.setText(sb.toString());
                                mInfo.setText(R.string.storage_card_hot_plug_after_insert_info);
                                mSDState.setVisibility(View.VISIBLE);
                            }
                            break;
                        case StorageCard_UNMOUNTED:
                            mInfo.setText(R.string.storage_card_hot_plug_insert_info);
                            mSDState.setVisibility(View.GONE);
                            break;
                        default:
                            break;
                    }
                    if(mStoragePulloutAction && mStorageInsertAction){
                        finishStorageCradResult =true;
                        mHandler.sendEmptyMessage(1003);
                    }
                    break;
                case 1002:
                    int SIM_state = (int) msg.obj;
                    if (SIM_state == SIM_VALID) {
                        showDevice(0);
                        mSim.setVisibility(View.VISIBLE);
                    }else mSim.setVisibility(View.GONE);
                    LogUtil.d("citapk sim_state:" + SIM_state);
                    LogUtil.d("citapk simNotInsertAction:" + simNotInsertAction);
                    LogUtil.d("citapk simInsertAction:" + simInsertAction);
                    LogUtil.d("citapk simPulloutAction:" + simPulloutAction);

                    if(simNotInsertAction && !simInsertAction && !simPulloutAction)
                        mSimReminInfo.setText(R.string.sim_hot_plug_insert_info);
                    else if(simInsertAction && (SIM_state == SIM_VALID))
                        mSimReminInfo.setText(R.string.sim_hot_plug_after_insert_info);
                    else if(simPulloutAction && (SIM_state == SIM_INVALID) )
                        mSimReminInfo.setText(R.string.sim_hot_plug_insert_info);
                    else if(SIM_state == SIM_VALID)
                        mSimReminInfo.setText(R.string.sim_hot_plug_after_insert_info);

                    if(simNotInsertAction && simInsertAction && simPulloutAction ) {
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
                    LogUtil.d("citapk 1 sim_state:" + SIM_state);
                    LogUtil.d("citapk 1 simNotInsertAction:" + simNotInsertAction);
                    LogUtil.d("citapk 1 simInsertAction:" + simInsertAction);
                    LogUtil.d("citapk 1 simPulloutAction:" + simPulloutAction);


                    break;
                case 1003:
                    LogUtil.d("citapk finishedResultTest :" + finishedResultTest+"citapk finishedResultTest2 :" + finishedResultTest2+" finishStorageCradResult: "+finishStorageCradResult);
                    if(phoneCount==1) {
                        if (finishedResultTest && finishStorageCradResult) {
                            if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME))
                                deInit(mFatherName, SUCCESS);
                            else
                                mSuccess.setVisibility(View.VISIBLE);
                        }
                    }else if(phoneCount==2){
                        if (finishedResultTest&&finishedResultTest2 && finishStorageCradResult) {
                            if (!mFatherName.equals(MyApplication.PCBASignalNAME) && !mFatherName.equals(MyApplication.PreSignalNAME))
                                deInit(mFatherName, SUCCESS);
                            else
                                mSuccess.setVisibility(View.VISIBLE);
                        }
                    }

                    break;
                case 1004:
                    int SIM2_state = (int) msg.obj;
                    if (SIM2_state == SIM_VALID) {
                        showDevice(1);
                        mSim2.setVisibility(View.VISIBLE);
                    }else mSim2.setVisibility(View.GONE);
                    LogUtil.d("citapk sim_state:" + SIM2_state);
                    LogUtil.d("citapk simNotInsertAction:" + sim2NotInsertAction);
                    LogUtil.d("citapk simInsertAction:" + sim2InsertAction);
                    LogUtil.d("citapk simPulloutAction:" + sim2PulloutAction);

                    if(sim2NotInsertAction && !sim2InsertAction && !sim2PulloutAction)
                        mSimReminInfo2.setText(R.string.sim_hot_plug_insert_info);
                    else if(sim2InsertAction && (SIM2_state == SIM_VALID))
                        mSimReminInfo2.setText(R.string.sim_hot_plug_after_insert_info);
                    else if(sim2PulloutAction && (SIM2_state == SIM_INVALID) )
                        mSimReminInfo2.setText(R.string.sim_hot_plug_insert_info);
                    else if(SIM2_state == SIM_VALID)
                        mSimReminInfo2.setText(R.string.sim_hot_plug_after_insert_info);

                    if(sim2NotInsertAction && sim2InsertAction && sim2PulloutAction ) {
                        sim2NotInsertAction = false;
                        sim2InsertAction = false;
                        sim2PulloutAction = false;
                        sim2InsertStatus = false;
                        sim2PullOutStatus = false;
                        finishedResultTest2 = true;
                        //mSimReminInfo.setText(R.string.sim_test_finish);
                        mSim2.setVisibility(View.GONE);
                        mHandler.sendEmptyMessage(1003);
                    }
                    LogUtil.d("citapk 1 sim_state:" + SIM2_state);
                    LogUtil.d("citapk 1 simNotInsertAction:" + sim2NotInsertAction);
                    LogUtil.d("citapk 1 simInsertAction:" + sim2InsertAction);
                    LogUtil.d("citapk 1 simPulloutAction:" + sim2PulloutAction);


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
        if(mSIMReceive != null)
            unregisterReceiver(mSIMReceive);
        if (mStorageCradReceive!=null)
            unregisterReceiver(mStorageCradReceive);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
        mHandler.removeMessages(9999);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
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

    private void showDevice(int phoneId) {
        LogUtil.d("sim card number :" + phoneCount);

        int i;

        List<String> keyList = getKeyList();
        List<String> resultList0 = getResultList(0);
        List<String> resultList1 = getResultList(1);
        LogUtil.d("list 0 size:" + resultList0.size());
        LogUtil.d("list 1 size:" + resultList1.size());

        if (phoneId== 1) {
            mSim2.setText("");
            for (i = 0; i < keyList.size(); i++) {
                mSim2.append(keyList.get(i) + resultList1.get(i) + "\n");
            }
        } else if(phoneId == 0) {
            mSim.setText("");
            for (i = 0; i < keyList.size(); i++) {
                mSim.append(keyList.get(i) + resultList0.get(i) + "\n");
            }
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
                //   int state = telMgr.getSimState();
                    mSlotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                //     LogUtil.d("citapk onReceive state:" + state);
                final int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                final int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                LogUtil.d("chenyong citapk onReceive phoneId:" + phoneId + ",slotId:" + slotId);
                if (phoneId == 0) {
                    final int state = TelephonyManager.getDefault().getSimState(phoneId);
                    LogUtil.d("chenyong citapk onReceive simState:" + state);
                    switch (state) {
                        case TelephonyManager.SIM_STATE_READY:
                            simState = SIM_VALID;
                            simInsertStatus = true;
                            if (simNotInsertAction)
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
                            if (simInsertStatus) {
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
                    mHandler.sendMessage(msg);
                } else if (phoneId == 1) {
                    final int state = TelephonyManager.getDefault().getSimState(phoneId);
                    LogUtil.d("chenyong citapk onReceive simState:" + state);
                    switch (state) {
                        case TelephonyManager.SIM_STATE_READY:
                            simState = SIM_VALID;
                            sim2InsertStatus = true;
                            if (sim2NotInsertAction)
                                sim2InsertAction = true;
                            LogUtil.d("citapk simInsert");
                            break;
                        case TelephonyManager.SIM_STATE_UNKNOWN:
                        case TelephonyManager.SIM_STATE_ABSENT:
                        case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                        case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                        case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                        default:
                            simState = SIM_INVALID;
                            if (sim2InsertStatus) {
                                //simPullOutStatus = true;
                                sim2PulloutAction = true;
                                sim2InsertStatus = false;  //reset sim insert status.
                                LogUtil.d("citapk sim2PullOut");
                            }
                            sim2NotInsertAction = true;
                            LogUtil.d("citapk sim2State == SIM_INVALID");
                            break;
                    }
                    Message msg = mHandler.obtainMessage();
                    msg.what = 1004;
                    msg.obj = simState;
                    mHandler.sendMessage(msg);

                }else {
                    return;
                }
            }

        }
    }

    public class StroageCardStateReceive extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("citapk SDCardTest", "Receive " + action);
            Message msg = mHandler.obtainMessage();
            switch (action){
                case Intent.ACTION_MEDIA_MOUNTED:
                    mStorageInsertAction = true;
                    msg.what = 1001;
                    msg.obj = StorageCard_MOUNTED;
                    mHandler.sendMessage(msg);
                    break;
                case Intent.ACTION_MEDIA_EJECT:
                    mInfo.setText(R.string.sd_card_hot_plug_wait_info);
                    mSDState.setVisibility(View.GONE);
                    break;
                case Intent.ACTION_MEDIA_UNMOUNTED:
                    mStoragePulloutAction = true;
                    msg.what = 1001;
                    msg.obj = StorageCard_UNMOUNTED;
                    mHandler.sendMessage(msg);
                    break;
                default:
                    break;

            }

        }
    }

    
    public String getSDCardMemory() {
        //File path = Environment.getExternalStorageDirectory();
        try{
            StatFs statFs = new StatFs(getSDPath(mContext));
            long blocksize = statFs.getBlockSize();
            long totalblocks = statFs.getBlockCount();
            long availableblocks = statFs.getAvailableBlocks();
            long totalsize = blocksize * totalblocks;
            long availablesize = availableblocks * blocksize;
            String totalsize_str = Formatter.formatFileSize(this, totalsize);
            String availablesize_strString = Formatter.formatFileSize(this,
                    availablesize);
            return totalsize_str;
        }catch (Exception e){

        }
        return null;
    }

    private String getSDPath(Context mcon) {
        String sd = null;
        StorageManager mStorageManager = (StorageManager) mcon
                .getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        for (int i = 0; i < volumes.length; i++) {
            sd = volumes[i].getPath();
            if(sd.equals("/storage/emulated/0"))
                sd = null;
            LogUtil.d("citapk StorageCardHotPlugActivity sd:" + sd);
            //return sd;
        }
        return sd;
    }

    public boolean getSecondaryStorageState() {
        String SDPath = null;
        try {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod(
                    "getVolumePaths", (Class<?>[]) null);
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, new Object[]{});
            // second element in paths[] is secondary storage path
            SDPath = paths[1];
        } catch (Exception e) {

        }
        if(SDPath != null){
            return true;
        }
        return false;
    }


}
