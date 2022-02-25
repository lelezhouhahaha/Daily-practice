package com.meigsmart.meigrs32.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.util.DataUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;


public class Main_Default_Activity extends Activity implements View.OnClickListener {
    private boolean cit1_result = false;
    private Button cit_test1, cit_test2;
    public TextView mTitle;
    String mDefaultSystemLanguageKey = "common_cit_system_default_language";
    boolean mDefaultSystemLanguage = true;
    private boolean isMC509 = "MC509".equals(DataUtil.getDeviceName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String languageConfig = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mDefaultSystemLanguageKey);
        if(!languageConfig.isEmpty())
            mDefaultSystemLanguage = languageConfig.equals("true");
        else mDefaultSystemLanguage = true;

        if(!mDefaultSystemLanguage)
            setdefaultLanguage(this);
        if(!isMC509){
            Intent cit_test = new Intent(Main_Default_Activity.this, MainActivity.class);
            Main_Default_Activity.this.startActivity(cit_test);
            Main_Default_Activity.this.finish();
        }
        setContentView(R.layout.activity_main_default);
        cit_test1 = (Button) findViewById(R.id.cit1_test);
        cit_test2 = (Button) findViewById(R.id.cit2_test);
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText(R.string.select_title);
        if(checkCit1_Test()){
            cit_test1.setBackgroundColor(getResources().getColor(R.color.green_1));
        }else{
            cit_test1.setBackgroundColor(getResources().getColor(R.color.red_800));
        }
        cit_test2.setBackgroundColor(R.drawable.round_red);
        cit_test1.setOnClickListener(this);
        cit_test2.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cit1_test:
                SystemProperties.set("persist.sys.db_name_cit","cit1_test");
                Intent cit1_test = new Intent(Main_Default_Activity.this, MainActivity.class);
                Main_Default_Activity.this.startActivity(cit1_test);
                break;
            case R.id.cit2_test:
                Log.d("Meig_cit", "checkCit1_Test():" + checkCit1_Test());
                if (checkCit1_Test()) {
                    SystemProperties.set("persist.sys.db_name_cit","cit2_test");
                    Intent cit2_test = new Intent(Main_Default_Activity.this, MainActivity.class);
                    Main_Default_Activity.this.startActivity(cit2_test);
                } else {
                    Toast.makeText(Main_Default_Activity.this, R.string.please_cit1_title, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private boolean checkCit1_Test() {
        try {
            String jsonFilePath = "/storage/emulated/0/CITResults/pcba_auto_result.json";
            String input = readFileToString(jsonFilePath);
            JSONArray jsonArray = new JSONArray(input);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.getString("name").equals("pcba_all")) {
                    if (jsonObject.getString("result").equals("Success")) {
                        cit1_result = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Meig_cit", "e:"+e.toString());
            cit1_result = false;
        }
        return cit1_result;
    }

    public static String readFileToString(String Path) {
        BufferedReader reader = null;
        String laststr = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(Path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                laststr += tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return laststr;
    }

    private void setdefaultLanguage(Context context) {
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        config.locale = Locale.CHINESE;
        context.getResources().updateConfiguration(config, metrics);
    }
}

