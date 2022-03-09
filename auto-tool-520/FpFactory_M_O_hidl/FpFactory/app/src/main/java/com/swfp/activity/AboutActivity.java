package com.swfp.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.swfp.device.MessageCallBack;
import com.swfp.factory.R;

/**
 * Created by zhouj on 2017/4/6.
 */

public class AboutActivity extends BaseActivity {
	private static final String TAG = "sw-AboutActivity";
	
    private LinearLayout mContainer;
    private LayoutParams mLayoutParams;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setTitle(R.string.title_about);

        mContainer = (LinearLayout) findViewById(R.id.container);

        mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        if (isConnected) {
            mHandler.sendEmptyMessage(10086);
        }
    }

    @Override
    protected MessageCallBack getMessageCallBack() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String separator = System.getProperty("line.separator");

            String libVer = manager.getVersionInfo();
            String appVer = getResources().getString(R.string.app_ver) + getPackVersion();
            String sdklibVer = getResources().getString(R.string.sdklib_ver) + com.swfp.device.BuildConfig.VERSION_NAME;

            final StringBuilder sb = new StringBuilder();
            sb.append(appVer + separator +sdklibVer + separator + libVer);

            String[] ss = sb.toString().split(separator);

            TextView tv = null;
            mContainer.removeAllViews();
            for (String s : ss) {
                System.out.println(s);

                tv = new TextView(AboutActivity.this);
                tv.setLayoutParams(mLayoutParams);
                tv.setText(getBuilder(s.trim()));
                mContainer.addView(tv);
            }
        }
    };

    private SpannableStringBuilder getBuilder(String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        if (text != null && text.contains(":")) {
            ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)), text.indexOf(":") + 1,
                    text.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        return ssb;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem itemTestNTX = menu.findItem(R.id.action_ntx_test);
        if(!getResources().getBoolean(R.bool.is_show_ntx_pager)){
            itemTestNTX.setVisible(false);
        }
        MenuItem itemTestSensitivity = menu.findItem(R.id.action_sensitivity_test);
        if(!getResources().getBoolean(R.bool.is_show_sensitivity_pager)){
            itemTestSensitivity.setVisible(false);
        }
        MenuItem itemTouchCali = menu.findItem(R.id.action_touch_calibration);
        if(!getResources().getBoolean(R.bool.is_show_touch_cali_pager)){
            itemTouchCali.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_score) {//指纹打分
            Intent intent = new Intent(AboutActivity.this, ScoreActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_pixel) {//坏点信息
            Intent intent = new Intent(AboutActivity.this, PixelTestActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_kvalue) {//校正信息
            Intent intent = new Intent(AboutActivity.this, KValueActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_detect) {//测试项
            Intent intent = new Intent(AboutActivity.this, DetectActivity.class);
            intent.setClassName("com.swfp.factory","com.swfp.activity.DetectActivity");
            intent.putExtra("is_override_xml", true);
            intent.putExtra("result_by_broadcast", false);
            intent.putExtra("press_finger_delay", 5000);
            startActivityForResult(intent,1);
            return true;
        } else if (item.getItemId() == R.id.action_ntx_test) {//ntx测试
            Intent intent = new Intent(AboutActivity.this, NTXTestActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_sensitivity_test) {//灵敏度测试
            Intent intent = new Intent(AboutActivity.this, SensitivityTestActivity.class);
            startActivity(intent);
            return true;
        }else if (item.getItemId() == R.id.action_touch_calibration) {//触摸变化量
            Intent intent = new Intent(AboutActivity.this, TouchCaliActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getPackVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return getResources().getString(R.string.unknow_version);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data!=null){
            int timeout = data.getIntExtra("timeout",1);
            int value = data.getIntExtra("value",1);
            Log.e(TAG,"timeout "+timeout+" value "+value +"resultCode: "+resultCode);
        }
    }
}
