package com.example.oemscandemo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

import com.hsm.barcode.*;
import com.hsm.barcode.DecoderException.ResultID;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.media.SoundPool;
import android.media.AudioManager;

public class MainActivity extends Activity{
	
	private static final String TAG = "OemScanDemo";
	private static final String OEM_SCAN_DEMO_VERSION = "$LastChangedRevision: 107 $";
	private Decoder m_Decoder = null;				// Decoder object
	private DecodeResult m_decResult = null;			// Result object

	private TextView m_DecodeResultsView;				// ResultsView object
	private static long decodeTime = 0;					// Time for decode

	private static int g_nDecodeTimeout = 30000;//10000; 		// Decode timeout 10 seconds
	private static int g_nDecodeTime = 0; 		// Decode timeout 10 seconds
					// Scan Key value // FIXME: make -1?
	public static final String PREFS_NAME = "MyPrefsFile"; 	// Preference file to store scan key

	public int g_nImageWidth = 0;			// Global image width
	public int g_nImageHeight = 0;			// Global image height

	public static MainActivity instance = null;	// For accessing MainActivity from another activity
	private String mSaveScanData = "";
	Button m_ScanButton;
	TextView testMode;

	private SoundPool soundpool;
	private static int	heightBeepId;
	private static int	middleBeepId;
	public String g_Scan_Start_Type = "";
	private static final int MSG_START_SCAN = 1000;
	private static final int MSG_FINISH = 1002;
	
	private void initSoundpool() {
		if (soundpool != null) {
			soundpool.release();
			}
		soundpool = new SoundPool(2, AudioManager.STREAM_NOTIFICATION, 0);
//		heightBeepId = soundpool.load("/etc/scan_buzzer.ogg", 1);
//		middleBeepId = soundpool.load("/etc/scan_new.ogg", 1);
		heightBeepId = soundpool.load(this,R.raw.scan_buzzer,1);
		middleBeepId = soundpool.load(this,R.raw.scan_new, 1);
		}

	private void playSound(int type) {
		Log.i(TAG, "PlaySound......");
		if (soundpool != null) {
			if (type == 1) {
				soundpool.play(middleBeepId, 1, 1, 0, 0, 1);
				} else {
					soundpool.play(heightBeepId, 1, 1, 0, 0, 1);
					}
			}
		}


	/**
	  * Application create?
	  * 
	  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		m_ScanButton = (Button) findViewById(R.id.buttonScan);
		testMode = (TextView)findViewById(R.id.textViewMode);
        // Create instance
        instance = this;
        m_decResult = new DecodeResult();
		m_Decoder = new Decoder();

		Intent mIntent = getIntent();
		String name = mIntent.getStringExtra("name");
		if(null != name) testMode.setText(name);

		String scanType = "ScanType:";
		scanType += readLineFromFile("/dev/sunmi/scan/scan_head_type");
		Log.d(TAG, "scanType:" + scanType);
		TextView textViewScanType = (TextView)findViewById(R.id.textViewScanType);
		textViewScanType.setText(scanType);
		g_Scan_Start_Type = getIntent().getStringExtra("ScanStartType");
		Log.d(TAG, "onCreate g_Scan_Start_Type:" + g_Scan_Start_Type);
		if(g_Scan_Start_Type != null && !g_Scan_Start_Type.isEmpty()) {
			Log.d(TAG, "onCreate g_Scan_Start_Type is not empty!");
			if ("auto".equals(g_Scan_Start_Type) || "pcbaautotest".equals(g_Scan_Start_Type)) {
				m_ScanButton.setVisibility(View.GONE);
			}
		}

		initSoundpool();
		new ScanTask(0).execute();
    }
	protected String readLineFromFile( String path ){
		String data = "";
		FileInputStream file = null;
		try{
			file = new FileInputStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(file));
			data = reader.readLine();
			if (file != null) {
				file.close();
				file = null;
			}
		}catch(Exception e){
			try {
				if (file != null) {
					file.close();
					file = null;
				}
			} catch (IOException io) {
				Log.e(TAG,"readLineFromFile operation fail");
			}
		}
		return data;
	}

	class ScanTask extends AsyncTask {
		private int type;

		ScanTask(int type){
			this.type = type;
		}

		@Override
		protected Object doInBackground(Object[] objects) {
			if(type == 0){
				connectDecoder();
			}else if(type == 1){
				start_scan();
			}else if(type == 2){
				disconnectDecoder();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			if(type == 0){
				if ("auto".equals(g_Scan_Start_Type) || "pcbaautotest".equals(g_Scan_Start_Type)) {
					new ScanTask(1).execute();
				}else {
					m_ScanButton.setVisibility(View.VISIBLE);
				}
			}else if(type == 1){
				//m_ScanButton.setEnabled(true);
				deInit();
			}else if(type == 2){
				finish();
			}
		}
	}

	private void start_scan(){

		try {
			Log.d(TAG, "start_scan start");
			m_Decoder.waitForDecodeTwo(g_nDecodeTimeout, m_decResult);	// wait for decode with results arg
			if (m_decResult.length>0){
				g_nDecodeTime = m_Decoder.getLastDecodeTime();
				DisplayDecodeResults();
				//break;
			}
			Log.d(TAG, "start_scan end");
		}catch (DecoderException e) {
			Log.d(TAG, "start_scan DecoderException");
			HandleDecoderException(e);

		}
		//deInit();
	}

	private void connectDecoder(){
		try
		{
			Log.d(TAG, "connectDecoderLibrary start");
			m_Decoder.connectDecoderLibrary();
			Log.d(TAG, "connectDecoderLibrary end");
		}
		catch(DecoderException e)
		{
			HandleDecoderException(e);

			// TODO: Disable features if we cannot connect
		}
	}


	private void disconnectDecoder(){
		try {
			Log.d(TAG, "disconnectDecoderLibrary start");
			m_Decoder.disconnectDecoderLibrary();
			Log.d(TAG, "disconnectDecoderLibrary end");
		} catch (DecoderException e) {
			HandleDecoderException(e);

			// TODO: Disable features if we cannot connect
		}
	}

    /** 
	  * Options menu inflate
	  * 
	  */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
    	//getMenuInflater().inflate(R.menu.settings, menu);
        return false;
    }

	/** 
     * Start scan 
     * 
     * */
	private void StartScanning(){
		new ScanTask(1).execute();
	}

    /** 
     * Called when the user clicks the Scan button 
     * 
     * */
	public void onClickScan(View view) {
		StartScanning();
		m_ScanButton.setEnabled(false);
	}
    /** 
	  * Called when application gets focus
	  * 
	  */
    @Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first

		Log. d(TAG, "onResume");

	}


    /** 
	  * Called when application loses focus
	  * 
	  */
	@Override
	public void onPause() {
	    super.onPause();  // Always call the superclass method first
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
	}

    /** 
	  * Displays the decoded results (note: called from thread)
	  * 
	  */
    private void DisplayDecodeResults() 
    {
    	runOnUiThread(new Runnable() {

            @Override
            public void run() {

            	m_DecodeResultsView = (TextView) findViewById(R.id.textViewDataResults);
            	
            	if (m_decResult.length > 0 )
            	{
            		
            		Log. d(TAG, "decode success!");
					if(m_decResult.barcodeData.length()!=0)
					{
						mSaveScanData = m_decResult.barcodeData;
						Log. d(TAG, "szf0728  m_decResult.barcodeData:" + m_decResult.barcodeData);
						m_DecodeResultsView.setText("Data : " + m_decResult.barcodeData +
							"\nLength: " + m_decResult.length +
							"\nAimID: " + String.format("]%c%c (0x%02x%02x)", m_decResult.aimId, m_decResult.aimModifier, m_decResult.aimId, m_decResult.aimModifier) +
							"\nCodeID: " + String.format("%c (0x%x)", m_decResult.codeId, m_decResult.codeId) +
							"\nTTR (ms): "+ g_nDecodeTime );
						 playSound(2);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
//						 Log. d(TAG, "TTR " + decodeTime + " ms [" + m_Decoder.getLastDecodeTime() + "ms]");
				} else {
					Log. d(TAG, "No Read!");
            		m_DecodeResultsView.setText("No Read");
            	}
            }
        });
    	
    }

	public void HandleDecoderException(final DecoderException e)
	{
		runOnUiThread(new Runnable() {

            @Override
            public void run() {
				Log. d(TAG, "HandleDecoderException++");

				if(true)
				{

					Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

					e.printStackTrace();
				}
				else // more user friendly?
				{
					switch(e.getErrorCode())
					{

						case ResultID.RESULT_ERR_NOTCONNECTED:
							Toast.makeText(getApplicationContext(), "Error: Engine not connected to perform this operation (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
							break;
						case ResultID.RESULT_ERR_NOIMAGE:
							Toast.makeText(getApplicationContext(), "Error: No image (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
							break;
						default:
							Toast.makeText(getApplicationContext(), "Unknown Error (" + e.getErrorCode() + ")", Toast.LENGTH_LONG).show();
					}
				}

				Log. d(TAG, "HandleDecoderException--");
        }
      });
	}

	protected void deInit(){
		Log. d(TAG, "deInit");
		Intent intent = new Intent();
		intent.putExtra("results",mSaveScanData);
		setResult(1111,intent);
		new ScanTask(2).execute();
	}


	@Override
	public void onBackPressed() {
		showBackDialog();
	}

	private void showBackDialog(){
		String message = getResources().getString(R.string.scaning_back);
		AlertDialog mAlertDialog = new AlertDialog.Builder(MainActivity.this)
				.setMessage(message)
				.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.create();
		mAlertDialog.show();
	}
}
