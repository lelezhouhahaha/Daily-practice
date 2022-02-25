package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Message;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;

public class DMRActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private static final String TAG = "DMRActivity";

    private DMRActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.GPIO36_DMRSPK_INT)
    public TextView mGpio36DmrspkInt;
    @BindView(R.id.GPIO47_DMR_PTT)
    public TextView mGpio47DmrPtt;
    //@BindView(R.id.layout)
    //private LinearLayout mLayout;

    private String mFatherName = "";


    private String GPIO47_DMR_PTT_KEY = "common_gpio47_dmr_ptt_node";
    private String GPIO36_DMRSPK_INT_KEY = "common_gpio36_dmrspk_int_node";

    private String gpio47_dmr_ptt_node = "";//gpio47_dmr_ptt被设置成按键了
    private String gpio36_dmrspk_int_node = "/sys/devices/platform/soc/soc:meig-dmr-adc/meig-dmr-adc/dmr_ptt";

    private String gpio47_dmr_ptt_value = "";
    private String gpio36_dmrspk_int_value = "";

    private boolean gpio47_dmr_ptt_status = false;
    private String resultString;
    private int mConfigTime = 0;
    private Runnable mRun;
    private Thread mThread;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_dmr;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mFail.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.DMRActivity);
        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        if (mFatherName.equals(MyApplication.RuninTestNAME)){
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        }else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }

        String customGpio36DmrspkIntNode = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO36_DMRSPK_INT_KEY);
        if (customGpio36DmrspkIntNode != null && !customGpio36DmrspkIntNode.isEmpty()){
            gpio36_dmrspk_int_node = customGpio36DmrspkIntNode;
        }

        mHandle.sendEmptyMessageAtTime(1006, 100);
    }

    boolean writeToFile(final String path, final String value){
        try{
            FileOutputStream fGpioPtt = new FileOutputStream(path);
            fGpioPtt.write(value.getBytes());
            fGpioPtt.close();
        } catch (Exception e){
            e.printStackTrace();
            Log.d(TAG,"write to file "+path+"fail");
            return false;
        }
        return true;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1002:
                    deInit(mFatherName, FAILURE);
                    break;
                case 1003:
                    writeToFile(gpio36_dmrspk_int_node, "1");
                    gpio36_dmrspk_int_value = DataUtil.readLineFromFile(gpio36_dmrspk_int_node);
                    mHandle.sendEmptyMessageDelayed(1004,1500);
                    Log.d(TAG, "1003handleMessage: "+gpio36_dmrspk_int_value+" "+gpio47_dmr_ptt_value);
                    break;
                case 1004:
                    if (!gpio36_dmrspk_int_value.isEmpty())
                        mGpio36DmrspkInt.setText(Html.fromHtml(getResources().getString(R.string.gpio36_dmrspk_int_test)+
                                "&nbsp;" + "<font color='#FF0000'>" + gpio36_dmrspk_int_value + "</font>"));
                    mGpio47DmrPtt.setText(Html.fromHtml(getResources().getString(R.string.gpio47_dmr_ptt_test)+
                            "&nbsp;" + "<font color='#FF0000'>" + gpio47_dmr_ptt_value + "</font>"));
                    Log.d(TAG, "1004handleMessage: "+gpio36_dmrspk_int_value+" "+gpio47_dmr_ptt_value);
                    if (isSuccess()){
                        mSuccess.setVisibility(View.VISIBLE);
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        mHandle.sendEmptyMessageDelayed(1001, 1000);
                    } else {
                        mHandle.sendEmptyMessageDelayed(1002, 1000);
                    }
                    break;
                case 1005:
                    writeToFile(gpio36_dmrspk_int_node, "0");
                    gpio36_dmrspk_int_value = DataUtil.readLineFromFile(gpio36_dmrspk_int_node);
                    if (!gpio36_dmrspk_int_value.isEmpty())
                        mGpio36DmrspkInt.setText(Html.fromHtml(getResources().getString(R.string.gpio36_dmrspk_int_test)+
                                "&nbsp;" + "<font color='#FF0000'>" + gpio36_dmrspk_int_value + "</font>"));
                    mGpio47DmrPtt.setText(Html.fromHtml(getResources().getString(R.string.gpio47_dmr_ptt_test)+
                            "&nbsp;" + "<font color='#FF0000'>" + gpio47_dmr_ptt_value + "</font>"));
                    Log.d(TAG, "1005handleMessage: "+gpio36_dmrspk_int_value+" "+gpio47_dmr_ptt_value);
                    mHandle.sendEmptyMessageDelayed(1003, 1500);
                    break;
                case 1006:
                    gpio36_dmrspk_int_value = DataUtil.readLineFromFile(gpio36_dmrspk_int_node);
                    if (!gpio36_dmrspk_int_value.isEmpty())
                        mGpio36DmrspkInt.setText(Html.fromHtml(getResources().getString(R.string.gpio36_dmrspk_int_test)+
                                "&nbsp;" + "<font color='#FF0000'>" + gpio36_dmrspk_int_value + "</font>"));
                    mGpio47DmrPtt.setText(Html.fromHtml(getResources().getString(R.string.gpio47_dmr_ptt_test)+
                            "&nbsp;" + "<font color='#FF0000'>" + gpio47_dmr_ptt_value + "</font>"));
                    Log.d(TAG, "1006handleMessage: "+gpio36_dmrspk_int_value+" "+gpio47_dmr_ptt_value);
                    mHandle.sendEmptyMessageDelayed(1005, 100);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    boolean isSuccess(){
        if (gpio47_dmr_ptt_status){
            return true;
        }else {
            return false;
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mHandle.removeMessages(1001);
        mHandle.removeMessages(1002);
        mHandle.removeMessages(1003);
        mHandle.removeMessages(1004);
        mHandle.removeMessages(1005);
        mHandle.removeMessages(1006);
        mHandle.removeMessages(9999);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        int scanCode = event.getScanCode();
        Log.d(TAG, "onKeyDown: scanCode--->"+scanCode);
        Log.d(TAG,"event:"+event);
        if(scanCode == 549){
            gpio47_dmr_ptt_value = getResources().getString(R.string.normal);
            gpio47_dmr_ptt_status = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        int scanCode = event.getScanCode();
        Log.d(TAG, "onKeyUp: scanCode--->"+scanCode);
        if (scanCode == 549){
            gpio47_dmr_ptt_value = getResources().getString(R.string.normal);
            gpio47_dmr_ptt_status = true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View view) {
        if (view == mBack){
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (view == mSuccess){
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (view == mFail){
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(mFatherName, result, Const.RESULT_NOTEST);
        } else if (result == 1){
            deInit(mFatherName, result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(mFatherName, result);
        }
    }
}
