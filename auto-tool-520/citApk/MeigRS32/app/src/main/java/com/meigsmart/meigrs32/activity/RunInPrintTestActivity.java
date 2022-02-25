package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
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
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.InnerResultCallback;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;

public class RunInPrintTestActivity extends BaseActivity implements View.OnClickListener {
    private String TAG = "RunInPrintTestActivity";

    private RunInPrintTestActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.print_result)
    public TextView mResult;
    private String mFatherName = "";
    private SunmiPrinterService sunmiPrinterService;

    private int mConfigTime = 0;
    private Runnable mRun;
    private int count_print = 1;
    private PrintTask printTask = null;
    public static final byte GS =  0x1D;// Group separator
    byte[] rv = null;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_print_runin_test;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mSuccess.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTitle.setText(R.string.RunInPrintTestActivity);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        sunmiPrinterService = MyApplication.sunmiPrinterService;
        Log.d(TAG,"sunmiPrinterService0000:"+sunmiPrinterService);
        if (mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        SharedPreferences sharedPreferences = getSharedPreferences("print", Context.MODE_PRIVATE);
        count_print = sharedPreferences.getInt("count",1);
        if(count_print>1){
            count_print=count_print+1;
        }
        new PrintTask().execute();
        mResult.setText("当前为第" + count_print + "次打印......");

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                updateFloatView(mContext, mConfigTime);
                if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();
    }


    class PrintTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(count_print%2==0){
                mHandler.sendEmptyMessageDelayed(1001,2000);
            }
            try {
                if (!checkPrint()) {
                    return null;
                }
                rv=printBitmap(initBlackBlock(30,600));
                SimpleDateFormat formatter = new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());
                String current_time = formatter.format(curDate);
                String test_result="PASS";
                sunmiPrinterService.clearBuffer();
                sunmiPrinterService.printTextWithFont("第"+count_print+"次"+" "+current_time+" "+test_result, "", 23, null);
                sunmiPrinterService.sendRAWData(rv,null);
                sunmiPrinterService.exitPrinterBufferWithCallback(true, innerResultCallback);
                SharedPreferences sharedPreferences = getSharedPreferences("print", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("count", count_print);
                editor.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG,"count_print:"+count_print);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            count_print++;
            mResult.setText("当前为第" + count_print + "次打印......");
            mHandler.sendEmptyMessage(1006);
        }
    }

    public static byte[] initBlackBlock(int h, int w){
        int hh = h;
        int ww = (w - 1)/8 + 1;
        byte[] data = new byte[ hh * ww + 6];

        data[0] = (byte)ww;//xL
        data[1] = (byte)(ww >> 8);//xH
        data[2] = (byte)hh;
        data[3] = (byte)(hh >> 8);

        int k = 4;
        for(int i=0; i<hh; i++){
            for(int j=0; j<ww; j++){
                data[k++] = (byte)0xFF;
            }
        }
        data[k++] = 0x00;data[k++] = 0x00;
        return data;
    }

    public static byte[] printBitmap(byte[] bytes){
        byte[] bytes1  = new byte[4];
        bytes1[0] = GS;
        bytes1[1] = 0x76;
        bytes1[2] = 0x30;
        bytes1[3] = 0x00;

        return byteMerger(bytes1, bytes);
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
        mHandler.removeCallbacks(mRun);
        Log.d(TAG,"printTask:"+printTask);
        if (printTask != null && !printTask.isCancelled() && printTask.getStatus() == AsyncTask.Status.RUNNING) {
            printTask.cancel(true);
            printTask = null;
        }
        sunmiPrinterService=null;
        Log.d(TAG,"PrinterService onDestroy:"+sunmiPrinterService);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        //unbindPrintService();
        sunmiPrinterService=null;
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mHandler.removeMessages(1005);
                    mHandler.removeMessages(1006);
                    deInit(mFatherName, SUCCESS);
                    break;
                case 1005:
                    mHandler.removeMessages(1005);
                    mHandler.removeMessages(1006);
                    break;
                case 1006:
                    mHandler.sendEmptyMessage(1005);
                    Log.d(TAG,"PrinterService printTask:"+sunmiPrinterService);
                    if(sunmiPrinterService!=null) {
                        printTask = new PrintTask();
                        printTask.execute();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final InnerResultCallback innerResultCallback = new InnerResultCallback() {
        @Override
        public void onRunResult(boolean isSuccess) {
        }

        @Override
        public void onReturnString(String result) {
            Log.e("Printer_zhr", "result:" + result);
        }

        @Override
        public void onRaiseException(int code, String msg) {
            Log.e("Printer_zhr", "code:" + code + ",msg:" + msg);
        }

        @Override
        public void onPrintResult(int code, String msg) {
            Log.e("Printer_zhr", "code:" + code + ",msg:" + msg);
        }
    };
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
    private boolean checkPrint() {
        if (MyApplication.sunmiPrinterService == null) {
            return false;
        }
        return true;
    }

}
