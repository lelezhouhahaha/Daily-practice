package com.meigsmart.meigrs32.activity;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


public class Tamper_Proof_Activity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private Tamper_Proof_Activity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.content_show)
    public TextView mContentShow;
    @BindView(R.id.test_color1)
    public TextView mTestColor1;
    @BindView(R.id.test_color2)
    public TextView mTestColor2;
    @BindView(R.id.test_color3)
    public TextView mTestColor3;
    @BindView(R.id.test_color4)
    public TextView mTestColor4;
    @BindView(R.id.test_color5)
    public TextView mTestColor5;
    @BindView(R.id.test_color6)
    public TextView mTestColor6;
    @BindView(R.id.trigertest)
    public LinearLayout mTrigertest;
    @BindView(R.id.trigger_status)
    public TextView mtrigger_status;
    @BindView(R.id.trigger_txt)
    public TextView trigger_txt;
    @BindView(R.id.trigger_type)
    public TextView mTrigger_type;
    @BindView(R.id.test_mask1)
    public TextView mTestMask1;
    @BindView(R.id.test_mask2)
    public TextView mTestMask2;
    @BindView(R.id.test_mask3)
    public TextView mTestMask3;
    @BindView(R.id.test_mask4)
    public TextView mTestMask4;
    @BindView(R.id.test_mask5)
    public TextView mTestMask5;
    @BindView(R.id.test_mask6)
    public TextView mTestMask6;
    @BindView(R.id.mask_type)
    public TextView mMask_type;

    public final int TRIGER_TEST_SUSSCESS = 1001;
    public final int TRIGER_TEST_Fail = 1004;
    public final int TEST_SUCCESS = 1002;
    public final int TEST_FAIL = 1003;
    public final int TRIGER_TEST_LOG = 1005;
    public final int CLEAN_TRIGER_LOG = 1006;
    public final int READ_STATUS = 1007;
    public final int READ_TRIGER_TEST_LOG = 1008;

    List<String> tamper = new ArrayList<>();
    List<String> tamper_new = new ArrayList<>();
    List<String> tamper_type = new ArrayList<>();
    private String log;
    private String log_new;
    private long MASK_DYNAMIC1 =  0x000001;
    private long MASK_DYNAMIC2 =  0x000002;
    private long MASK_STAIC1 =  0x001004;
    private long MASK_STAIC2 =  0x002004;
    private long MASK_STAIC3 =  0x004008;
    private long MASK_STAIC4 =  0x008008;
    private long MASK_ROOTKEY_UNNORMAL=  0x000010;

    private long CT_DYNAMIC1 =  0x000002;
    private long CT_DYNAMIC2 =  0x000008;
    private long CT_STAIC1 =  0x000010;
    private long CT_STAIC2 =  0x000020;
    private long CT_STAIC3 =  0x000040;
    private long CT_STAIC4 =  0x000080;


    private long MASK_ROOTKEY_ERROR =  0x000002;
    private long MASK_ROOTKEY_LOST =  0x000001;
    private long CONTACT_TRIGGER = 0x000000;

    private int trigger_status = 0;
    private String resultInfo = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_tamper_proof;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.Tamper_Proof_Test);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterLong(getString(R.string.connect_loading));
            return;
        }

        mHandler.sendEmptyMessageDelayed(READ_STATUS,100);

    }

    private void getTamperLog() {
        new readLog(2).execute();
    }

    private static String convert(String str) {
        StringBuilder builder = new StringBuilder(str);
        str = builder.reverse().toString();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (i % 32 == 0) {
                if (i + 32 > str.length()) {
                    stringBuilder.append(str.substring(i));
                    break;
                }
                stringBuilder.append(str.substring(i, i + 32) + ",");
            }
        }
        str = stringBuilder.reverse().toString();
        if (str.charAt(0) == ',') {
            str = str.substring(1);
        }
        return str;
    }

    public void SplitString(String ppp) {
        String[] strs = ppp.split(",");
        for (int j = 0; j < strs.length; j++) {
            tamper.add(j, strs[j]);
            tamper_new.add(j,tamper.get(j).substring(22,30));
            tamper_type.add(j,tamper.get(j).substring(14,22));
            Log.d("Tamper_Proof_Activity","complete log: "+strs[j]+"  tamper_new:"+tamper_new.get(j)+" tamper_type:"+tamper_type.get(j));
            if(j == 0){
                resultInfo = strToAscii(strs[j])+"\n";
                continue;
            }

            resultInfo =resultInfo + strs[j] + "\n";


            if(CONTACT_TRIGGER == Long.parseLong(tamper_type.get(j),16)){
                if((CT_DYNAMIC1 == Long.parseLong(tamper_new.get(j),16)) || (CT_DYNAMIC1 == (Long.parseLong(tamper_new.get(j),16) & CT_DYNAMIC1))){
                    mTestColor1.setVisibility(View.VISIBLE);
                    mTestColor1.setText(getString(R.string.status_dyanmic1));
                }
                if((CT_DYNAMIC2 == Long.parseLong(tamper_new.get(j),16)) || (CT_DYNAMIC2 == (Long.parseLong(tamper_new.get(j),16) & CT_DYNAMIC2))){
                    mTestColor2.setVisibility(View.VISIBLE);
                    mTestColor2.setText(getString(R.string.status_dyanmic2));
                }
                if((CT_STAIC1 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC1 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC1))){
                    mTestColor3.setVisibility(View.VISIBLE);
                    mTestColor3.setText(getString(R.string.status_static1));
                }
                if((CT_STAIC2 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC2 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC2))){
                    mTestColor4.setVisibility(View.VISIBLE);
                    mTestColor4.setText(getString(R.string.status_static2));
                }
                if((CT_STAIC3 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC3 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC3))){
                    mTestColor5.setVisibility(View.VISIBLE);
                    mTestColor5.setText(getString(R.string.status_static3));
                }
                if((CT_STAIC4 == Long.parseLong(tamper_new.get(j),16)) || (CT_STAIC4 == (Long.parseLong(tamper_new.get(j),16) & CT_STAIC4))){
                    mTestColor6.setVisibility(View.VISIBLE);
                    mTestColor6.setText(getString(R.string.status_static4));
                    mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,100);
                }
                mTrigger_type.setVisibility(View.VISIBLE);
                mTrigger_type.setText(getString(R.string.status_trigger));
            }else{
                if((MASK_DYNAMIC1 == Long.parseLong(tamper_new.get(j),16)) || (MASK_DYNAMIC1 == (Long.parseLong(tamper_new.get(j),16) & MASK_DYNAMIC1))){
                    mTestMask1.setVisibility(View.VISIBLE);
                    mTestMask1.setText(getString(R.string.status_dyanmic1));
                }
                if((MASK_DYNAMIC2 == Long.parseLong(tamper_new.get(j),16)) || (MASK_DYNAMIC2 == (Long.parseLong(tamper_new.get(j),16) & MASK_DYNAMIC2))){
                    mTestMask2.setVisibility(View.VISIBLE);
                    mTestMask2.setText(getString(R.string.status_dyanmic2));
                }
                if((MASK_STAIC1 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC1 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC1))){
                    mTestMask3.setVisibility(View.VISIBLE);
                    mTestMask3.setText(getString(R.string.status_static1));
                }
                if((MASK_STAIC2 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC2 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC2))){
                    mTestMask4.setVisibility(View.VISIBLE);
                    mTestMask4.setText(getString(R.string.status_static2));
                }
                if((MASK_STAIC3 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC3 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC3))){
                    mTestMask5.setVisibility(View.VISIBLE);
                    mTestMask5.setText(getString(R.string.status_static3));
                }
                if((MASK_STAIC4 == Long.parseLong(tamper_new.get(j),16)) || (MASK_STAIC4 == (Long.parseLong(tamper_new.get(j),16) & MASK_STAIC4))){
                    mTestMask6.setVisibility(View.VISIBLE);
                    mTestMask6.setText(getString(R.string.status_static4));
                }
                if(MASK_ROOTKEY_ERROR == Long.parseLong(tamper_type.get(j),16)){
                    mMask_type.setVisibility(View.VISIBLE);
                    mMask_type.setText(getString(R.string.status_rootkey_error));
                }
                if(MASK_ROOTKEY_LOST == Long.parseLong(tamper_type.get(j),16)){
                    mMask_type.setVisibility(View.VISIBLE);
                    mMask_type.setText(getString(R.string.status_rootkey_lost));
                }

             }

            }
        trigger_txt.setText(resultInfo);
        mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,100);
    }

    private void setTextInvisible(){
        mTestColor1.setVisibility(View.GONE);
        mTestColor2.setVisibility(View.GONE);
        mTestColor3.setVisibility(View.GONE);
        mTestColor4.setVisibility(View.GONE);
        mTestColor5.setVisibility(View.GONE);
        mTestColor6.setVisibility(View.GONE);
        mTrigger_type.setVisibility(View.GONE);
        mMask_type.setVisibility(View.GONE);
        mTestMask1.setVisibility(View.GONE);
        mTestMask2.setVisibility(View.GONE);
        mTestMask3.setVisibility(View.GONE);
        mTestMask4.setVisibility(View.GONE);
        mTestMask5.setVisibility(View.GONE);
        mTestMask6.setVisibility(View.GONE);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case TRIGER_TEST_LOG:
                    if(log == null || log.isEmpty()){
                        Log.d("Tamper_Proof_Activity","trigger log is null");
                        mTestColor1.setVisibility(View.VISIBLE);
                        mTestColor1.setText(getString(R.string.status_success));
                        mHandler.sendEmptyMessageDelayed(TRIGER_TEST_SUSSCESS,100);
                    }else{
                        log_new = convert(log);
                        SplitString(log_new);
                    }
                    break;
                case TRIGER_TEST_SUSSCESS:
                    String text = "";
                    text = getString(R.string.Tamper_Proof_Test) + ": "+getString(R.string.success);
                    mContentShow.setText(text);
                    mTrigertest.setBackgroundColor(Color.GREEN);
                    mSuccess.setVisibility(View.VISIBLE);
                    break;
                case TRIGER_TEST_Fail:
                    text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.fail);
                    mContentShow.setText(text);
                    mTrigertest.setBackgroundColor(Color.RED);
                    /*if(mFatherName.equals(MyApplication.MMI1_PreName)||mFatherName.equals(MyApplication.MMI1_PreSignalName)){
                        clearlog.setVisibility(View.VISIBLE);
                        clearlogbtn.setVisibility(View.VISIBLE);
                        clearlogbtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setClearlog();
                                clearlog.setVisibility(View.INVISIBLE);
                                clearlogbtn.setVisibility(View.INVISIBLE);
                                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_LOG,100);
                            }
                        });
                    }*/
                    break;
                case TEST_SUCCESS:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);
                    break;
                case TEST_FAIL:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.red_800));
                    deInit(mFatherName, FAILURE);
                    break;
                case CLEAN_TRIGER_LOG:
                    setClearlog();
                    mHandler.sendEmptyMessageDelayed(READ_TRIGER_TEST_LOG,5000);
                    break;
                case READ_STATUS:
                    new readLog(1).execute();
                    break;
                case READ_TRIGER_TEST_LOG:
                    setTextInvisible();
                    getTamperLog();
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private class readLog extends AsyncTask<Void, Void, Void> {
        int readType;
        public readLog(int type){
            readType = type;
        }
        @Override
        protected void onPreExecute() {
            mContentShow.setText(R.string.tamper_tip);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if(1 == readType) {
                    trigger_status = MyApplication.getInstance().securityOptV2.getSecStatus();
                }
                if(2 == readType) {
                    log = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.TAMPER_LOG);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(1 == readType) {
                if(trigger_status == 0 && !(mFatherName.equals(MyApplication.MMI2_PreName)||mFatherName.equals(MyApplication.MMI2_PreSignalName))){
                    mHandler.sendEmptyMessageDelayed(CLEAN_TRIGER_LOG,100);
                }else{
                    mHandler.sendEmptyMessageDelayed(READ_TRIGER_TEST_LOG,100);
                }

                mtrigger_status.setVisibility(View.VISIBLE);
                mtrigger_status.setText(getString(R.string.trigger_state)+Integer.toHexString(trigger_status));
                trigger_txt.setText(resultInfo);
            }
            if(2 == readType) {
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_LOG,100);
            }

            super.onPostExecute(unused);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(TRIGER_TEST_SUSSCESS);
        mHandler.removeMessages(TRIGER_TEST_Fail);
        mHandler.removeMessages(9999);
        mHandler.removeMessages(TEST_SUCCESS);
        mHandler.removeMessages(TEST_FAIL);

    }

    private void setClearlog(){
        try {
            MyApplication.getInstance().basicOptV2.setSysParam(AidlConstants.SysParam.TERM_STATUS,AidlConstantsV2.SysParam.CLEAR_TAMPER_LOG);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

    private String strToAscii(String hexstr) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < hexstr.length(); i+=2) {
            String str = hexstr.substring(i, i+2);

            output.append((char)Integer.parseInt(str, 16));

        }
        return output.toString();
    }

}
