package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.RuninConfig;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.sunmi.peripheral.printer.InnerResultCallback;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import butterknife.BindView;

public class SunmiPrinter_NewTestActivity extends BaseActivity implements OnClickListener, PromptDialog.OnPromptDialogCallBack{

	private String TAG = "PrinterTest";

	private SunmiPrinter_NewTestActivity mContext;
	@BindView(R.id.title)
	public TextView mTitle;
	@BindView(R.id.back)
	public LinearLayout mBack;
	@BindView(R.id.success)
	public Button mSuccess;
	@BindView(R.id.fail)
	public Button mFail;
	@BindView(R.id.text_msg)
	public TextView mMsg;
	@BindView(R.id.btn_test)
	public Button mBtnPrint;
	private String mFatherName = "";
	String Pcba_sn = "0000000000000000000";

	private int mConfigTime = 0;
	private Runnable mRun;
	public static final byte GS =  0x1D;// Group separator
	byte[] rv = null;

	private boolean showSuccessButton = false;
	private SunmiPrinterService sunmiPrinterService;

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

	private void showDialog() {
		AlertDialog.Builder dialog =
	            new AlertDialog.Builder(SunmiPrinter_NewTestActivity.this);
		dialog.setMessage(R.string.do_printerTest);
		dialog.setPositiveButton(R.string.str_yes,
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	mBtnPrint.setEnabled(true);
					String msg = "";
					try {
						String serviceVersion = sunmiPrinterService.getServiceVersion();
						Log.d("Printer_zhr","serviceVersion:"+serviceVersion);
						Log.d("Printer_zhr","mWoyouService.getPrinterVersion():"+sunmiPrinterService.getPrinterVersion());
						msg = msg+"Service Version:  "+serviceVersion+"\n";
						msg = msg+"Printer Version:  "+sunmiPrinterService.getPrinterVersion()+"\n";
						mMsg.setText(msg);
					} catch (RemoteException e) {
						Log.d(TAG, "====>error:"+e.toString());
						e.printStackTrace();
					}
					mBtnPrint.setOnClickListener(new OnClickListener() {

	        			@Override
	        			public void onClick(View v) {
	        					Log.d("Printer_zhr","printerSelfChecking");
								try {
									String PrinterVersion = sunmiPrinterService.getPrinterVersion();
									if (!showSuccessButton && !PrinterVersion.equals("")
											&& !PrinterVersion.isEmpty() && PrinterVersion != null) {
										mSuccess.setVisibility(View.VISIBLE);
										showSuccessButton = true;
									}
								}catch (Exception e){

								}
								if(mFatherName.equals(getResources().getString(R.string.PCBASignalActivity))||mFatherName.equals(getResources().getString(R.string.PCBAActivity))){
									onPrintPCBA();
								}else{
									onPrintMMI();
								}
	        			}
	        		});
	            }
	        });
		dialog.setNegativeButton(R.string.str_no,
	            new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	mBtnPrint.setEnabled(false);
	            	if (!showSuccessButton) {
					//	mSuccess.setVisibility(View.VISIBLE);
						showSuccessButton = true;
					}
	            }
	        });
		dialog.setCancelable(false);
		dialog.show();
	}

	public static byte[] initBlackBlock(int w){
		int ww = (w + 7)/8 ;
		int n = (ww + 11)/12;
		int hh = n * 24;
		byte[] data = new byte[ hh * ww + 5];

		data[0] = (byte)ww;//xL
		data[1] = (byte)(ww >> 8);//xH
		data[2] = (byte)hh;
		data[3] = (byte)(hh >> 8);

		int k = 4;
		for(int i=0; i < n; i++){
			for(int j=0; j<24; j++){
				for(int m =0; m<ww; m++){
					if(m/12 == i){
						data[k++] = (byte)0xFF;
					}else{
						data[k++] = 0;
					}
				}
			}
		}
		data[k++] = 0x0A;
		return data;
	}

	private void onPrintPCBA() {
		try {
			if (!checkPrint()) {
				return;
			}
			sunmiPrinterService.clearBuffer();
			sunmiPrinterService.printTextWithFont(Pcba_sn+"\n", "", 23, null);
			sunmiPrinterService.exitPrinterBufferWithCallback(true, innerResultCallback);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onPrintMMI() {
		try {
			if (!checkPrint()) {
				return;
			}
			sunmiPrinterService.clearBuffer();
			rv=printBitmap(initBlackBlock(384));
			sunmiPrinterService.printTextWithFont("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT"+"\n", "", 23, null);
			sunmiPrinterService.sendRAWData(rv,null);
			sunmiPrinterService.lineWrap(3, null);
			sunmiPrinterService.exitPrinterBufferWithCallback(true, innerResultCallback);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_print_new_test;
	}

	@Override
	protected void initData() {
		mContext = this;
		super.startBlockKeys = Const.isCanBackKey;
		mBack.setVisibility(View.GONE);
		mSuccess.setVisibility(View.GONE);
		//mFail.setVisibility(View.GONE);
		mBack.setOnClickListener(this);
		mSuccess.setOnClickListener(this);
		mFail.setOnClickListener(this);
		mTitle.setText(R.string.SunmiPrinter_NewTestActivity);
		sunmiPrinterService = MyApplication.sunmiPrinterService;

		mDialog.setCallBack(this);
		mFatherName = getIntent().getStringExtra("fatherName");
		super.mName = getIntent().getStringExtra("name");
		addData(mFatherName, super.mName);
		Pcba_sn=SystemProperties.get(getResources().getString(R.string.version_default_config_serial_no), getString(R.string.empty));
		if(Pcba_sn.equals("")||Pcba_sn.isEmpty()||Pcba_sn==null){
			Pcba_sn="0000000000000000000";
		}

		if(mFatherName.equals(MyApplication.RuninTestNAME)) {
			mConfigTime = RuninConfig.getRunTime(mContext, this.getLocalClassName());
		} else {
			mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
		}

		mBtnPrint = (Button)findViewById(R.id.btn_test);
		mMsg = (TextView)findViewById(R.id.text_msg);

		mHandler.sendEmptyMessage(1001);
		mRun = new Runnable() {
			@Override
			public void run() {
				mConfigTime--;
				updateFloatView(mContext,mConfigTime);
				if (((mConfigTime == 0) && mFatherName.equals(MyApplication.RuninTestNAME)) || (mFatherName.equals(MyApplication.RuninTestNAME) && RuninConfig.isOverTotalRuninTime(mContext))) {
					mHandler.sendEmptyMessage(1111);
				}
				mHandler.postDelayed(this, 1000);
			}
		};
		mRun.run();
	}

	private boolean checkPrint() {
		if (MyApplication.sunmiPrinterService == null) {
			return false;
		}
		return true;
	}

	public static byte[] printBitmap(Bitmap bitmap){
		byte[] bytes1  = new byte[4];
		bytes1[0] = GS;
		bytes1[1] = 0x76;
		bytes1[2] = 0x30;
		bytes1[3] = 0x00;

		byte[] bytes2 = getBytesFromBitMap(bitmap);
		return byteMerger(bytes1, bytes2);
	}

	public static byte[] printBitmap(Bitmap bitmap, int mode){
		byte[] bytes1  = new byte[4];
		bytes1[0] = GS;
		bytes1[1] = 0x76;
		bytes1[2] = 0x30;
		bytes1[3] = (byte) mode;

		byte[] bytes2 = getBytesFromBitMap(bitmap);
		return byteMerger(bytes1, bytes2);
	}

	public static byte[] printBitmap(byte[] bytes){
		byte[] bytes1  = new byte[4];
		bytes1[0] = GS;
		bytes1[1] = 0x76;
		bytes1[2] = 0x30;
		bytes1[3] = 0x00;

		return byteMerger(bytes1, bytes);
	}

	public static byte[] getBytesFromBitMap(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int bw = (width - 1) / 8 + 1;

		byte[] rv = new byte[height * bw + 4];
		rv[0] = (byte) bw;//xL
		rv[1] = (byte) (bw >> 8);//xH
		rv[2] = (byte) height;
		rv[3] = (byte) (height >> 8);

		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int clr = pixels[width * i + j];
				int red = (clr & 0x00ff0000) >> 16;
				int green = (clr & 0x0000ff00) >> 8;
				int blue = clr & 0x000000ff;
				byte gray = (RGB2Gray(red, green, blue));
				rv[bw*i + j/8 + 4] = (byte) (rv[bw*i + j/8 + 4] | (gray << (7 - j % 8)));
			}
		}

		return rv;
	}

	private static byte RGB2Gray(int r, int g, int b) {
		return (false ? ((int) (0.29900 * r + 0.58700 * g + 0.11400 * b) > 200)
				: ((int) (0.29900 * r + 0.58700 * g + 0.11400 * b) < 200)) ? (byte) 1 : (byte) 0;
	}

	public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
		byte[] byte_3 = new byte[byte_1.length + byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}

	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(mRun);
		super.onDestroy();
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
	public void onResultListener(int result) {
		if (result == 0){
			deInit(mFatherName, result,Const.RESULT_NOTEST);
		}else if (result == 1){
			deInit(mFatherName, result,Const.RESULT_UNKNOWN);
		}else if (result == 2){
			deInit(mFatherName, result);
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case 1001:
					showDialog();
					break;
				case 1111:
					deInit(mFatherName, FAILURE);
					break;
				case 9999:
					deInit(mFatherName, FAILURE,msg.obj.toString());
					break;
			}
		}
	};
}
