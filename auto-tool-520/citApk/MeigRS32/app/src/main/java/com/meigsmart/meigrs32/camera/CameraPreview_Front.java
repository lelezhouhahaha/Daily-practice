package com.meigsmart.meigrs32.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.config.CustomConfig;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;
import com.meigsmart.meigrs32.util.FileUtil;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraPreview_Front extends SurfaceView implements
		SurfaceHolder.Callback {
	private static final String TAG = "CameraPreview_Front";
	private int viewWidth = 0;
	private int viewHeight = 0;
	private OnCameraStatusListener listener;
	private SurfaceHolder holder;
	private Camera camera;
	private boolean takePhoto=false;
	private String mFrontCameraRotation = null;
	private String mFrontCameraRotationAngleConfig = "common_front_camera_rotation_angle";
	private final String DEVICE_NAME = "common_device_name_test";
	private final String CAMERA_SWAP_WIDTH_HEIGHT = "commom_camera_swap_width_height";

	/** commom_front_camera_previewsize_method value.
	 *  selectPreviewSize:0  findBestPreviewSize:1  findPreviewSizeByScreen:2  WxH:1920x1080
	 */
	private final String CAMERA_PREVIEW_SIZE_METHOD = "commom_front_camera_previewsize_method";
	private String previewSizeMethod ="0";

	private boolean mFrontCameraSwapWidthHeight;
	public boolean isPreviewing=false;//hejianfeng add for zendao12318
	private boolean isMT535_version =false;
	private String projectName = "";
	private String WIFI_mt535 = "common_device_wifi_only";

	private PictureCallback pictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			try {
				synchronized (Thread.currentThread()) {
					camera.stopPreview();
					Thread.currentThread().notify();
				}
			} catch (Exception e) {
			}
			if (null != listener) {
				listener.onCameraStopped(data);
			}
		}
	};

	public CameraPreview_Front(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mFrontCameraSwapWidthHeight = "slb761x".equals( DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, DEVICE_NAME))
				|| "SLM752".equals( DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, DEVICE_NAME))
				|| "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CAMERA_SWAP_WIDTH_HEIGHT));

		previewSizeMethod = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CAMERA_PREVIEW_SIZE_METHOD);
		projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);
		isMT535_version = "MT535".equals(projectName);
	}


	public void surfaceCreated(SurfaceHolder holder) {
		if(!Utils.checkCameraHardware(getContext())) {
			return;
		}
		Log.d("MMCAM","getCameraInstance");
		camera = getCameraInstance();
		/*try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
			camera.release();
			camera = null;
		}
		updateCameraParameters();
		if (camera != null) {
			camera.startPreview();
		}*/
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e(TAG, "==surfaceDestroyed==");
		/*if(camera!=null) {
			camera.release();
			camera = null;
		}*/
		isPreviewing=false;//hejianfeng add for zendao12318
	}

	public void surfaceChanged(final SurfaceHolder holder, int format, int w,
							   int h) {
		// stop preview before making changes
		try {
			camera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}
		//hejianfeng modif start for zendao 10260
		MyApplication.getInstance().getSingleThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				// set preview size and make any resize, rotate or
				// reformatting changes here
				updateCameraParameters();
				// start preview with new settings
				try {
					camera.setPreviewDisplay(holder);
					camera.startPreview();
					//hejianfeng add start for zendao12318
					Thread.sleep(500);
					isPreviewing=true;
					//hejianfeng add end for zendao12318
				} catch (Exception e){
					Log.d(TAG, "Error starting camera preview: " + e.getMessage());
				}
			}
		});
		//hejianfeng modif end for zendao 10260
	}

	public Camera getCameraInstance() {
		Camera c = null;
		try {
			int cameraCount = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras(); // get cameras number
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo); // get cameraInfo
				if ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)||isMT535_version) {
					try {
						c = Camera.open(camIdx);
					} catch (RuntimeException e) {
					}
				}
			}
			if (c == null) {
				Log.d("MMCAM","open front camera 1");
				c = Camera.open(1); // attempt to get a Camera instance
			}
		} catch (Exception e) {
		}
		return c;
	}

	public boolean isCameraAvalible(){
		try {
			int cameraCount = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras(); // get cameras number
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo); // get cameraInfo
				if ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)||isMT535_version) {
					Log.d("MMCAM","device have front camera");
					return true;
				}
			}

		} catch (Exception e) {
			Log.d("MMCAM","getFrontCam error:"+e.toString());
		}
		return false;
	}

	private void updateCameraParameters() {
		if (camera != null) {
			Camera.Parameters p = camera.getParameters();
			try {//add by wangjinfeng for bug 16484
				setParameters(p);
				camera.setParameters(p);
			} catch (Exception e) {
				try{
					LogUtil.d("CameraPreview_Front: Exception setParameters" );
					Camera.Size previewSize = findBestPreviewSize(p);
					p.setPreviewSize(previewSize.width,previewSize.height);
					p.setPictureSize(previewSize.width, previewSize.height);
					camera.setParameters(p);
				}catch (Exception ex){
					LogUtil.d("CameraPreview_Front: Exception" );
				}
			}
		}
	}

	private Camera.Size selectPreviewSize(Camera.Parameters parameters){
		List<Camera.Size> previewSizeList =  parameters.getSupportedPreviewSizes();
		if (previewSizeList.size() == 0){
			LogUtil.d("CameraPreview_Front: previewSizeList size is 0" );
			return null;

		}

		Camera.Size currentSelectSize = null;
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		int deviceWidth =displayMetrics.widthPixels;
		int deviceHeight = displayMetrics.heightPixels;
		double currentSelectSize_value = 0;
		if (mFrontCameraSwapWidthHeight) {
			deviceHeight =displayMetrics.widthPixels;
			deviceWidth = displayMetrics.heightPixels;
		}
		LogUtil.d("CameraPreview_Front: Front deviceWidth:" + deviceWidth + " deviceHeight： " + deviceHeight);
//		for (int i = 1; i < 41 ; i++){
//			for(Camera.Size itemSize : previewSizeList){
//				LogUtil.d("CameraPreview_Front: i ：" + i + "Front deviceWidth:" + itemSize.width + " deviceHeight： " + itemSize.height);
////                Log.e(TAG, "selectPreviewSize: itemSize w="+itemSize.width+"h"+itemSize.height);
//				if (itemSize.height > (deviceHeight - i*5) && itemSize.height < (deviceHeight + i*5)){
//					if (currentSelectSize != null){
//						if (Math.abs(deviceWidth-itemSize.width) < Math.abs(deviceWidth - currentSelectSize.width)){
//							currentSelectSize = itemSize;
//							continue;
//						}
//					}else {
//						currentSelectSize = itemSize;
//					}
//
//				}
//			}
//		}
		for(Camera.Size itemSize : previewSizeList){
			double x1 = itemSize.height - deviceHeight;
			double y1 = itemSize.width - deviceWidth;
			LogUtil.d("CameraPreview_Front: Front x1:" + x1 + " y1： " + y1);
			if(currentSelectSize != null){
				double temp = (Math.pow(x1,2.0)+Math.pow(y1,2.0));
				if(temp<currentSelectSize_value){
					currentSelectSize_value = temp;
					currentSelectSize = itemSize;
				}
			}else{
				currentSelectSize = itemSize;
				currentSelectSize_value = Math.pow(x1,2.0)+Math.pow(y1,2.0);
			}
		}
		LogUtil.d("CameraPreview_Front: Front ="+currentSelectSize.width+"h"+currentSelectSize.height);
		return currentSelectSize;
	}

	//add by wangjinfeng for bug 16484
	private void setParameters(Camera.Parameters p) throws Exception {
		List<String> focusModes = p.getSupportedFocusModes();
		if (focusModes
				.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}

		long time = new Date().getTime();
		p.setGpsTimestamp(time);
		p.setPictureFormat(PixelFormat.JPEG);
		//Camera.Size previewSize = findPreviewSizeByScreen(p);
		Camera.Size previewSize;
		if(TextUtils.isEmpty(previewSizeMethod) || previewSizeMethod.equals("0")){
			previewSize = selectPreviewSize(p);
		}else if( previewSizeMethod.equals("1")){
			previewSize = findBestPreviewSize(p);
		}else if( previewSizeMethod.equals("2")){
			previewSize = findPreviewSizeByScreen(p);
		}else if( previewSizeMethod.contains("x")){
			int sizeX, sizeY;
			try {
			    sizeX = Integer.parseInt(previewSizeMethod.substring(0,previewSizeMethod.indexOf("x")));
			    sizeY = Integer.parseInt(previewSizeMethod.substring(previewSizeMethod.indexOf("x")+1));
				Log.d(TAG,"AAAAAAAAA- sizeX:" + sizeX+ " sizeY: " + sizeY);
				previewSize = camera.new Size(sizeX, sizeY);
			} catch (NumberFormatException e) {
				Log.d(TAG,"NumberFormatException:xxxxx");
				previewSize = selectPreviewSize(p);
			}
		}else{
			previewSize = selectPreviewSize(p);
		}

		//p.setPreviewSize(1440,1080);
		Log.d(TAG,"AAAAAAAAA- previewSize.width:" + previewSize.width + " previewSize.height: " + previewSize.height);
		p.setPreviewSize(previewSize.width,previewSize.height);
		p.setPictureSize(previewSize.width, previewSize.height);
		//p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO
		boolean WIFI_BUILD = false;
		boolean WIFI_BUILD1 = false;
		boolean ERO_BUILD = false;
		boolean LA_BUILD = false;
		String wifi_path = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, Const.TEST_RIL_STATE);
		String wifi_path1 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, WIFI_mt535);
		if(((null != wifi_path) && !wifi_path.isEmpty())||((null != wifi_path1) && !wifi_path1.isEmpty())){
			WIFI_BUILD = FileUtil.readFromFile(wifi_path).contains("1");
			WIFI_BUILD1 = FileUtil.readFromFile(wifi_path1).contains("0");
			ERO_BUILD = FileUtil.readFromFile(wifi_path1).contains("1");
			LA_BUILD = FileUtil.readFromFile(wifi_path1).contains("2");
		}
		if(isMT535_version && LA_BUILD){
			p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
		}
		if (getContext().getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			Log.d(TAG, "AAAAAAAAA");
			camera.setDisplayOrientation(180);
			p.setRotation(180);
		}
		mFrontCameraRotation = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mFrontCameraRotationAngleConfig);
		if(!(mFrontCameraRotation == null) && !(mFrontCameraRotation.isEmpty())){
			camera.setDisplayOrientation(Integer.parseInt(mFrontCameraRotation));
			p.setRotation(Integer.parseInt(mFrontCameraRotation));
			Log.d("MM0415","mFrontCameraRotation:"+Integer.parseInt(mFrontCameraRotation));
		}else{


		}
		
	}

	public boolean takePicture() {
		if (camera != null) {
			try {
				synchronized (Thread.currentThread()) {
					camera.takePicture(null, null, pictureCallback);
					Thread.currentThread().wait(1000);
				}
				takePhoto=true;
			} catch (Exception e) {
				e.printStackTrace();
				takePhoto=false;
				Log.e(TAG,"e:"+e.toString());
			}
		}
		return takePhoto;
	}

	public void start() {
		if (camera != null) {
			try {
				camera.startPreview();
			} catch (Exception e){
				Log.d(TAG, "Error start camera preview: " + e.getMessage());
			}
		}
	}

	public void stop() {
		if (camera != null) {
			try {
			camera.stopPreview();
			} catch (Exception e){
				Log.d(TAG, "Error stop camera preview: " + e.getMessage());
			}
		}
	}

	public void destroy(){
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	public interface OnCameraStatusListener {
		void onCameraStopped(byte[] data);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		viewWidth = MeasureSpec.getSize(widthSpec);
		viewHeight = MeasureSpec.getSize(heightSpec);
		Log.d(TAG,"viewWidth"+viewWidth);
		Log.d(TAG,"viewHeight"+viewHeight);
		super.onMeasure(
				MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY));
	}

	private Camera.Size findPreviewSizeByScreen(Camera.Parameters parameters) {
		if (viewWidth != 0 && viewHeight != 0) {
			Log.d(TAG,"BBBBBBB");
			return camera.new Size(Math.max(viewWidth, viewHeight),
					Math.min(viewWidth, viewHeight));

		} else {
			Log.d(TAG,"CCCCCCCCC");
			return camera.new Size(Utils.getScreenWH(getContext()).heightPixels,
					Utils.getScreenWH(getContext()).widthPixels);
		}
	}

	private Camera.Size findBestPreviewSize(Camera.Parameters parameters) {

		String previewSizeValueString = null;
		previewSizeValueString = parameters.get("preview-size-values");

		if (previewSizeValueString == null) {
			previewSizeValueString = parameters.get("preview-size-value");
		}

		Log.d(TAG,"previewSizeValueString="+previewSizeValueString);

		if (previewSizeValueString == null) {
			return camera.new Size(Utils.getScreenWH(getContext()).widthPixels,
					Utils.getScreenWH(getContext()).heightPixels);
		}
		float bestX = 0;
		float bestY = 0;

		float tmpRadio = 0;
		float viewRadio = 0;

		if (viewWidth != 0 && viewHeight != 0) {
			viewRadio = Math.min((float) viewWidth, (float) viewHeight)
					/ Math.max((float) viewWidth, (float) viewHeight);
		}

		String[] COMMA_PATTERN = previewSizeValueString.split(",");
		for (String prewsizeString : COMMA_PATTERN) {
			prewsizeString = prewsizeString.trim();

			int dimPosition = prewsizeString.indexOf('x');
			if (dimPosition == -1) {
				continue;
			}

			float newX = 0;
			float newY = 0;

			try {
				newX = Float.parseFloat(prewsizeString.substring(0, dimPosition));
				newY = Float.parseFloat(prewsizeString.substring(dimPosition + 1));
			} catch (NumberFormatException e) {
				continue;
			}

			float radio = Math.min(newX, newY) / Math.max(newX, newY);
			if (tmpRadio == 0) {
				tmpRadio = radio;
				bestX = newX;
				bestY = newY;
			} else if (tmpRadio != 0 && (Math.abs(radio - viewRadio)) < (Math.abs(tmpRadio - viewRadio))) {
				tmpRadio = radio;
				bestX = newX;
				bestY = newY;
			}
		}

		Log.d(TAG,"viewWidth="+viewWidth+"  viewHeight="+viewHeight+"  bestX="+bestX+"  bestY="+bestY);
		if (bestX > 0 && bestY > 0) {
			return camera.new Size((int) bestX, (int) bestY);
		}
		return null;
	}


}
