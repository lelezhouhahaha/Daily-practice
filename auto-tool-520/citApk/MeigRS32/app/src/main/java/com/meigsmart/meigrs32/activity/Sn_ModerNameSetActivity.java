package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import com.meigsmart.meigrs32.util.OdmCustomedProp;

public class Sn_ModerNameSetActivity extends Activity {
    private static final String TAG = "Sn_ModerNameSetActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String version_new = intent.getStringExtra("persist_version");
        Log.d(TAG, "set version_new:" + version_new);
        String sn_new = intent.getStringExtra("persist_sn");
        Log.d(TAG, "set sn_new:" + sn_new);
        if (version_new != null) {
            //SystemProperties.set("persist.custmized.model_name", version_new);
            SystemProperties.set(OdmCustomedProp.getModelNameProp(), version_new);
        }
        if (sn_new != null) {
            //SystemProperties.set("persist.custmized.sn", sn_new);
            SystemProperties.set(OdmCustomedProp.getSnProp(), sn_new);
        }
        finish();
    }
}
