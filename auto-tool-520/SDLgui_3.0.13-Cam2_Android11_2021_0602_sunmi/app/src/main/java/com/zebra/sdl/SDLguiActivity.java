//-----------------------------------------------------------
// Android SDL Sample App
//
// Copyright (c) 2015 Zebra Technologies
//-----------------------------------------------------------

package com.zebra.sdl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.zebra.sdl.R;
import com.zebra.adc.decoder.BarCodeReader;
import com.zebra.util.SessionTimer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import org.xmlpull.v1.XmlPullParser;

public class SDLguiActivity extends Activity implements
		BarCodeReader.DecodeCallback, BarCodeReader.PictureCallback, BarCodeReader.PreviewCallback,
		SurfaceHolder.Callback, BarCodeReader.VideoCallback
{
	// ------------------------------------------------------
	static final private boolean 	saveSnapshot = false; // true = save snapshot to file
	static private boolean 			sigcapImage = true; // true = display signature capture
	static private boolean 			videoCapDisplayStarted = true;
	static private boolean 			previewCallbackWithBufferCalled = false;
	static private int 				ImgCount = 0;
	static private boolean  		se2100_test_platform = false;
	static private boolean 			turboMode = false;
	static private boolean 			genPreview 		= false;	// true = display generic Android-Camera preview frames
	static private boolean 			oneShotPreview 	= false;	// true = do 1-shot preview, false = use pre-alloc buffer(s)

	//states
	static final int STATE_IDLE 		= 0;
	static final int STATE_DECODE 		= 1;
	static final int STATE_HANDSFREE	= 2;	
	static final int STATE_PREVIEW		= 3;	//snapshot preview mode
	static final int STATE_SNAPSHOT		= 4;
	static final int STATE_VIDEO 		= 5;
	static final int STATE_GEN_PREVIEW	= 6;	//generic (non-SDL) preview mode
	static final int STATE_GEN_SURFACE	= 7;	//Surface Created
	
	// -----------------------------------------------------
	// statics
	static SDLguiActivity app = null;

	// -----------------------------------------------------
	// ui
	private TextView testMode = null;
	private TextView tvStat = null;
	private TextView tvData = null;
	private EditText edPnum = null;
	private EditText edPval = null;
	private CheckBox chBeep = null;
	private CheckBox chBeep_RP 				= null;

	private ImageView image 					= null;	//snaphot image screen
	
	private SurfaceView surfaceView 			= null;	//video screen
	private SurfaceHolder surfaceHolder 	= null;
	private LayoutInflater controlInflater	= null;
	
	// system
	private ToneGenerator tg 		= null;

	// BarCodeReader specifics
	private BarCodeReader bcr 		= null;

	private boolean beepMode 		= true; 		// decode beep enable
	private int Mobile_reading_pane	= 716; 		// Mobile Phone reading Pane
	private int reading_pane_value  = 1; 
	private boolean snapPreview 	= false;		// snapshot preview mode enabled - true - calls viewfinder which gets handled by 
	private int trigMode			= BarCodeReader.ParamVal.LEVEL;
	private boolean atMain			= false;
	private int state				= STATE_IDLE;
	private int decodes 			= 0;
	private boolean paul_stress_test = false;
	private int decode_fail_count 	= 0;
	private int motionEvents 		= 0;
	private int modechgEvents 	= 0;
	private int cameraId = 2;

	private int snapNum				= 0;		//saved snapshot #
	private String decodeDataString;
	private String decodeStatString;
	private static int decCount = 0;
	private String multiDecodedData = null;
	
	private Toast		toast					= null;
	
	private ByteArrayOutputStream os 	    = null;
	private byte[]	previewBuf			    = null;

	//specify application in demo  mode or test mode
	private APP_MODE application_mode = APP_MODE.DEMO_MODE;

	//menu-specific
	private int menu_selection = 0;

	//test mode -specific
	private LinearLayout layoutTestMode = null;
	private TextView tvCurrentCount = null;
	private TextView tvSuccessCount = null;
	private TextView tvScanPerMin = null;
	private TextView tvFirstScan = null;
	private TextView tvMinScan = null;
	private TextView tvMaxScan = null;
	private TextView tvAvgScan = null;
	private MediaPlayer player = null;

	private static final int START_DECODE 	= 1001;
	private static final int STOP_DECODE 	= 1002;

	private boolean bInDecodeMode = false;
	private static final String TAG = "SDLgui";
	public String g_Scan_Start_Type = "";
	private static final int MSG_STOP_SCAN = 1000;
	private String mSaveScanData = "";
	private long startTime = 0,endTime = 0;
	private ScanReleaseTask mScanReleaseTask = null;
	private boolean isDeinit = false;

	private enum APP_MODE
	{
		DEMO_MODE, TEST_MODE;
	}

	static
	{
		System.loadLibrary("IAL");
		System.loadLibrary("SDL");
		
		if(android.os.Build.VERSION.SDK_INT >= 26)
			System.loadLibrary("barcodereaderCam2"); // Android 8.0
		else if(android.os.Build.VERSION.SDK_INT >= 24)
			System.loadLibrary("barcodereader70"); // Android 7.0
		else if(android.os.Build.VERSION.SDK_INT >= 19)
			System.loadLibrary("barcodereader44"); // Android 4.4
		else if(android.os.Build.VERSION.SDK_INT >= 18)
			System.loadLibrary("barcodereader43"); // Android 4.3
		else
			System.loadLibrary("barcodereader");   // Android 2.3 - Android 4.2
				
			
		//System.loadLibrary("barcodereader");   // Android 2.3 - Android 4.2
	}


	Handler testModeHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Log.i("SDL-gui", "paul handleMessage: " + msg.what);
			switch (msg.what)
			{
				case START_DECODE:
					startTime = System.currentTimeMillis();
					if (bcr != null )
					    bcr.startDecode();
					break;
				case STOP_DECODE:
					testModeHandler.removeMessages(START_DECODE);
					if ( bcr != null )
					    bcr.stopDecode();
					break;
			}
		}
	};
	// ------------------------------------------------------
	public SDLguiActivity()
	{
		app = this;
	}

	// ------------------------------------------------------
	// Called with the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mainScreen();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// sound
		//tg = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
		chBeep.setChecked(beepMode);

		final AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.beep3);
		final FileDescriptor fileDescriptor = afd.getFileDescriptor();
		player = new MediaPlayer();
		try {
			player.setDataSource(fileDescriptor, afd.getStartOffset(),
					afd.getLength());
			player.setLooping(false);
			player.prepare();
		} catch (IOException ex) {
			Log.e("SDC",ex.getLocalizedMessage());
		}

		testMode = (TextView)findViewById(R.id.testMode);

		Intent mIntent = getIntent();
		String name = mIntent.getStringExtra("name");
		if(null != name) testMode.setText(name);

		g_Scan_Start_Type = getIntent().getStringExtra("ScanStartType");
		Log.d(TAG, "g_Scan_Start_Type:" + g_Scan_Start_Type);
		String scanType = readLineFromFile("/dev/sunmi/scan/scan_head_type");
		Log.d(TAG, "scanType:" + scanType);
		TextView textViewScanType = (TextView)findViewById(R.id.textViewScanType);
		textViewScanType.setText(scanType);
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

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_STOP_SCAN:
					//((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");

					dspStat(R.string.stopdecode);
					deInit();
					break;
			}
		}
	};

	private void deInit(){
		isDeinit = true;
		if(null != mScanReleaseTask) mScanReleaseTask=null;
		mScanReleaseTask = new ScanReleaseTask();
		mScanReleaseTask.execute();
	}

	class ScanReleaseTask extends AsyncTask {

		@Override
		protected Object doInBackground(Object[] objects) {
			Log.d(TAG, "ScanReleaseTask start ");
			state = STATE_IDLE;
			decCount = 0;
			decodeDataString = new String("");
			decodeStatString = new String("");
			//clearData();
			//dspStat(R.string.stopdecode);
			if ( null != bcr ) {
				bcr.stopDecode();
				bcr.release();
				bcr = null;
			}
			SystemClock.sleep(1000);
			return null;
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			((Button)findViewById(R.id.buttonPressTrigger)).setClickable(true);
			((Button)findViewById(R.id.buttonPressTrigger)).setEnabled(true);
			Intent intent = new Intent();
			intent.putExtra("results",mSaveScanData);
			setResult(1111,intent);
			Log.d(TAG, "ScanReleaseTask end ");
			finish();
		}
	}

	//-----------------------------------------------------
	@Override
	protected void onStop()
	{
		super.onStop();
		if (null != bcr && !isDeinit)
		{
			setIdle();			
			bcr.release();
			bcr = null;
		}
		if(null != mScanReleaseTask && isDeinit){
			mScanReleaseTask.cancel(true);
			mScanReleaseTask = null;
		}
	}

	// ------------------------------------------------------
	// Called when the activity is about to start interacting with the user.
	@Override
	protected void onResume()
	{
		super.onResume();
		state = STATE_IDLE;
		
		toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_SHORT);	

		try
		{
			dspStat(getResources().getString(R.string.app_name) + " v" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
			
			if(se2100_test_platform)
			{
				
				if(android.os.Build.VERSION.SDK_INT >= 18)
					bcr = BarCodeReader.open(cameraId, getApplicationContext()); // Android 4.3 and above
				else
					bcr = BarCodeReader.open(0); // Android 2.3
			}
			else
			{
				if(android.os.Build.VERSION.SDK_INT >= 18)
					bcr = BarCodeReader.open(cameraId, getApplicationContext()); // Android 4.3 and above
				else
					bcr = BarCodeReader.open(0); // Android 2.3
			}
				
			
			if (bcr == null)
			{
				dspErr("open failed");
				return;
			}
			bcr.setDecodeCallback(this);
			
			BarCodeReader.Parameters bp = bcr.getParameters();

			Log.i("Zebra-SDLgui", "0714 Setting Preview Size to 1360x800");
			//bp.setPreviewSize(1360, 800);
			bp.setPictureSize(1360, 800);

			if (!oneShotPreview)
			{
				BarCodeReader.Size previewSz = bp.getPreviewSize();
				previewBuf = new byte[(previewSz.width * previewSz.height * 3)/2];
			}
			// Set parameter
			if(se2100_test_platform)
				bcr.setParameter(765, 0); // For QC/MTK platforms

			//Both Qualcomm and MTK don't support (AIMAGE_FORMAT_PRIVATE)//0x22
			//#define COLORFORMAT_CAMERA2 (AIMAGE_FORMAT_YUV_420_888)//0x23
			//#define COLORFORMAT_CAMERA2 (AIMAGE_FORMAT_RAW_PRIVATE)//0x24
			//#define COLORFORMAT_CAMERA2 (AIMAGE_FORMAT_RAW10)//0x25
			//#define COLORFORMAT_CAMERA2 (AIMAGE_FORMAT_Y8)//0x20203859
			int sm6115 = 1;
			if (sm6115 != 0) // For Qualcomm SM6115
			{
				bcr.setParameter(8600, 0x25); // AIMAGE_FORMAT_RAW10
				bcr.setParameter(8601, 1760); // line stride is 1760 on SM6115 when using RAW10
			}
			//bcr.setParameter(8602, 1); // This parameter decides to close the capture session in stopPreview(). 1 is do not close. 0 is close. default is 0.
			bcr.setParameter(1895, 1); //#1895 is used for controlling CmdAcqOff. 1: SDL doesn't send CmdAcqOff. 0: SDL sends CmdAcqOff.
			//bcr.setParameter(1896, 20); //#1896 is used for controlling noStopPreviewTimeout. in the unit of 500ms.
			bcr.setParameter(764, 1);
			bcr.setParameter(1894, 1); //#1894, enable thermal management in SDL.
			bcr.setParameter(765, 0);
			//bcr.setParameter(762,0);
			//bcr.setParameter(674, 2);
			//bcr.setParameter(-2, 1);
			// Sample of how to setup OCR Related String Parameters
  			// OCR Parameters
			// Enable OCR-B 
			// bcr.setParameter(681, 1);
			
  			// Set OCR templates
  			//String OCRSubSetString = "01234567890"; // Only numeric characters
			//String OCRSubSetString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!%"; // Only numeric characters
  			// Parameter # 686 - OCR Subset
  			//bcr.setParameter(686, OCRSubSetString);
  			
  			//String OCRTemplate = "54R"; // The D ignores all characters after the template
  			// Parameter # 547 - OCR Template
  			//bcr.setParameter(547, OCRTemplate);
  			// Parameter # 689 - OCR Minimum characters
  			//bcr.setParameter(689, 13);
  		    // Parameter # 690 - OCR Maximum characters
  			//bcr.setParameter(690, 13); 
  			
  			// Set Orientation
  			//bcr.setParameter(687, 4); // 4 - omnidirectional
  			
  			// Sets OCR lines to decide 
  			//bcr.setParameter(691, 2); // 2 - OCR 2 lines
			
			// Set decode session timeout to a small value for testing:
			bcr.setParameter(136, 30); // 3 seconds

			// End of OCR Parameter Sample
			if(se2100_test_platform)
			{
				bcr.setDisplayOrientation(180); // For the Clover device
			}

			updateParamUI();

		}
		catch (Exception e)
		{
			dspErr("open excp:" + e);
			Log.v("Zebra-SDLgui","[DEBUG SANDUN] error :" + e);
			Writer writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			Log.v("Zebra-SDLgui" , "[DEBUG SANDUN] stack trace " + writer.toString());
		}

		String sEng = bcr.getStrProperty(BarCodeReader.PropertyNum.ENGINE_VER).trim();
		String projectName = getConfig();
		if("MC520_GMS".equals(projectName)){
			SystemProperties.set("persist.vendor.custmized.scan_version", sEng);
		}else {
			SystemProperties.set("persist.custmized.scan_version", sEng);
		}
		Log.d(TAG, "sEng:" +sEng);

		if(g_Scan_Start_Type != null && !g_Scan_Start_Type.isEmpty()){
			Log.d(TAG, " g_Scan_Start_Type is not empty!");
			if("auto".equals(g_Scan_Start_Type) || "pcbaautotest".equals(g_Scan_Start_Type)){
				if ( state == STATE_IDLE ){
					if ( (bcr != null) && !g_Scan_Start_Type.equals("pcbaautotest") )
					{
						bcr.setParameter(BarCodeReader.ParamNum.CONTINUE_BC_READ,1);
					}
					int status = doDecode();
					if(status == BarCodeReader.BCR_SUCCESS)
					{
						((Button) findViewById(R.id.buttonPressTrigger)).setText("STOP");
					}
				}else{
					((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");
					dspStat(R.string.stopdecode);
					doStopDecode();
				}
				mHandler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 30000);
			}
		}
	}

	public String getConfig() {
		File configFile = new File("/system/etc/meig/cit_common_config.xml");
		String tagValue = "";
		if (!configFile.exists()) {
			return tagValue;
		}
		try {
			InputStream inputStream = new FileInputStream("/system/etc/meig/cit_common_config.xml");
			XmlPullParser xmlPullParser = Xml.newPullParser();
			xmlPullParser.setInput(inputStream, "UTF-8");
			int type = xmlPullParser.getEventType();
			while (type != XmlPullParser.END_DOCUMENT) {
				switch (type) {
					case XmlPullParser.START_TAG:
						String startTagName = xmlPullParser.getName();
						if("common_project_name".equals(startTagName)) {
							tagValue = xmlPullParser.nextText();
							return tagValue;
						}
						break;
				}
				type = xmlPullParser.next();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return tagValue;
	}

	// === Android UI methods =======================================
	//-----------------------------------------------------
	// create main screen
	private void mainScreen()
	{
		Log.i("Zebra-SDLgui", "mainScreen " + atMain);
		
		if (atMain)
			return;
	
		atMain = true;
		
		setContentView(R.layout.main);		// Inflate our UI from its XML layout description.

		// Hook up button presses to the appropriate event handler.
		//((Button) findViewById(R.id.buttonReleaseTrigger)).setOnClickListener(mTriggerReleaseListener);
		((Button) findViewById(R.id.buttonPressTrigger)).setOnClickListener(mTriggerPressListener);
		((Button) findViewById(R.id.buttonHF)).setOnClickListener(mHandsFreeListener);
		((Button) findViewById(R.id.buttonSnap)).setOnClickListener(mSnapListener);
		((Button) findViewById(R.id.buttonVid)).setOnClickListener(mVidListener);
		((Button) findViewById(R.id.buttonGet)).setOnClickListener(mGetParamListener);
		((Button) findViewById(R.id.buttonSet)).setOnClickListener(mSetParamListener);
		((Button) findViewById(R.id.buttonDfl)).setOnClickListener(mDflParamListener);
		((Button) findViewById(R.id.buttonProp)).setOnClickListener(mPropListener);
		((Button) findViewById(R.id.buttonEnable)).setOnClickListener(mEnableAllListener);
		((Button) findViewById(R.id.buttonDisable)).setOnClickListener(mDisableAllListener);
		((Button) findViewById(R.id.buttonDecImage)).setOnClickListener(mGetDecodedImageListener);
				
		((CheckBox) findViewById(R.id.checkBeep)).setOnClickListener(mCheckBeepListener);
		((CheckBox) findViewById(R.id.checkReadingPane)).setOnClickListener(mCheckReadingPaneListener);
		((CheckBox) findViewById(R.id.checkBoxContiniousDecode)).setOnClickListener(mCheckContinuousMode);

		// ui items
		tvStat = (TextView) findViewById(R.id.textStatus);
		tvData = (TextView) findViewById(R.id.textDecode);
		edPnum = (EditText) findViewById(R.id.editPnum);
		edPval = (EditText) findViewById(R.id.editPval);
		chBeep = (CheckBox) findViewById(R.id.checkBeep);
		chBeep.setChecked(beepMode);
		
		chBeep_RP = (CheckBox) findViewById(R.id.checkReadingPane);
		chBeep_RP.setChecked(false);

		// set the decode button text //
		((Button) findViewById(R.id.buttonPressTrigger)).setText("DECODE");
	}

	//-----------------------------------------------------	
	// create snapshot image screen
	private void snapScreen(Bitmap bmSnap)
	{
		atMain = false;
		setContentView(R.layout.image);
		image = (ImageView) findViewById(R.id.snap_image);
		image.setOnClickListener(mImageClickListener);
		
		if (bmSnap != null)
			image.setImageBitmap(bmSnap);
	}
	
	//-----------------------------------------------------	
	// create preview/video screen
	private void vidScreen(boolean addButton)
	{
		atMain = false;
		setContentView(R.layout.surface);
		
		getWindow().setFormat(PixelFormat.UNKNOWN);
		surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.setFixedSize(1360,800);
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		surfaceView.setOnClickListener(mImageClickListener);		
		if (addButton)
		{
   		controlInflater = LayoutInflater.from(getBaseContext());
   		View viewControl = controlInflater.inflate(R.layout.control, null);
   		LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
   		this.addContentView(viewControl, layoutParamsControl);
   		((Button) findViewById(R.id.takepicture)).setOnClickListener(mTakePicListener);
		}
	}

	//-----------------------------------------------------
	// SurfaceHolder callbacks
	// SurfaceHolder callbacks
	public void surfaceCreated(SurfaceHolder holder)
	{		
		Log.i("Zebra-SDLgui", "surfaceCreated " + se2100_test_platform + "State " + state);
		
		if(se2100_test_platform)
		{
			try {
				bcr.setPreviewDisplay(holder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // just set the preview display 
		}
		
		if (state == STATE_PREVIEW)
   		{			
				bcr.startViewFinder(this);			//snapshot with preview mode
   		}
		else if (state == STATE_DECODE)
		{
			if(se2100_test_platform)
			{
				bcr.startDecode();
			}
		}
		else if (state == STATE_HANDSFREE)
		{
			if(se2100_test_platform)
			{
				bcr.startHandsFreeDecode(BarCodeReader.ParamVal.HANDSFREE);
			}
		}
		else if (state == STATE_SNAPSHOT)
		{
			if(se2100_test_platform)
			{
				//bcr.startDecode();
				bcr.takePicture(app);
			}
		}
		else if (state == STATE_GEN_PREVIEW)
		{
			if(se2100_test_platform)
			{
				bcr.startPreview();
			}
		}
   		else //must be video	
   		{
			if(se2100_test_platform)
   			{
   				bcr.startVideoCapture(this);
   			}
   			else
   			{
   				bcr.startPreview();
   			}
   		}
	}

	//-----------------------------------------------------
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
	}

	//-----------------------------------------------------
	public void surfaceDestroyed(SurfaceHolder holder)
	{
	}
	
	// ------------------------------------------------------
	// Called when your activity's options menu needs to be created.
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		//super.onCreateOptionsMenu(menu);

		// menu.add(0, DECODE_ID, 0, R.string.decode).setShortcut('1', 'd');
		// menu.add(0, SNAP_ID, 0, R.string.snap).setShortcut('1', 's');

		getMenuInflater().inflate(R.menu.option_menu,menu);

		if (menu_selection != 0) {

			MenuItem selected = (MenuItem) menu.findItem(menu_selection);
			selected.setChecked(true);
		}

		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the id of radio button selected in the menu
		outState.putInt("selection", menu_selection);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// Restore state members from saved instance
		menu_selection = savedInstanceState.getInt("selection");
	}

	// ------------------------------------------------------
	// Called right before your activity's option menu is displayed.
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		return true;
	}


	// ------------------------------------------------------
	// callback for beep checkbox
	OnClickListener mCheckBeepListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			beepMode = ((CheckBox) v).isChecked();
		}
	};

	// ------------------------------------------------------
	// callback for beep checkbox
	OnClickListener mCheckReadingPaneListener = new OnClickListener()
		{
			public void onClick(View v)
			{

//			if ( ((CheckBox) v).isChecked() )
//			{
//				chBeep_RP.setChecked(true);
//				reading_pane_value = 1;
//				bcr.setParameter(Mobile_reading_pane, reading_pane_value);
//				dspStat("Enabled mobile Phone Reading Pane");
//			}
//			else
//			{
//				chBeep_RP.setChecked(false);
//				reading_pane_value = 0;
//				bcr.setParameter(Mobile_reading_pane, reading_pane_value);
//				dspStat("Disabled mobile Phone Reading Pane");
//			}
			}
		};

	OnClickListener mCheckContinuousMode = new OnClickListener(){
		@Override
		public void onClick(View view) {
			CheckBox checkBox = (CheckBox) view;
			if ( checkBox.isChecked() ){
				if ( bcr != null )
				{
					bcr.setParameter(BarCodeReader.ParamNum.CONTINUE_BC_READ,1);
				}
			}else{
				if ( bcr!= null ) {
					bcr.setParameter(BarCodeReader.ParamNum.CONTINUE_BC_READ,0);
                    ((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");
				}
			}
		}
	};

	
	// ------------------------------------------------------
	// callback for decode button press
//	OnClickListener mTriggerReleaseListener = new OnClickListener()
//	{
//		public void onClick(View v)
//		{
//			doStopDecode();
//		}
//	};

	// ------------------------------------------------------
	// callback for decode button press
	OnClickListener mTriggerPressListener = new OnClickListener()
	{
		public void onClick( View v )
		{

			if ( !  isContousRead() ) {

				if (state == STATE_IDLE) {
					((Button)findViewById(R.id.buttonPressTrigger)).setEnabled(false);
					((Button)findViewById(R.id.buttonPressTrigger)).setClickable(false);
					int status = doDecode();
					if(status == BarCodeReader.BCR_SUCCESS)
					{
						((Button) findViewById(R.id.buttonPressTrigger)).setText("STOP");
					}
					/**/
					Log.i("SDLgui", "paul_stress_test=" + paul_stress_test);
					if (paul_stress_test)
						paul_stress_test = false;
					else
						paul_stress_test = true;
					/**/
				}else{
					((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");
					dspStat(R.string.stopdecode);
					doStopDecode();
				}

			}else{
			
				if ( state == STATE_IDLE ){
					int status = doDecode();
					if(status == BarCodeReader.BCR_SUCCESS)
					{
						//((Button) findViewById(R.id.buttonPressTrigger)).setText("STOP");
					}

				}else{
					//((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");
					dspStat(R.string.stopdecode);
					doStopDecode();

				}
			}

		}
	};

	// ------------------------------------------------------
	// callback for HandsFree button press
	OnClickListener mHandsFreeListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			doHandsFree();
		}
	};

	// ------------------------------------------------------
	// callback for snapshot button press
	OnClickListener mSnapListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			doSnap();
		}
	};

	// ------------------------------------------------------
	// callback for video button press
	OnClickListener mVidListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			// Do native preview from the Camera Service or video from SDL which goes through image processing
			if(genPreview)
				doPreview();
			else
				doVideo();
		}
	};

	// ------------------------------------------------------
	// callback for properties button press
	OnClickListener mPropListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			doGetProp();
		}
	};

	// ------------------------------------------------------	
	// callback for take-picture button on snap-preview screen
	OnClickListener mTakePicListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			doSnap1();
		}
	};
	
	// ------------------------------------------------------
	// callback for video screen click
	OnClickListener mImageClickListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			setIdle();
			mainScreen();
		}
	};

	// ------------------------------------------------------
	// callback for decode button press
	OnClickListener mDflParamListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			AlertDialog.Builder ad = new AlertDialog.Builder(app);		
	       ad.setMessage("Default ALL Parameters?")
	       .setCancelable(false)
	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               doDefaultParams();
	           }
	       })
	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	       public void onClick(DialogInterface dialog, int id) {
	           //just ignore it
	          }
	      });

	      Dialog dlg = ad.create();
			dlg.show();
		}
	};

	// ------------------------------------------------------
	// callback Get Param for button press
	OnClickListener mGetParamListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			getParam();
		}
	};

	// ------------------------------------------------------
	// callback enable all parameters for button press
	OnClickListener mEnableAllListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			dspStat("All Paramters Enabled");
			bcr.enableAllCodeTypes();
			String FilePath = "/mnt/sdcard/PAACDC08-001-R00D0.DAT";
			 
			boolean fIgnoreRelString = true;
			boolean fIgnoreSignature = false;
			
			int Status = bcr.FWUpdate(FilePath, fIgnoreRelString, fIgnoreSignature);

			if (Status == 0)
				dspStat("All Paramters Enabled\nFW Update Successful");
			else
				dspStat("All Paramters Enabled\nFW Update Unsuccessful");
		}
	};
	
	// ------------------------------------------------------
	// callback Disable all parameters for button press
	OnClickListener mDisableAllListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			dspStat("All Paramters Disabled");
			bcr.disableAllCodeTypes();
		}
	};
	
	// ------------------------------------------------------
	// callback Get Last Decoded image for button press
	OnClickListener mGetDecodedImageListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			//dspErr("LastImageDecodeComplete called");
			byte[] data = bcr.getLastDecImage();
			
			//String temp = "length " + data.length + " ";
			//dspStat(temp);
				
			if (data == null)
			{
				dspErr("LastImageDecodeComplete: data null - no image");
			}
						
			// display snapshot		
			Bitmap bmSnap = BitmapFactory.decodeByteArray(data, 0, data.length);
			snapScreen(bmSnap);
			
			if (bmSnap == null)
			{
				dspErr("LastImageDecodeComplete: no bitmap");
			}
			image.setImageBitmap(bmSnap);
		}
	};
	
	// ------------------------------------------------------
		// callback Turbo Mode for button press
	/*	OnClickListener mTurboModeListener = new OnClickListener()
		{
			public void onClick(View v)
			{
				if(!se2100_test_platform)
				{					
					if(!turboMode)
					{					
						if(se2100_test_platform)		
							setupSE2100_TestPlatformForPreview(3);
						bcr.setTurboMode();	
						turboMode = true;
					}
					else
					{
						bcr.clearTurboMode();
						turboMode = false;
					}
				}
				else
				{
					dspStat("Feature not supported");
				}
			}
		};
		*/
	
	// ------------------------------------------------------
	// callback for Set Param button press
	OnClickListener mSetParamListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			setParam();
		}
	};

	// ----------------------------------------
	// display status string
	private void dspStat(String s)
	{
		if(atMain)
			tvStat.setText(s);		
	}

	// ----------------------------------------
	// display status resource id
	private void dspStat(int id)
	{
		if(atMain)
			tvStat.setText(id);
	}

	// ----------------------------------------
	// display error msg
	private void dspErr(String s)
	{
		if(atMain)
			tvStat.setText("ERROR" + s);
	}

	// ----------------------------------------
	// display status string
	private void dspData(String s)
	{
		if(atMain)
		{
			if(multiDecodedData != null)
			{
				tvData.setText(multiDecodedData+"\n"+s);
				multiDecodedData += "\n" + s;
			}else {
				tvData.setText(s);
				multiDecodedData = s;
			}
		}
		else
		{
			if(!s.isEmpty() && s != "" && multiDecodedData !=null){
				toast.setText(multiDecodedData+"\n"+s);
				multiDecodedData += "\n" + s;
			}else {
				toast.setText(s);
				multiDecodedData = s;
			}

			toast.setDuration(Toast.LENGTH_LONG);
			toast.show();
		}
		mSaveScanData = multiDecodedData;
		Log.d(TAG, " mSaveScanData:" + mSaveScanData);
		if(!"auto".equals(g_Scan_Start_Type)) {
			mHandler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 1000);
		}
	}

	private void clearData()
	{
		if(atMain)
		{
				tvData.setText("");
		}

		multiDecodedData = null;

	}
	// -----------------------------------------
	private void beep()
	{
		if (tg != null)
			tg.startTone(ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING);

		if(player != null) {
			player.start();
		}
	}

	// ----------------------------------------
	private void getParam()
	{
		setIdle();		

		// get param #
		String s = edPnum.getText().toString();
		try
		{
			int num = Integer.parseInt(s);

			if(isStringParameter(num))
			{
				doGetStrParam(num);
			}else {
			    doGetParam(num);
                        }
		}
		catch (NumberFormatException nx)
		{
			dspStat("value ERROR");
		}
	}

	//chck String Parameter
	boolean isStringParameter(int num)
	{
		if((num == 547) || (num == 686) || (num == 700) || (num == 834))
		{
			return true;
		}
		else
			return false;
	}
	// ----------------------------------------
	private void setParam()
	{
		setIdle();
		
		// get param #
		String sn = edPnum.getText().toString();
		String sv = edPval.getText().toString();
		try
		{
			int num = Integer.parseInt(sn);
			if(isStringParameter(num))
			{
				doSetStrParam(num,sv);
			}else {
				int val = Integer.parseInt(sv);
				doSetParam(num, val);
			}


		}
		catch (NumberFormatException nx)
		{
			dspStat("value ERROR");
		}

		updateParamUI();
	}

// ==== SDL methods =====================
	
	// ----------------------------------------	
	private boolean isHandsFree()
	{
		return (trigMode == BarCodeReader.ParamVal.HANDSFREE);
	}

	private boolean isContousRead()
	{
		if (bcr != null) {
			return (1 == bcr.getNumParameter(BarCodeReader.ParamNum.CONTINUE_BC_READ));
		} else{
			return false;
		}
	}

	// ----------------------------------------	
	private boolean isAutoAim()
	{
		return (trigMode == BarCodeReader.ParamVal.AUTO_AIM);		
	}

	// ----------------------------------------
	// reset Level trigger mode
	void resetTrigger()
	{
		doSetParam(BarCodeReader.ParamNum.PRIM_TRIG_MODE, BarCodeReader.ParamVal.LEVEL); 
		trigMode = BarCodeReader.ParamVal.LEVEL;		
	}

	// ----------------------------------------
	// get param
	private String doGetStrParam(int num)
	{
		String val = bcr.getStrParameter(num);
		if (val != null)
		{
			dspStat("Get # " + num + " = " + val);
			edPval.setText(val);
		}
		else
		{
			dspStat("Get # " + num + " FAILED (" + val + ")");
			edPval.setText(val);
		}
		return val;
	}

	// ----------------------------------------
	// get param
	private int doGetParam(int num)
	{
		int val = bcr.getNumParameter(num);
		if (val != BarCodeReader.BCR_ERROR)
		{
			dspStat("Get # " + num + " = " + val);
			edPval.setText(Integer.toString(val));
		}
		else
		{
			dspStat("Get # " + num + " FAILED (" + val + ")");
			edPval.setText(Integer.toString(val));
		}
		return val;
	}

	// ----------------------------------------
	// set number param
	private int doSetParam(int num, int val)
	{
		String s= "";
		int ret = bcr.setParameter(num, val);
		if (ret != BarCodeReader.BCR_ERROR)
		{
			if (num == BarCodeReader.ParamNum.PRIM_TRIG_MODE)
			{
				trigMode = val;
				if (val == BarCodeReader.ParamVal.HANDSFREE)
				{
					s = "HandsFree";
				}
				else if (val == BarCodeReader.ParamVal.AUTO_AIM)
				{
					s = "AutoAim";
					ret = bcr.startHandsFreeDecode(BarCodeReader.ParamVal.AUTO_AIM);
					if (ret != BarCodeReader.BCR_SUCCESS)
					{
						dspErr("AUtoAIm start FAILED");
					}
				}
				else if (val == BarCodeReader.ParamVal.LEVEL)
				{
					s = "Level";
				}
			}
			else if (num == BarCodeReader.ParamNum.IMG_VIDEOVF)
			{
				if ( snapPreview=(val == 1) )
					s = "SnapPreview";				
			}
		}
		else
			s = " FAILED (" + ret +")";
			
		dspStat("Set #" + num + " to " + val + " " + s);			
		return ret;
	}

	// ----------------------------------------
	// set string param
	private int doSetStrParam(int num, String val)
	{
		String s= "";
		int ret = bcr.setParameter(num, val);
		if (ret == BarCodeReader.BCR_ERROR) {
			s = " FAILED (" + ret + ")";
		}
		dspStat("Set #" + num + " to " + val + " " + s);
		return ret;
	}

	// ----------------------------------------
	// set Default params
	private void doDefaultParams()
   {
		setIdle();		
   	  bcr.setDefaultParameters();
   	  dspStat("Parameters Defaulted");

   	  // reset modes
  		snapPreview = false;
   	  int val = bcr.getNumParameter(BarCodeReader.ParamNum.PRIM_TRIG_MODE);
  		if (val != BarCodeReader.BCR_ERROR)  
   		trigMode = val;

	   updateParamUI();
   }

   //updateUI according to the parameter
   private void updateParamUI()
   {
   		if(bcr != null) {
			//set Continous checkbox according to the set value
			int isContinousRead = bcr.getNumParameter(BarCodeReader.ParamNum.CONTINUE_BC_READ);

			if (isContinousRead == 1) {
				((CheckBox) findViewById(R.id.checkBoxContiniousDecode)).setChecked(true);
			} else {
				((CheckBox) findViewById(R.id.checkBoxContiniousDecode)).setChecked(false);
			}
		}
   }

	// ----------------------------------------
	// get properties
	private void doGetProp()
	{
		setIdle();
		String sMod = bcr.getStrProperty(BarCodeReader.PropertyNum.MODEL_NUMBER).trim();
		String sSer = bcr.getStrProperty(BarCodeReader.PropertyNum.SERIAL_NUM).trim();
		String sImg = bcr.getStrProperty(BarCodeReader.PropertyNum.IMGKIT_VER).trim();
		String sEng = bcr.getStrProperty(BarCodeReader.PropertyNum.ENGINE_VER).trim();
		String sBTLD = bcr.getStrProperty(BarCodeReader.PropertyNum.BTLD_FW_VER).trim();
		
		int buf = bcr.getNumProperty(BarCodeReader.PropertyNum.MAX_FRAME_BUFFER_SIZE);
		int hRes = bcr.getNumProperty(BarCodeReader.PropertyNum.HORIZONTAL_RES);
		int vRes = bcr.getNumProperty(BarCodeReader.PropertyNum.VERTICAL_RES);

		String s = "Model:\t\t" + sMod + "\n";
		s += "Serial:\t\t" + sSer + "\n";
		s += "Bytes:\t\t" + buf + "\n";
		s += "V-Res:\t\t" + vRes + "\n";
		s += "H-Res:\t\t" + hRes + "\n";
		s += "ImgKit:\t\t" + sImg + "\n";
		s += "Engine:\t" + sEng + "\n";
		s += "FW BTLD:\t" + sBTLD + "\n";
		
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		if (dlg != null)
		{
			dlg.setTitle("SDL Properties");
			dlg.setMessage(s);
			dlg.setPositiveButton("ok", null);
			dlg.show();
		}
	}
	
	private void setupSE2100_TestPlatformForPreview(int previewMode)
	{
		try
		{
			atMain = false;
			
			Log.i("Zebra-SDLgui", "setupSE2100_TestPlatformForPreview " + previewMode);
			
			if(previewMode == 1)
			{
				
				setContentView(R.layout.previewimage);
				// Create a tiny preview surface so that startPreview can work. 
				// Newer android versions do not start camera preview unless a preview display is given
				// Added here for use with the SE2100 test device
				getWindow().setFormat(PixelFormat.UNKNOWN);
				surfaceView = (SurfaceView) findViewById(R.id.cameraPreviewSurface_2);				
			}
			else
			{
				setContentView(R.layout.previewimage_tiny);
				// Create a tiny preview surface so that startPreview can work. 
				// Newer android versions do not start camera preview unless a preview display is given
				// Added here for use with the SE2100 test device		
				getWindow().setFormat(PixelFormat.UNKNOWN);
				surfaceView = (SurfaceView) findViewById(R.id.SurfaceView1);
				image = (ImageView) findViewById(R.id.imageView1);
			}
			
			//surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
			surfaceHolder = surfaceView.getHolder();
			surfaceHolder.setFixedSize(1360,800);
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			
			surfaceView.setOnClickListener(mImageClickListener);		
						
		}
		catch (Exception e)
		{
			dspErr("open excp:" + e);
		}
	}

	// ----------------------------------------
	// start a decode session
	private int doDecode()
	{
		int status = BarCodeReader.BCR_ERROR;

		if (setIdle() != STATE_IDLE)
			return status;
		
		state = STATE_DECODE;

		decCount = 0;
		decodeDataString = new String("");
		decodeStatString = new String("");
		startTime = System.currentTimeMillis();

		clearData();
		dspStat(R.string.decoding);
		
		if(se2100_test_platform)
			setupSE2100_TestPlatformForPreview(2);
		else {
			try {
				status = bcr.startDecode(); // start decode (callback gets results)
			} catch (Exception e) {
				dspErr("open excp:" + e);
			}
		}

		return status;
	}


	// ----------------------------------------
	// start a decode session
	private void doStopDecode()
	{
		state = STATE_IDLE;
		decCount = 0;
		decodeDataString = new String("");
		decodeStatString = new String("");
		//clearData();
		//dspStat(R.string.stopdecode);
		testModeHandler.sendEmptyMessage(STOP_DECODE);
	}

	// ----------------------------------------
	// start HandFree decode session
	private void doHandsFree()
	{
		/*if (setIdle() != STATE_IDLE)
			return;*/
		
		trigMode = BarCodeReader.ParamVal.HANDSFREE;
		state = STATE_HANDSFREE;

		decodeDataString = new String("");
		decodeStatString = new String("");
		clearData();
		dspStat("HandsFree decoding");
		
		if(se2100_test_platform)
		{
			setupSE2100_TestPlatformForPreview(2);
		}
		else
		{
			int ret = bcr.startHandsFreeDecode(BarCodeReader.ParamVal.HANDSFREE);
			if (ret != BarCodeReader.BCR_SUCCESS)
				dspStat("startHandFree FAILED :"+ret);
		}
	}
	
	// ----------------------------------------
	// BarCodeReader.DecodeCallback override
	public void onDecodeComplete(int symbology, int length, byte[] data, BarCodeReader reader)
	{
		if (state == STATE_DECODE && ! isContousRead() )
			state = STATE_IDLE;
		
		// Get the decode count
		if( length == BarCodeReader.DECODE_STATUS_MULTI_DEC_COUNT )
			decCount = symbology;

		if ( isHandsFree()==false && state!= STATE_SNAPSHOT)
			mainScreen();
		
		if ( length > 0)
		{
			//Moved the below lines to the end for better performance
			//if (isHandsFree()==false && isAutoAim()==false)
				//bcr.stopDecode();

			++decodes;
			
			if ( symbology == 0x69 )	// signature capture
			{
				if (sigcapImage)
     			{
   				Bitmap bmSig = null;
   				int scHdr = 6;
   				if ( length > scHdr )
   					bmSig = BitmapFactory.decodeByteArray(data, scHdr, length-scHdr);
   				
   				if ( bmSig != null )
   					snapScreen(bmSig);

   				else
   					dspErr("OnDecodeComplete: SigCap no bitmap");
     			}

				decodeStatString += new String("[" + decodes + "] type: " + symbology + " len: " + length);
				decodeDataString += new String(data);
			}
			else
			{
				if ( symbology == 0x99 )	//type 99?
				{
					symbology = data[0];
					int n = data[1];
					int s = 2;
					int d = 0;
					int len = 0;

					byte d99[] = new byte[data.length];
					for (int i=0; i<n; ++i)
					{
						s += 2;
						len = data[s++];
						System.arraycopy(data, s, d99, d, len);
						s += len;
						d += len;
					}
					d99[d] = 0;
					data = d99;
				}
				endTime = System.currentTimeMillis();
				decodeStatString += new String("[F:"+ decode_fail_count +"][OK:" + decodes + "] type: " + symbology + " len: " + length
				+ "time:"+(endTime-startTime)+"ms");
				decodeDataString += new String(data);
				dspStat(decodeStatString);
				dspData(decodeDataString);

				Log.i("TEST","Decode Success ");

				if(decCount > 1) // Add the next line only if multiple decode
				{
					decodeStatString += new String(" ; ");
					decodeDataString += new String(" ; ");
				}
				else
				{
					decodeDataString = new String("");
					decodeStatString = new String("");
				}
			}
			
			if (beepMode)
				beep();
			if (isHandsFree()==false && isAutoAim()==false && isContousRead() == false) {
				bcr.stopDecode();
				runOnUiThread( new Runnable() {

					public void run() {
						if (atMain) {
							//((Button) findViewById(R.id.buttonPressTrigger)).setText("DECODE");
						}
				}});
			}
		}
      else	// no-decode
      {
      	clearData();
      	switch (length)
      	{
      	case BarCodeReader.DECODE_STATUS_TIMEOUT:
			decodeStatString = new String("decode timed out");
      		//dspStat("decode timed out");
			Log.i("TEST","No Decode : decode timed out");
			((Button)findViewById(R.id.buttonPressTrigger)).setEnabled(true);
			((Button)findViewById(R.id.buttonPressTrigger)).setClickable(true);
			((Button)findViewById(R.id.buttonPressTrigger)).setText("DECODE");
			if("pcbaautotest".equals(g_Scan_Start_Type)){
				mHandler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 1000);
			}

			//Save Frame
			//bcr.setParameter(-2, 1);
      		break;
      		
      	case BarCodeReader.DECODE_STATUS_CANCELED:
      		//dspStat("decode cancelled");
			decodeStatString = new String("decode cancelled");
			Log.i("TEST","No Decode : decode cancelled");
      		break;

      	case BarCodeReader.DECODE_STATUS_ERROR:
			decodeStatString = new String("DECODE_STATUS_ERROR");
			break;
      	default:
			//Log.i("TEST","No Decode : decode failed");
      		//dspStat("decode failed");
      		break;
      	}
		  if( length != BarCodeReader.DECODE_STATUS_MULTI_DEC_COUNT )
		  {
			  ++decode_fail_count;
		  decodeStatString += new String("[F:"+ decode_fail_count +"][OK:" + decodes + "]");
		  dspStat(decodeStatString);
		  }
      }
		runOnUiThread( new Runnable() {

			public void run() {
				if (atMain) {
					//((Button) findViewById(R.id.buttonPressTrigger)).setText("DECODE");
				}
			}});

      if (false){//(paul_stress_test) {
		  if (decodes < 500000 && length != BarCodeReader.DECODE_STATUS_MULTI_DEC_COUNT/*&& length > 0*/) {

			  Log.i("SDL-gui", "paul decodes=" + decodes+ "F:"+ decode_fail_count);

			  SystemClock.sleep(50);
			  testModeHandler.sendEmptyMessage(START_DECODE);
			  //doDecode();
		  }
	  }
      else {
		  if (isHandsFree() == false && isContousRead() == false && state != STATE_SNAPSHOT) {
			  doStopDecode();
		  }
	  }

	}
	// ----------------------------------------
	// start a snap/preview session
	private void doSnap()
	{
		if (setIdle() != STATE_IDLE)		
			return;

		resetTrigger();
		clearData();
		
		if(se2100_test_platform)	
		{
			setupSE2100_TestPlatformForPreview(2);
		}
		
		if (snapPreview)		//snapshot-preview mode?
		{
			state = STATE_PREVIEW;
			videoCapDisplayStarted = false;
			dspStat("Snapshot Preview");
			bcr.startViewFinder(this); 
		}
		else
		{
			state = STATE_SNAPSHOT;
			
			// Some of the newer platforms needs the acquisition to be started only after a rendering surface 
			// is created.  Handled inside the SurfaceCreated callback. 
			if(!se2100_test_platform)
			{
				snapScreen(null);
				bcr.takePicture(app);
			}
		}
	}

	// ----------------------------------------
	// take snapshot   
	private void doSnap1()
	{

		if (state == STATE_PREVIEW)
		{
			bcr.stopPreview();
			state = STATE_SNAPSHOT;
		}

		if (state == STATE_SNAPSHOT)
		{
			snapScreen(null);
			bcr.takePicture(app);
		}
		else //unexpected state - reset mode
		{
			setIdle();
			mainScreen();
		}
	}

	// ----------------------------------------
	public void onPictureTaken(int format, int width, int height, byte[] abData, BarCodeReader reader)
	{
		// Render it on the snapshot image
		
		Log.i("Zebra-SDLgui", "onPictureTaken of res" + width + " w" + height + " h");
		
		
		if( se2100_test_platform )
			snapScreen(null);
		
		if ( image == null)
			return;			

		// display snapshot		
		Bitmap bmSnap = BitmapFactory.decodeByteArray(abData, 0, abData.length);
		if (bmSnap == null)
		{
			dspErr("OnPictureTaken: no bitmap");
			return;			
		}

		image.setImageBitmap(bmSnap);
		//image.setImageBitmap(rotated(bmSnap));
	
		// Save snapshot to the SD card
		if (saveSnapshot)
		{
			String snapFmt = "bin";			
			switch (bcr.getNumParameter(BarCodeReader.ParamNum.IMG_FILE_FORMAT))
			{
			case BarCodeReader.ParamVal.IMG_FORMAT_BMP:
				snapFmt = "bmp";
				break;
			
			case BarCodeReader.ParamVal.IMG_FORMAT_JPEG:
				snapFmt = "jpg";				
				break;
			
			case BarCodeReader.ParamVal.IMG_FORMAT_TIFF:
				snapFmt = "tif";	
				break;
			}
			
			File filFSpec = null;
			try
			{
				String strFile = String.format("se4500_img_%d.%s", snapNum, snapFmt);
				File filRoot = Environment.getExternalStorageDirectory();
				File filPath = new File(filRoot.getAbsolutePath() + "/DCIM/Camera");
				filPath.mkdirs();
				filFSpec = new File(filPath, strFile);
				FileOutputStream fos = new FileOutputStream(filFSpec);
				fos.write(abData);
				fos.close();
				++snapNum;

			}
			catch (Throwable thrw)
			{
				dspErr("Create '" + filFSpec.getAbsolutePath() + "' failed");
				dspErr("Error=" + thrw.getMessage());
			}
		}
		
		Log.i("Zebra-SDLgui", "onPictureTaken Exit");
	}
	
	// ----------------------------------------
	// start non-SDL Preview mode
	private void doPreview()
	{
		if (setIdle() != STATE_IDLE)
			return;

		resetTrigger();
		clearData();
		dspStat("preview started");
		
		if(se2100_test_platform)
			setupSE2100_TestPlatformForPreview(2);
		
		state = STATE_GEN_PREVIEW;
		
		if(!se2100_test_platform)
			snapScreen(null);
		
		try
		{
			if (oneShotPreview)
			{
				bcr.setOneShotPreviewCallback(this);
			}
		else
			{
				bcr.addCallbackBuffer(previewBuf);
				bcr.setPreviewCallbackWithBuffer(this);
			}
			
			if(!se2100_test_platform)
				bcr.startPreview();
		}
		catch (Exception e)
		{
			dspErr("doPreview: " + e);
		}
	}

	
	// ----------------------------------------
	// generic preview callback
	public void onPreviewFrame(byte[] data, BarCodeReader arg1)
	{
		if (  genPreview == false  )
			return;
		
		if (state != STATE_GEN_PREVIEW)
		{
			setIdle();
			return;
		}
		
		Log.i("Zebra-SDLgui", "OnPreviewFrame");
			
		doPreviewFrame(data);
	}

	// ----------------------------------------
	// start video session
	private void doVideo()
	{
		if (setIdle() != STATE_IDLE)		
			return;
	
		resetTrigger();
		clearData();
		dspStat("video started");
		
		if(se2100_test_platform)		
			setupSE2100_TestPlatformForPreview(2);
		state = STATE_VIDEO;
		videoCapDisplayStarted = false;
		
		
		if(!se2100_test_platform) // For all non-SE2100 engines
			bcr.startVideoCapture(this);
	}
	
	//------------------------------------------------------------
		private void doPreviewFrame(byte[] data)
		{
			BarCodeReader.Parameters bp = bcr.getParameters();
			BarCodeReader.Size sz = bp.getPreviewSize();
			if (os == null)
				os = new ByteArrayOutputStream((sz.width * sz.height * 3)/2);

			if (os != null)
			{
				int pf = bp.getPreviewFormat();
				os.reset();
				YuvImage yuv = new YuvImage(data, pf, sz.width, sz.height, null);
				Rect r = new Rect(0, 0, sz.width, sz.height);
				yuv.compressToJpeg(r, 100, os);
				byte b1[] = os.toByteArray();
				Bitmap bmPre = BitmapFactory.decodeByteArray(b1, 0, b1.length);
				if (bmPre != null)
					image.setImageBitmap(rotated(bmPre));
				else
					dspErr("onPreviewFrame: FAILED");
			}

			if (oneShotPreview)
				bcr.setOneShotPreviewCallback(this);
			else
	  			bcr.addCallbackBuffer(previewBuf);	//recycle buffer			
		}

	//------------------------------------------	
	private int setIdle()
	{
		int prevState = state;
		int ret = prevState;		//for states taking time to chg/end
		
		Log.i("Zebra-SDLgui", "Current State " + prevState);

//		if ( atMain ){
//			if( isContousRead() ) {
//				((Button) findViewById(R.id.buttonPressTrigger)).setText("DECODE");
//			}else{
//				((Button) findViewById(R.id.buttonPressTrigger)).setText()
//			}
//		}
		
		state = STATE_IDLE;
		switch (prevState)
		{
		case STATE_HANDSFREE:
			resetTrigger();
			//fall thru
		case STATE_DECODE:
			dspStat("decode stopped");
			bcr.stopDecode();
			break;
		case STATE_GEN_PREVIEW:
		case STATE_VIDEO:
			bcr.stopPreview();
			break;
		
		case STATE_SNAPSHOT:
			if(se2100_test_platform)
			{
				bcr.stopDecode();
			}
			ret = STATE_IDLE;
			break;
		default:
			ret = STATE_IDLE;
		}
		return ret;
	}

	// ----------------------------------------
	public void onEvent(int event, int info, byte[] data, BarCodeReader reader)
	{
		switch (event)
		{
		case BarCodeReader.BCRDR_EVENT_SCAN_MODE_CHANGED:
			++modechgEvents;
			dspStat("Scan Mode Changed Event (#" + modechgEvents + ")");
			break;

		case BarCodeReader.BCRDR_EVENT_MOTION_DETECTED:
			++motionEvents;
			dspStat("Motion Detect Event (#" + motionEvents + ")");
			break;
			
		case BarCodeReader.BCRDR_EVENT_SCANNER_RESET:
			//dspStat("Reset Event"); // No need to display this event
			break;

		default:
			// process any other events here
			break;
		}
	}

	//-------------------------------------------------------
	private Bitmap rotated(Bitmap bmSnap)
	{
		Matrix matrix = new Matrix();
		if (matrix != null)
		{
			matrix.postRotate(90); 	  
			// create new bitmap from orig tranformed by matrix
			Bitmap bmr = Bitmap.createBitmap(bmSnap , 0, 0, bmSnap.getWidth(), bmSnap.getHeight(), matrix, true);
			if (bmr != null)
				return bmr;
		}

		return bmSnap;		//when all else fails
	}

	public void onVideoFrame(int format, int width, int height, byte[] data,
			BarCodeReader reader) {
		
		Log.i("Zebra-SDLgui", "onVideoFrame " + "width " + width + "height " + height);
		
		// display snapshot		
		Bitmap bmSnap = BitmapFactory.decodeByteArray(data, 0, data.length);
		
		if(!se2100_test_platform)
		{
			if(videoCapDisplayStarted == false)
			{
				atMain = false;
				videoCapDisplayStarted = true;
				setContentView(R.layout.previewimage_tiny);
				image = (ImageView) findViewById(R.id.imageView1);
				
				// This handles snapshot with viewfinder
				if(state == STATE_PREVIEW)
				{
					controlInflater = LayoutInflater.from(getBaseContext());
					View viewControl = controlInflater.inflate(R.layout.control, null);
					LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
					this.addContentView(viewControl, layoutParamsControl);
					((Button) findViewById(R.id.takepicture)).setOnClickListener(mTakePicListener);
				}
				else
				{
					image.setOnClickListener(mImageClickListener);
				}
			}
		}
		
		if (bmSnap != null && image != null)
			image.setImageBitmap(bmSnap);	
		
		/*toast.setText("Video Frame Received\n");
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();*/
	}

	@Override
	public void onBackPressed() {
		showBackDialog();
	}

	private void showBackDialog(){
		String message = getResources().getString(R.string.scaning_back);
		AlertDialog mAlertDialog = new AlertDialog.Builder(SDLguiActivity.this)
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
}//end-class
