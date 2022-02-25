package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.app.AppGlobals;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;

import static com.meigsmart.meigrs32.util.DataUtil.initConfig;
import static com.meigsmart.meigrs32.util.DataUtil.readLineFromFile;

public class UsbTypeCOtgActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private UsbTypeCOtgActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindView(R.id.success)
    public Button mSuccess;
    @BindView(R.id.fail)
    public Button mFail;
    @BindView(R.id.show)
    public TextView mShow;
	@BindView(R.id.mouse_area)
    public Button mMouseArea;
    private String mFatherName = "";
    private InputManager mInputManager;
    private MyHandler mHandler;
    private boolean mUsbOtgConfigUdisk;
    private String mUdiskTotalSpace= "";
    private String mUdiskUsedSpace="";
    private int mConfigTime = 0;
    private StorageManager mStorageManager;
    private String commonConfigUsbOtgMouse = "common_usb_otg_udisk_config_bool";
    //private boolean mAccessMouseFlag = false;
    //private boolean mGoOutMouseFlag = false;

    //private static final String Bot_connect_change_state_flag = "common_bot_connect_state_flag";
    //private static final String Bot_connect_state = "common_bot_connect_state_path";
    private static final String TYPEC_INSERT_DIRECTION_PATH = "common_UsbTypeCOtgActivity_typeC_insert_direction_path";
    private static final String TAG = UsbTypeCOtgActivity.class.getSimpleName();
    private static final int HANDLER_TYPEC_OTG = 1000;
    private boolean botflag = true;
    private String botstate = "";
    private boolean mUsbForwardPlug = false;
    private boolean mUsbForwardPlugAccessMouseFlag = false;
    private boolean mUsbForwardPlugGoOutMouseFlag = false;
    private boolean mUsbReversePlug = false;
    private boolean mUsbReversePlugAccessMouseFlag = false;
    private boolean mUsbReversePlugGoOutMouseFlag = false;
    private int mTypecInsertDirection = 0;
    private String mTypecInsertDirectionPath = "";
    private final String TAG_POGOPIN_OTG_TYPE_NODE = "dc_pogopin_otg_type_node";

    private boolean isUSBConnected(){
        boolean mRet = false;
        String type_node = initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_POGOPIN_OTG_TYPE_NODE);
        if ((type_node != null) && !type_node.isEmpty()) {
            String type = readLineFromFile(type_node);
            int mUsbTypeValue = Integer.valueOf(type);
            LogUtil.d(TAG, "type:" + type +  " mUsbTypeValue:" + mUsbTypeValue);
            if((mUsbTypeValue < 400000) || (mUsbTypeValue > 600000))
                mRet = true;
        }
        LogUtil.d(TAG, "mRet:" + mRet);
        return mRet;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_usb_otg;
    }

    @Override
    protected void initData() {
        mContext = this;
        mHandler = new MyHandler(mContext);
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.GONE);
        mBack.setOnClickListener(this);
//        mTitle.setText(R.string.pcba_type_c_usb_otg);
        mSuccess.setOnClickListener(this);
        mFail.setOnClickListener(this);
        mTypecInsertDirectionPath = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TYPEC_INSERT_DIRECTION_PATH);
        getInsertMode();
        Log.d(TAG, "onHover 1 mTypecInsertDirectionPath:" + mTypecInsertDirectionPath);
        Log.d(TAG, "onHover 1 mTypecInsertDirection:" + mTypecInsertDirection);

        mMouseArea.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int what = event.getAction();
                Log.d(TAG, "onHover what:" + what);
                if (!isUSBConnected()) {
                    return false;
                }
                switch(what){
                    case MotionEvent.ACTION_HOVER_ENTER:
                        Log.d(TAG, " bottom ACTION_HOVER_ENTER");
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        if(mTypecInsertDirection == 1) {
                            mUsbForwardPlug = true;
                            mUsbForwardPlugAccessMouseFlag = true;
                        }else if(mTypecInsertDirection == 2){
                            mUsbReversePlug = true;
                            mUsbReversePlugAccessMouseFlag = true;
                        }
                        Log.d(TAG, " bottom ACTION_HOVER_MOVE mTypecInsertDirection:" + mTypecInsertDirection);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        if(mTypecInsertDirection == 1) {
                            mUsbForwardPlug = true;
                            mUsbForwardPlugGoOutMouseFlag = true;
                        }else if(mTypecInsertDirection == 2){
                            mUsbReversePlug = true;
                            mUsbReversePlugGoOutMouseFlag = true;
                        }
                        Log.d(TAG, " bottom ACTION_HOVER_EXIT mTypecInsertDirection:" + mTypecInsertDirection);
                        break;
                }
                if((mUsbForwardPlug && mUsbForwardPlugGoOutMouseFlag && mUsbForwardPlugAccessMouseFlag) && (mUsbReversePlug && mUsbReversePlugAccessMouseFlag && mUsbReversePlugGoOutMouseFlag) && !mUsbOtgConfigUdisk) {
                    mSuccess.setVisibility(View.VISIBLE);
                    if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                        mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                        deInit(mFatherName, SUCCESS);//auto pass pcba
                    }
                }
                return false;
            }
        });

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        mTitle.setText(super.mName);
        if(mFatherName.equals(MyApplication.RuninTestNAME)) {
            mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
        } else {
            mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        }
        mStorageManager = mContext.getSystemService(StorageManager.class);

        String tmpStr = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, commonConfigUsbOtgMouse);
        Log.d(TAG, "tmpStr:" + tmpStr);
        if(tmpStr.length() != 0) {
            mUsbOtgConfigUdisk = tmpStr.equals("true");
        }else {
            mUsbOtgConfigUdisk = getResources().getBoolean(R.bool.usb_otg_default_config_udisk);
        }
        Log.d(TAG, "mUsbOtgConfigUdisk:" + mUsbOtgConfigUdisk);

        if( mUsbOtgConfigUdisk ) {
            mMouseArea.setVisibility(View.INVISIBLE);
            mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
        }else{
            updateTipsInfo();
        }
        if (isUSBConnected()) {
            getDiskInfo();
        }
        mHandler.sendEmptyMessage(HANDLER_TYPEC_OTG);
    }

     private static class MyHandler extends Handler {
         WeakReference<Activity> reference;
         public MyHandler(Activity activity) {
             reference = new WeakReference<>(activity);
         }

         @Override
         public void handleMessage(Message msg) {
             super.handleMessage(msg);
             UsbTypeCOtgActivity activity = (UsbTypeCOtgActivity) reference.get();
             switch (msg.what) {
                 case HANDLER_TYPEC_OTG:
                     activity.updateTipsInfo();
                     activity.mHandler.sendEmptyMessageDelayed(activity.HANDLER_TYPEC_OTG, 1000);
                     break;
             }
         }
     }

    private void getInsertMode(){
        if(!mTypecInsertDirectionPath.isEmpty()){
            mTypecInsertDirection = Integer.parseInt(FileUtil.replaceBlank(FileUtil.readFile(mTypecInsertDirectionPath)));
        }

        if(mTypecInsertDirection == 1) {
            mUsbForwardPlug = true;
        }else if(mTypecInsertDirection == 2){
            mUsbReversePlug = true;
        }
    }

    private void updateTipsInfo(){
        String valueStr =  "";
        String valueMouseStr =  "";

        getInsertMode();
        if(mTypecInsertDirection == 1) {
            valueStr = String.format(getResources().getString(R.string.mouseAlreadyForwardReminder), getResources().getString(R.string.mouseArea));
            if(mUsbForwardPlugAccessMouseFlag && mUsbForwardPlugGoOutMouseFlag){
                valueStr = String.format(getResources().getString(R.string.mouseReverseReminder), getResources().getString(R.string.mouseArea));
            }
            mShow.setText(valueStr);
        }else if(mTypecInsertDirection == 2){
            valueStr = String.format(getResources().getString(R.string.mouseAlreadyReverseReminder), getResources().getString(R.string.mouseArea));
            if(mUsbReversePlugAccessMouseFlag && mUsbReversePlugGoOutMouseFlag) {
                valueStr = String.format(getResources().getString(R.string.mouseForwardReminder), getResources().getString(R.string.mouseArea));
            }
            mShow.setText(valueStr);
        }else{
            if((mUsbReversePlugAccessMouseFlag && mUsbReversePlugGoOutMouseFlag && mUsbForwardPlugAccessMouseFlag && mUsbForwardPlugGoOutMouseFlag)||
                    (!mUsbReversePlugAccessMouseFlag && !mUsbReversePlugGoOutMouseFlag && !mUsbForwardPlugAccessMouseFlag && !mUsbForwardPlugGoOutMouseFlag)) {
                valueStr = String.format(getResources().getString(R.string.mouseForwardReminder), getResources().getString(R.string.mouseArea));
                mShow.setText(valueStr);
            }
        }
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            Log.d(TAG, "onVolumeStateChanged");
            if (!isUSBConnected()) {
                return;
            }
            getDiskInfo();
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            Log.d(TAG, "onDiskDestroyed");
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    private void getDiskInfo(){
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
       long totalBytes = 0;
        Context context = AppGlobals.getInitialApplication();
		
		for (VolumeInfo volume : volumes) {
			if (volume.getType() == VolumeInfo.TYPE_PUBLIC){
				 if ((volume.isMountedReadable())&&(!volume.disk.isSd())) {
					File path = volume.getPath();
					if (totalBytes <= 0) {
						totalBytes = path.getTotalSpace();
					}
					long freeBytes = path.getFreeSpace();
					long usedBytes = totalBytes - freeBytes;

                    mUdiskUsedSpace = Formatter.formatFileSize(context, usedBytes);
					mUdiskTotalSpace = Formatter.formatFileSize(context, totalBytes);
                    mShow.setText(getResources().getString(R.string.uDiskUsedSpaceSize) + mUdiskUsedSpace + "\n\n" + getResources().getString(R.string.uDiskTotalSpaceSize) +mUdiskTotalSpace);
					if(mUdiskTotalSpace.length() != 0 || mUdiskUsedSpace.length() != 0){
                        /*botflag = changestateflag();
                        if (botflag) {
                            mSuccess.setVisibility(View.VISIBLE);
                            if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                                mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                                deInit(mFatherName, SUCCESS);//auto pass pcba
                            }
                        }*/
                        mSuccess.setVisibility(View.VISIBLE);
                        if (mFatherName.equals(MyApplication.PCBANAME) || mFatherName.equals(MyApplication.PreNAME)) {
                            mSuccess.setBackgroundColor(getResources().getColor(R.color.green_1));
                            deInit(mFatherName, SUCCESS);//auto pass pcba
                        }
                    }
                    break;
				}
			}
		}
        mUdiskUsedSpace = "";
        mUdiskTotalSpace = "";
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
    public void onDestroy(){
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
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

    /*private String getCurrentnode(String node) {
        String currentNow = "";
        try {
            char[] buffer = new char[1024];

            FileReader fileReader = new FileReader(node);
            int len = fileReader.read(buffer, 0, buffer.length);
            String data = new String(buffer, 0, len);
            currentNow = data;

            fileReader.close();
        } catch (Exception e) {
            Log.e(TAG, "Get current now node : " + node + "fail.");
            Log.e(TAG, "e1111 : "+e.toString());
        }
        return currentNow;
    }
    private boolean isNeedCheckUsbType()
    {
        final String flag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common__connect_usb_type_flag");
        Log.d(TAG, "isNeedCheckUsbType flag:" + flag);
        if(flag.equals("true"))
            return true;
        else
            return false;
        
    }
    private boolean isUsbTypeC()
    {
        final String path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, "common_connect_usb_type_path");
        final String type = getCurrentnode(path);
        Log.d(TAG, "isUsbTypeC type:" + type);
        if(type.contains("Sink attached"))
            return true;
        else 
            return false;
    }
    private boolean changestateflag(){
        boolean flag = true;
        String bot_changeflag = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_change_state_flag);
        boolean change_flag = bot_changeflag.equals("true");
        botstate = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Bot_connect_state);
        String botstate_now = getCurrentnode(botstate);
        if(botstate_now.contains("1") && change_flag){
            flag = false;
        }
        return flag;
    }*/
}
