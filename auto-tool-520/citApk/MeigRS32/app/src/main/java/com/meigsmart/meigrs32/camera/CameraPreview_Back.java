package com.meigsmart.meigrs32.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.SystemProperties;
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


public class CameraPreview_Back extends SurfaceView implements
		SurfaceHolder.Callback {
	private static final String TAG = "CameraPreview_Back";
	private int viewWidth = 0;
	private int viewHeight = 0;
	private OnCameraStatusListener listener;
	private SurfaceHolder holder;
	private Camera camera = null;
    private boolean takePhoto=false;
	private String mRearCameraRotation = null;
	private String mRearCameraRotationAngleConfig = "common_rear_camera_rotation_angle";
	private final String DEVICE_NAME = "common_device_name_test";
	private final String CAMERA_SWAP_WIDTH_HEIGHT = "commom_camera_swap_width_height";
	private String TAG_mt535 = "common_keyboard_test_bool_config";
	boolean is_mt535 = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, TAG_mt535).equals("true");
	private boolean mCameraSwapWidthHeight;
	public boolean isPreviewing=false;//hejianfeng add for zendao12318
	private String previewSizeMethod ="0";
	private final String CAMERA_PREVIEW_SIZE_METHOD = "commom_back_camera_previewsize_method";
	private String projectName = "";
	String face_select="";
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

	public CameraPreview_Back(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		projectName = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CustomConfig.SUNMI_PROJECT_MARK);

		mCameraSwapWidthHeight = "true".equals(DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CAMERA_SWAP_WIDTH_HEIGHT));
		previewSizeMethod = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, CAMERA_PREVIEW_SIZE_METHOD);
		face_select = FileUtil.readFromFile("/mnt/vendor/productinfo/cit/face_select");
	}


	public void surfaceCreated(SurfaceHolder holder) {
		if(!Utils.checkCameraHardware(getContext())) {
			return;
		}
		if(camera == null)
			camera = getCameraInstance();
		//hejianfeng modif start for zendao 10260
		MyApplication.getInstance().getSingleThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				updateCameraParameters();
			}
		});
		//hejianfeng modif end for zendao 10260
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
		/*if(camera !=null) {
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
				LogUtil.d("CameraPreview_Back: cout:" + cameraCount + " id:" + camIdx + " cameraInfo.facing:" + cameraInfo.facing);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					if("MT537".equals(projectName)) {
						try {
							if((face_select!=null)&&(!face_select.equals(""))&&(face_select.length()>=5)) {
								if (!face_select.substring(3, 4).equals("1")) {
									return Camera.open(camIdx);
								} else {
									return Camera.open(0);
								}
							}
						} catch (RuntimeException e) {
						}
					}else{
						try {
							Log.d("MMCAM","open back camera");
							return Camera.open(camIdx);
						} catch (RuntimeException e) {
						}
					}
				}else{
					try {
						Log.d("MMCAM","open back camera 0");
						return Camera.open(0);
					} catch (RuntimeException e) {
					}
				}
			}
			// if (c == null) {
			//     c = Camera.open(0); // attempt to get a Camera instance
			// }
		} catch (Exception e) {
			Log.d("MMCAM","getBackCam error:");
		}
		Log.e(TAG,"c1111:"+c);
		return c;
	}
	
	public boolean isCameraAvalible(){
		try {
			int cameraCount = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras(); // get cameras number
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo); // get cameraInfo
				if ((cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
					Log.d("MMCAM","device have back camera");
					return true;
				}
			}

		} catch (Exception e) {
			Log.d("MMCAM","getBackCam error:"+e.toString());
		}
		return false;
	}

	private void updateCameraParameters() {
		if (camera != null) {
			Camera.Parameters p = camera.getParameters();
			try {
				//add by wangjinfeng for bug 16484
				setParameters(p);
				camera.setParameters(p);
			} catch (Exception e) {
				try{
					LogUtil.d("CameraPreview_Back: Exception setParameters");
					Camera.Size previewSize = findBestPreviewSize(p);
					p.setPreviewSize(previewSize.width, previewSize.height);
					p.setPictureSize(previewSize.width, previewSize.height);
					camera.setParameters(p);
				}catch (Exception ex){
					LogUtil.d("CameraPreview_Back: Exception");
				}
			}
		}
	}
	private Camera.Size selectPreviewSize(Camera.Parameters parameters){
		List<Camera.Size> previewSizeList =  parameters.getSupportedPreviewSizes();
		if (previewSizeList.size() == 0){
			LogUtil.d("CameraPreview_Back: previewSizeList size is 0" );
			return null;

		}

		Camera.Size currentSelectSize = null;
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		int deviceWidth =displayMetrics.widthPixels;
		int deviceHeight = displayMetrics.heightPixels;
		double currentSelectSize_value = 0;
		if (mCameraSwapWidthHeight) {
			deviceHeight =displayMetrics.widthPixels;
			deviceWidth = displayMetrics.heightPixels;
		}
		LogUtil.d("CameraPreview_Back: Back deviceWidth:" + deviceWidth + " deviceHeight： " + deviceHeight);
//		for (int i = 1; i < 41 ; i++){
//			for(Camera.Size itemSize : previewSizeList){
//				LogUtil.d("CameraPreview_Back: i ：" + i + "Back deviceWidth:" + itemSize.width + " deviceHeight： " + itemSize.height);
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
			LogUtil.d("CameraPreview_Back: Back itemSize.width:" + itemSize.width + " itemSize.height： " + itemSize.height);
			LogUtil.d("CameraPreview_Back: Back x1:" + x1 + " y1： " + y1);
			if(currentSelectSize != null){
				double temp = (Math.abs(x1)+Math.abs(y1));
				if(temp<currentSelectSize_value){
					currentSelectSize_value = temp;
					currentSelectSize = itemSize;
				}
			}else{
				currentSelectSize = itemSize;
				currentSelectSize_value = Math.abs(x1)+Math.abs(y1);
			}
		}
		LogUtil.d("CameraPreview_Back: Back width ="+currentSelectSize.width+" height ="+currentSelectSize.height);
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
		Camera.Size previewSize;
		if(is_mt535)
		{
			previewSize = findPreviewSizeByScreen(p);
		}else{
			if(previewSizeMethod.equals("1")){
				previewSize = findBestPreviewSize(p);
			}else{
				previewSize = selectPreviewSize(p);
			}
		}
		Log.d(TAG," previewSize.width:" + previewSize.width + " previewSize.height: " + previewSize.height);
		p.setPreviewSize(previewSize.width, previewSize.height);
		p.setPictureSize(previewSize.width, previewSize.height);
        //p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//add by wangqinghao zd4338
		if (getContext().getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			camera.setDisplayOrientation(90);
			p.setRotation(90);
		}
		mRearCameraRotation = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mRearCameraRotationAngleConfig);
		if("MT537".equals(projectName)) {
			if((face_select!=null)&&!(face_select.equals(""))&&face_select.length()>=5){
			if (!face_select.substring(3,4).equals("1")) {
				if (!(mRearCameraRotation == null) && !(mRearCameraRotation.isEmpty())) {
					camera.setDisplayOrientation(Integer.parseInt(mRearCameraRotation));
					p.setRotation(Integer.parseInt(mRearCameraRotation));
					Log.d("MM0415", "mRearCameraRotation:" + Integer.parseInt(mRearCameraRotation));
				} else {

				}
			} else {
				camera.setDisplayOrientation(90);
				p.setRotation(90);
				Log.d("MM0415", "mRearCameraRotation face_select camera_rear");
			}
			}
		}else{
			if (!(mRearCameraRotation == null) && !(mRearCameraRotation.isEmpty())) {
				camera.setDisplayOrientation(Integer.parseInt(mRearCameraRotation));
				p.setRotation(Integer.parseInt(mRearCameraRotation));
				Log.d("MM0415", "mRearCameraRotation:" + Integer.parseInt(mRearCameraRotation));
			}
		}


	}

    public void autoFocus(){
        MyApplication.getInstance().getSingleThreadPool().execute(new Runnable() {
            @Override
            public void run() {
            	try {//add by wangjinfeng for bug 16484
					camera.autoFocus(new Camera.AutoFocusCallback() {
						@Override
						public void onAutoFocus(boolean success, Camera camera) {

						}
					});
				}catch (Exception e){
            		LogUtil.e(TAG,"autoFocus fialed");
				}
            }
        });
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
		super.onMeasure(
				MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY));
	}

	private Camera.Size findPreviewSizeByScreen(Camera.Parameters parameters) {
		if (viewWidth != 0 && viewHeight != 0) {
			return camera.new Size(Math.max(viewWidth, viewHeight),
					Math.min(viewWidth, viewHeight));
		} else {
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

		if (bestX > 0 && bestY > 0) {
			return camera.new Size((int) bestX, (int) bestY);
		}
		return null;
	}


}

