package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.meigsmart.meigrs32.model.TypeModel;

import butterknife.BindView;

public class GpioActivity extends BaseActivity implements View.OnClickListener {
    private GpioActivity mContext;
    private String TAG = "GpioTest";
    private Runnable mRun;
    private int mConfigTime = 0;
    private Handler mDelayHandler = null;
    private String mFatherName = "";
    private Runnable mDelayRunnable = null;
    private boolean  mIsExistFail =false;

    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;

    private static final String GPIO_PROP_KEY = "common_cit_gpio_test_prop";
    private String gpio_prop = "sys.gpio_test";
    private static final String GPIO_SUPPORT_PROP_KEY = "common_cit_gpio_support_prop";
    private String gpio_support_prop = "ro.boot.gpiotestflag";
    private String mIopartition = "/sys/iopartition/iopartition";
    private boolean mNeedWriteGpioFlag = false;
    private final String WRITE_IO_PARTITION_GPIO_PATH_KEY = "common_write_iopartition_gpio_path";
    private final String WRITE_IO_PARTITION_GPIO_FLAG_KEY = "common_write_iopartition_gpio_flag";
    private final String GPIO_TEST_RESULT_PATH_KEY = "common_cit_gpio_test_result_path";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gpio;
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
        mFail.setVisibility(View.VISIBLE);
        mTitle.setText(R.string.GpioIsTesting);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        String temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_PROP_KEY);
        if (!TextUtils.isEmpty(temp))
            gpio_prop = temp;
        temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_SUPPORT_PROP_KEY);
        if (!TextUtils.isEmpty(temp))
            gpio_support_prop = temp;
        addData(mFatherName, super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
         temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WRITE_IO_PARTITION_GPIO_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            mIopartition = temp;
        mNeedWriteGpioFlag = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WRITE_IO_PARTITION_GPIO_FLAG_KEY));
        temp = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, GPIO_TEST_RESULT_PATH_KEY);
        if (!TextUtils.isEmpty(temp))
            FILE_RESTULTS_INI = temp;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(2008);
                if(hasSimOrTCard())
                    mHandler.sendEmptyMessage(2002);
                while (hasSimOrTCard()) {
                    try {


                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
                mHandler.sendEmptyMessage(2003);
                try {


                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();

                }
                mHandler.sendEmptyMessage(2004);
            }
        }).start();


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

    private boolean hasSimOrTCard() {
        final int sim1state = TelephonyManager.getDefault().getSimState(0);
        final int sim2state = TelephonyManager.getDefault().getSimState(1);
        if(getSDCardMemory() != null||sim1state==TelephonyManager.SIM_STATE_READY||sim2state==TelephonyManager.SIM_STATE_READY) {
            return true;
        }
        return false;
    }
    private boolean hasSimCard() {
        final int sim1state = TelephonyManager.getDefault().getSimState(0);
        final int sim2state = TelephonyManager.getDefault().getSimState(1);
        if(sim1state==TelephonyManager.SIM_STATE_READY||sim2state==TelephonyManager.SIM_STATE_READY) {
            return true;
        }
        return false;
    }
    private boolean hasTCard() {
        if(getSDCardMemory() != null) {
            return true;
        }
        return false;
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
    private void startTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean en = ProcessShell();
                if(en == true){
                    mHandler.sendEmptyMessage(1002);
                    mHandler.sendEmptyMessage(1003);
                } else {
                    mHandler.sendEmptyMessage(2001);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDelayHandler != null) {
            mDelayHandler.removeCallbacks(mDelayRunnable);
            mDelayHandler = null;
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == mSuccess){
            if(mDelayHandler != null){
                mDelayHandler.removeCallbacks(mDelayRunnable);
                mDelayHandler = null;
            }
            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
            deInit(mFatherName, SUCCESS);
        }
        if (v == mFail){
            if(mDelayHandler != null){
                mDelayHandler.removeCallbacks(mDelayRunnable);
                mDelayHandler = null;
            }
            mFail.setBackgroundColor(getResources().getColor(R.color.red_800));
            deInit(mFatherName, FAILURE,Const.RESULT_UNKNOWN);
        }
    }
   public void onRestoreGpio(View v){
       LogUtil.d("===czq onRestoreGpio citapk :") ;
       mHandler.sendEmptyMessage(2005);
    }
    private boolean ProcessShell() {
        try {
            String lang= SystemProperties.get(gpio_support_prop);
            if (! "1".equals(lang)) {
                mHandler.sendEmptyMessage(2007);
                return false;
            }
            SystemProperties.set(gpio_prop,"0");
            SystemProperties.set(gpio_prop,"1");
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if(waitFileBlock(FILE_RESTULTS_INI) == false)
            return false;
        else
            return true;
    }

    private String FILE_RESTULTS_INI = "/data/data/com.meigsmart.meigrs32/gpio_test.ini";
    boolean writeToFile(final String path, final String value){
        try {
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.close();
        } catch (Exception e) {
            LogUtil.e(TAG, "write to file " + path + " abnormal.");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    static final class FileRunnable implements Runnable {
        private boolean result = false;
        public boolean getResult()
        {
            return this.result;
        }
        private String name;
        public void setName(String name)
        {
            this.name = name;
        }
        private int WaitTime;
        public void setWaitTime(int WaitTime)
        {
            this.WaitTime = WaitTime;
        }
        @Override
        public void run() {
            while(true)
            {
                try
                {
                    File f=new File(name);
                    if(!f.exists() || !f.canRead())
                    {
                        result = false;
                        WaitTime--;
                        Thread.sleep(1000);
                    }else{
                        result = true;
                        break;
                    }
                }
                catch (Exception e)
                {
                    result = false;
                }
            }
        }
    };
    private boolean waitFileBlock(String strFile)
    {
        FileRunnable tmp = new FileRunnable();
        tmp.setName(strFile);
        tmp.setWaitTime(30);
        tmp.run();
        return tmp.getResult();
    }
    private void createDialog(){
        LogUtil.e(TAG, "classname " + noSimCardTest()+ " abnormal.");
        LogUtil.e(TAG, "classnametcard " + noStorageCardTest()+ " abnormal.");
    if(noSimCardTest()&&noStorageCardTest())
        return;
        AlertDialog  mAlertDialog = new AlertDialog.Builder(mContext).create();
        if(noSimCardTest()) {
            mAlertDialog.setMessage(getResources().getString(R.string.gpio_plug_t_card));
        }else if(noStorageCardTest()) {
            mAlertDialog.setMessage(getResources().getString(R.string.gpio_plug_sim_card));
        } else {
            mAlertDialog.setMessage(getResources().getString(R.string.gpio_plug_sim_t_card));
        }
        mAlertDialog.show();

    }
   private boolean noSimCardTest() {
       List<String> config = Const.getXmlConfig(this,Const.CONFIG_PCBA);
           List<TypeModel> list = getDatas(mContext, config,super.mList);
           for (TypeModel m:list) {
               if(m.getCls().getName().toLowerCase().contains("sim")) {
                   return false;
               }
           }
           return true;
   }
    private boolean noStorageCardTest() {
        List<String> config = Const.getXmlConfig(this,Const.CONFIG_PCBA);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        for (TypeModel m:list) {
            if(m.getCls().getName().toLowerCase().contains("storage")) {
                return false;
            }
        }
        return true;
    }
    private HashMap getProcessResults() {
        LogUtil.w(TAG,"getProcessResults");
        HashMap sections = new HashMap();
        String secion;
        try {

            BufferedReader reader = new BufferedReader(new FileReader(FILE_RESTULTS_INI));
            String line;
            TableLayout mainLinerLayout = (TableLayout  ) this.findViewById(R.id.gpio_root_view);
            ArrayList<HashMap<String, Object>> meumList = new ArrayList<HashMap<String, Object>>();
            GridLayout layoytParent = null;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[") && line.endsWith("]")){
                    secion = line.replaceFirst("\\[(.*)\\]", "$1");
                    if(index>0) {
                        mainLinerLayout.addView(layoytParent);
                    }
                    layoytParent = new GridLayout(this);
                    TextView textview=new TextView(this);
                    textview.setText(secion);
                    textview.setId(index);
                    mainLinerLayout.addView(textview);
                    textview = null;
                    index = 0;
                } else if (line.matches(".*=.*") == true) {
                    if (layoytParent != null) {
                        int i = line.indexOf('=');
                        String name = line.substring(0, i);
                        String value = line.substring(i + 1);
                        Button codeBtn = null;
                        codeBtn = new Button( this );
                        codeBtn.setTextColor(Color.BLACK );
                        codeBtn.setTextSize( 10 );
                        codeBtn.setText( name );
                        codeBtn.setAllCaps(false);
                        codeBtn.setGravity( Gravity.CENTER);
                        codeBtn.setBackgroundColor( value.equals("true")?Color.GREEN:Color.RED );
                        if(!value.equals("true"))
                            mIsExistFail=true;
                        GridLayout.Spec rowSpec = GridLayout.spec(index/4);
                        GridLayout.Spec columeSpec = GridLayout.spec(index%4);
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec,columeSpec);
                        layoytParent.addView(codeBtn,params);
                        index++;
                    }
                }
            }
            if(layoytParent != null) {
                mainLinerLayout.addView(layoytParent);
                layoytParent = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sections;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    deInit(mFatherName, FAILURE, Const.RESULT_UNKNOWN);
                    break;
                case 1002:
                    getProcessResults();
                    mHandler.sendEmptyMessage(1003);
                    break;
                case 1003:
                    if(mIsExistFail)
                     {
                        mFail.setVisibility(View.VISIBLE);
                        mSuccess.setVisibility(View.GONE);
                     }
                    else
                    {
                        mFail.setVisibility(View.VISIBLE);
                        mSuccess.setVisibility(View.VISIBLE);
                        createDialog();
                     }
                    mTitle.setText("GPIO");
                    break;
                case 2001:
                    mFail.setVisibility(View.VISIBLE);
                    mSuccess.setVisibility(View.GONE);
                    mTitle.setText("GPIO");
                    break;
                case 2002:
                    if(hasSimCard()&&hasTCard()) {
                        mTitle.setText(getResources().getString(R.string.gpio_unplug_sim_t_card));
                    }else if(hasSimCard()) {
                        mTitle.setText(getResources().getString(R.string.gpio_unplug_sim_card));
                    } else {
                        mTitle.setText(getResources().getString(R.string.gpio_unplug_t_card));
                    }
                    break;
                case 2003:
                    mTitle.setText(R.string.GpioIsTesting);
                    break;
                case 2004:
                    startTest();
                    break;
                case 2005:
                    String sysGpioFlag ="sys.gpiotest.restore";
                    SystemProperties.set(sysGpioFlag, "1");
                    sendEmptyMessageDelayed(2006, 1000);
                    break;
                case 2006:
                        Intent reboot = new Intent(Intent.ACTION_REBOOT);
                        reboot.putExtra("nowait", 1);
                        reboot.putExtra("interval", 1);
                        reboot.putExtra("window", 0);
                        sendBroadcast(reboot);
                    break;
                case 2007:
                    TextView tv = (TextView) findViewById(R.id.test_result);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(R.string.software_not_support_gpio_test);
                    if(mNeedWriteGpioFlag){
                        Button rstgpio = (Button) findViewById(R.id.restore_gpio);
                        rstgpio.setVisibility(View.VISIBLE);
                    }
                    mFail.setVisibility(View.VISIBLE);
                    break;
                case 2008:
                    mFail.setVisibility(View.GONE);
                    break;
                case 9999:
                    deInit(mFatherName, FAILURE, msg.obj.toString());
                    break;
            }
        }
    };
}

