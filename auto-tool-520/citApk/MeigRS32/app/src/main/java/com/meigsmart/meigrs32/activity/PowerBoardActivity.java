package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.PersistResultModel;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import butterknife.BindView;

public class PowerBoardActivity extends BaseActivity implements View.OnClickListener , CheckListAdapter.OnCallBackCheckFunction {
    private PowerBoardActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private CheckListAdapter mAdapter;
    private int currPosition = 0;
    @BindView(R.id.more)
    public LinearLayout mMore;
    private boolean isLayout = true;//true linearLayout  ;false gridLayout

    private String mDefaultPath;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mFileName ;

	private AlertDialog mAlertDialog = null;
    private boolean mStartFailTest = false;
    private int AUTO_TEST_FAIL_ITEM_COUNT = 2;
    private String  AUTO_TEST_FAIL_ITEM_COUNT_TAG = "common_auto_test_fail_item_count";
    List<Integer> mFailItems = new ArrayList<>();
    private int failTestPosition = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = true;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mMore.setOnClickListener(this);
        mMore.setSelected(isLayout);
        mTitle.setText(R.string.PowerBoardActivity);

        String failTestCount =  DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, AUTO_TEST_FAIL_ITEM_COUNT_TAG);
        if(failTestCount != null && !failTestCount.isEmpty()){
            try {
                AUTO_TEST_FAIL_ITEM_COUNT = Integer.parseInt(failTestCount);
            }catch(NumberFormatException e){
                LogUtil.e("PCBActivity","failTestCount="+failTestCount);
            }
        }

        mDefaultPath = getResources().getString(R.string.power_board_save_log_default_path);
        mFileName = getResources().getString(R.string.power_board_save_log_file_name);
        isCustomPath = getResources().getBoolean(R.bool.power_board_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.power_board_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");
        LogUtil.d("citapk super.mName:" + super.mName);
        super.mFatherName = getIntent().getStringExtra("fatherName");
        LogUtil.d("citapk super.mFatherName:" + super.mFatherName);

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
            LogUtil.d("citapk super.mList:" + super.mList);
        }

        List<String> config = Const.getXmlConfig(this,Const.CONFIG_POWER_BOARD);
        List<TypeModel> list = getDatas(mContext, config,super.mList);
        if (list.size()>10)mMore.setVisibility(View.VISIBLE);
        mAdapter.setData(list);
        //ToastUtil.showBottomLong(getResources().getString(R.string.start_tag));
        mHandler.sendEmptyMessageDelayed(1005,100);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1003://test finish

                    if(AUTO_TEST_FAIL_ITEM_COUNT > 0 && mFailItems.size() > 0) {
                        mStartFailTest = true;
                        mHandler.sendEmptyMessageDelayed(1006, 100);
                        return;
                    }

                    //ToastUtil.showBottomShort(getResources().getString(R.string.power_board_test_finish));
                    new SaveResult().execute();
                    //saveLog();
                    break;
                case 1005:
                    startActivity(mAdapter.getData().get(currPosition));
                    break;
                case 1006:
                    startActivity(mAdapter.getData().get(mFailItems.get(failTestPosition)));
                    break;
            }
        }
    };

    private void saveLog() {
        List<FunctionBean> list = getFatherData(super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        List<PersistResultModel> persistResultList = new ArrayList<>();

        boolean isAllSuccess = true;
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            PersistResultModel persistModel = new PersistResultModel();

            switch (bean.getResults()){
                case 0:
                    model.setResult(Const.RESULT_NOTEST);
                    persistModel.setResult(Const.RESULT_NOTEST);
                    isAllSuccess = false;
                    break;
                case 1:
                    model.setResult(Const.RESULT_FAILURE);
                    persistModel.setResult(Const.RESULT_FAILURE);
                    isAllSuccess = false;
                    break;
                case 2:
                    model.setResult(Const.RESULT_SUCCESS);
                    persistModel.setResult(Const.RESULT_SUCCESS);
                    break;
            }

            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());

            persistModel.setName(bean.getSubclassName());

            resultList.add(model);
            persistResultList.add(persistModel);
        }

        PersistResultModel allResultModel = new PersistResultModel();
        allResultModel.setName("pcba_all");
        allResultModel.setResult(isAllSuccess ? Const.RESULT_SUCCESS : Const.RESULT_FAILURE);
        persistResultList.add(allResultModel);

        writeSDCardResult(isCustomPath ? mCustomPath : mDefaultPath, mFileName,  JSON.toJSONString(resultList));

        writePersistResult(Const.getLogPath(Const.TYPE_LOG_PATH_FILE),  Const.POWER_BOARD_AUTO_RESULT_FILE, JSON.toJSONString(persistResultList));
    }

    private boolean writePersistResult(String path, String fileName, String result) {
        File persistPath = new File(Const.getLogPath(Const.TYPE_LOG_PATH_DIR));
        if(persistPath.exists() && persistPath.isDirectory()){
            File dir = FileUtil.mkDir(new File(path));
            return !"".equals(FileUtil.writeFile(dir, fileName, result));
        }
        return false;
    }

    private boolean writeSDCardResult(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)) {
            File dir = FileUtil.mkDir(FileUtil.createRootDirectory(path));
            return !"".equals(FileUtil.writeFile(dir, fileName,result));
        }
        return false;
    }

        private class SaveResult extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
           createDialog();
        }
    
        @Override
        protected Void doInBackground(Void... params) {
    
            try {
    
                saveLog();
    
                Thread.sleep(5000);
    
            } catch (Exception e) {
    
            }
    
            return null;   
    
        }
    
        @Override
        protected void onPostExecute(Void e) {
    
            LogUtil.d("SaveResult -- >onPostExecute");
            updateDialg();
    
        }
    
    }
    private void updateDialg(){
        String resultStr = "";
        //boolean resultStatus = isAllSuccess();
        //if(resultStatus)
        //    resultStr = getResources().getString(R.string.success);
        //else resultStr = getResources().getString(R.string.fail);
        mAlertDialog.setCancelable(true);
        ProgressBar mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.save_loading);
        TextView mMessageView = (TextView) mAlertDialog.findViewById(R.id.result_textView);
        TextView mSaveResult = (TextView) mAlertDialog.findViewById(R.id.save_result);
        mProgressBar.setVisibility(View.GONE);
        mMessageView.setVisibility(View.VISIBLE);
        mMessageView.setText(resultStr);
        mSaveResult.setText(R.string.save_finish);
        mSaveResult.setTextColor(Color.YELLOW);
        //if(resultStatus) {
        //    mMessageView.setTextColor(Color.GREEN);
        //}else{
        //    mMessageView.setTextColor(Color.RED);
        //}
    }
    private void createDialog(){
        String title = getResources().getString(R.string.PowerBoardActivity);
        View view = View.inflate(getApplicationContext(), R.layout.dialog_result, null);
        mAlertDialog = new AlertDialog.Builder(mContext)
                            .setTitle(title)
                            .setView(view)
                            .create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();

    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
        if (v == mMore){
            if (isLayout){
                isLayout = false;
                mRecyclerView.setLayoutManager(new GridLayoutManager(this,2));
            }else {
                isLayout = true;
                mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            mMore.setSelected(isLayout);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(int position) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1005);
        mHandler.removeMessages(1006);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111 || resultCode == 1000){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                LogUtil.d("test results:" + results);

                if(!mStartFailTest && results != SUCCESS ){
                    mFailItems.add(currPosition);
                }
                if(mStartFailTest){
                    mAdapter.getData().get(mFailItems.get(failTestPosition)).setType(results);
                    mAdapter.notifyDataSetChanged();
                    if(results == SUCCESS) {
                        mFailItems.remove(failTestPosition);
                    }else {
                        failTestPosition++;
                    }
                    if(mFailItems.size() <= failTestPosition ){
                        failTestPosition = 0;
                        AUTO_TEST_FAIL_ITEM_COUNT--;
                    }
                }else {
                    mAdapter.getData().get(currPosition).setType(results);
                    mAdapter.notifyDataSetChanged();
                    currPosition++;
                }

                //if ( currPosition == Const.pcbaList.length ){
                if(currPosition == mAdapter.getItemCount()){
                    LogUtil.d("currPosition :" + currPosition);
                    LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                        mHandler.sendEmptyMessageDelayed(1003,Const.DELAY_TIME);
                       // mHandler.sendEmptyMessageDelayed(1004,Const.DELAY_TIME);
                        return;
                }
                Message msg = mHandler.obtainMessage();
                msg.what = 1004;
                mHandler.sendMessage(msg);
                LogUtil.d("mAdapter.getItemCount():" + mAdapter.getItemCount());
                mHandler.sendEmptyMessageDelayed(1005,Const.DELAY_TIME);
            }
        }
    }
}
