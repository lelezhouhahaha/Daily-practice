package com.test.client;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.mymodule.test.TestManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.test.lib.MyManager;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MainActivity extends Activity implements OnClickListener {
    MyManager myManager;
    AudioManager mAudioManager;
    TestManager mTestManager;
    Button btnSetValue;
    Button btnGetValue;
    TextView tvValue;
    public static final String TEST_SERVICE= "test";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setContentView(R.layout.activity_main);
        btnSetValue = (Button) findViewById(R.id.btn_set_value);
        btnSetValue.setOnClickListener(this);
        btnGetValue = (Button) findViewById(R.id.btn_get_value);
        btnGetValue.setOnClickListener(this);
        tvValue = (TextView) findViewById(R.id.tv_value);
        // 获取MyManager
        //myManager = MyManager.getInstance();
        mTestManager = (TestManager) getSystemService(TEST_SERVICE); //Context.TEST_SERVICE


    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_set_value:
                Log.d(TAG, "zll R.id.btn_set_value");
                mTestManager.testMethod();
                Log.d(TAG, "zll testMethod");
                int value = 100;/*new Random().nextInt();*/
                /*try {
                    //myManager.setValue(value);
                    Toast.makeText(this, "set value to "+value+ " success!", 0).show();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "set value fail!", 0).show();
                }*/
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Log.d(TAG, "zll sleep Exception");
                }
                Log.d(TAG, "zll get value");
               /* try {
                    //tvValue.setText("value:"+myManager.getValue());
                    //Log.d(TAG, "zll get value:" + myManager.getValue());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                    Log.d(TAG, "zll get value Exception");
                }*/
                break;
            case R.id.btn_get_value:
                Log.d(TAG, "zll R.id.btn_get_value");
                /*try {
                    //tvValue.setText("value:"+myManager.getValue());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                break;
            default:
                break;
        }
    }
}