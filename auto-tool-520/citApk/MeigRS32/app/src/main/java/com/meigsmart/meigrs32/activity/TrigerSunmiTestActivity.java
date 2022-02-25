package com.meigsmart.meigrs32.activity;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


public class TrigerSunmiTestActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack{
    private TrigerSunmiTestActivity mContext;
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
    @BindView(R.id.log_clear)
    public TextView clearlog;
    @BindView(R.id.log_clear_btn)
    public Button clearlogbtn;

    public static final int TRIGER_TEST_SUSSCESS = 1001;
    public static final int TRIGER_TEST_Fail = 1004;
    public static final int TEST_SUCCESS = 1002;
    public static final int TEST_FAIL = 1003;
    public static final int TRIGER_TEST_LOG = 1005;

    List<String> tamper = new ArrayList<>();
    List<String> tamper_new = new ArrayList<>();
    private String log;
    private String log_new;
    private long DYNAMIC1 =  0x000001;
    private long DYNAMIC2 =  0x000002;
    private long STAIC1 =  0x001004;
    private long STAIC2 =  0x002004;
    private long STAIC3 =  0x004008;
    private long STAIC4 =  0x008008;
    private String TAG_mt535 = "common_keyboard_test_bool_config";
    boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");

    @Override
    protected int getLayoutId() {
        return R.layout.activity_triger535;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.TrigerSunmiTestActivity);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        if (!MyApplication.getInstance().isConnectPaySDK()) {
            MyApplication.getInstance().bindPaySDKService();
            ToastUtil.showCenterLong(getString(R.string.connect_loading));
            return;
        }
        mHandler.sendEmptyMessageDelayed(TRIGER_TEST_LOG,300);

    }

    private void getTamperLog() {
        try {
            log = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.TAMPER_LOG);
            LogUtil.e("TrigerSunmiTestActivity", "get tamper log:" + log);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static String convert(String str) {
        StringBuilder builder = new StringBuilder(str);
        str = builder.reverse().toString();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (i % 32 == 0) {
                //防越界&保留最高位
                if (i + 32 > str.length()) {
                    stringBuilder.append(str.substring(i));
                    break;
                }
                stringBuilder.append(str.substring(i, i + 32) + ",");
            }
        }
        str = stringBuilder.reverse().toString();
        //消除字符串长度为3的倍数时多出的','
        if (str.charAt(0) == ',') {
            str = str.substring(1);
        }
        return str;
    }

    public void SplitString(String ppp) {
        String[] strs = ppp.split(",");
        String resultInfo = "";
        for (int j = 0; j < ppp.length() / 32 - 1; j++) {
            tamper.add(j, strs[j]);
   //         LogUtil.d("TrigerSunmiTestActivity", "j:" + j);
   //         LogUtil.d("TrigerSunmiTestActivity", "userNames:" + tamper.get(j));
            tamper_new.add(j,tamper.get(j).substring(22,30));
  //          Long.parseLong(tamper_new.get(j));
            if(j == 0){
                continue;
            }
  //          LogUtil.d("TrigerSunmiTestActivity", "tamper_new:" + tamper_new.get(j));
            if(DYNAMIC1 == (Long.parseLong(tamper_new.get(j),16) & DYNAMIC1)){
                mTestColor1.setVisibility(View.VISIBLE);
                mTestColor1.setText(getString(R.string.status_dyanmic1));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            if(DYNAMIC2 == (Long.parseLong(tamper_new.get(j),16) & DYNAMIC2)){
                mTestColor2.setVisibility(View.VISIBLE);
                mTestColor2.setText(getString(R.string.status_dyanmic2));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            if(STAIC1 == (Long.parseLong(tamper_new.get(j),16) & STAIC1)){
                mTestColor3.setVisibility(View.VISIBLE);
                mTestColor3.setText(getString(R.string.status_static1));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            if(STAIC2 == (Long.parseLong(tamper_new.get(j),16) & STAIC2)){
                mTestColor4.setVisibility(View.VISIBLE);
                mTestColor4.setText(getString(R.string.status_static2));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            if(STAIC3 == (Long.parseLong(tamper_new.get(j),16) & STAIC3)){
                mTestColor5.setVisibility(View.VISIBLE);
                mTestColor5.setText(getString(R.string.status_static3));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            if(STAIC4 == (Long.parseLong(tamper_new.get(j),16) & STAIC4)){
                mTestColor6.setVisibility(View.VISIBLE);
                mTestColor6.setText(getString(R.string.status_static4));
                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
            }
            mTestColor1.setVisibility(View.VISIBLE);
            mTestColor1.setText(getString(R.string.status_rootkey_error));
            mHandler.sendEmptyMessageDelayed(TRIGER_TEST_Fail,300);
        }
    }

    private void setTextInvisible(){
        mTestColor1.setVisibility(View.GONE);
        mTestColor2.setVisibility(View.GONE);
        mTestColor3.setVisibility(View.GONE);
        mTestColor4.setVisibility(View.GONE);
        mTestColor5.setVisibility(View.GONE);
        mTestColor6.setVisibility(View.GONE);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case TRIGER_TEST_LOG:
                    getTamperLog();
                    if(log == null || log.isEmpty() || log.length()<= 32){
                        setTextInvisible();
                        mTestColor1.setVisibility(View.VISIBLE);
                        mTestColor1.setText(getString(R.string.status_success));
                        mHandler.sendEmptyMessageDelayed(TRIGER_TEST_SUSSCESS,300);
                    }else{
                        setTextInvisible();
                        log_new = convert(log);
                        SplitString(log_new);
                    }
                    break;
                case TRIGER_TEST_SUSSCESS:
                    String text = "";
                    text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.success);
                    mContentShow.setText(text);
                    mTrigertest.setBackgroundColor(Color.GREEN);
                    mSuccess.setVisibility(View.VISIBLE);
                    if(is_mt535){
                        mSuccess.performClick();
                    }
                    break;
                case TRIGER_TEST_Fail:
                    text = getString(R.string.TrigerTestActivity) + ": "+getString(R.string.fail);
                    mContentShow.setText(text);
                    mTrigertest.setBackgroundColor(Color.RED);
                    if(SystemProperties.get("ro.build.type").equals("userdebug")){
                        clearlog.setVisibility(View.VISIBLE);
                        clearlogbtn.setVisibility(View.VISIBLE);
                        clearlogbtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setClearlog();
                                clearlog.setVisibility(View.INVISIBLE);
                                clearlogbtn.setVisibility(View.INVISIBLE);
                                mHandler.sendEmptyMessageDelayed(TRIGER_TEST_LOG,300);
                            }
                        });
                    }
                    break;
                case TEST_SUCCESS:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                    deInit(mFatherName, SUCCESS);
                    break;
                case TEST_FAIL:
                    mSuccess.setBackgroundColor(getResources().getColor(R.color.red_800));
                    deInit(mFatherName, FAILURE);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE,msg.obj.toString());
                    break;
            }
        }
    };



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


}
