package com.meigsmart.meigrs32.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.DataUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private static final String TAG = "CameraPreview";
	private int viewWidth = 0;
	private int viewHeight = 0;
	private OnCameraStatusListener listener;
	private SurfaceHolder holder;
	private Camera camera;
    private boolean takePhoto=false;
	private String mRearCameraRotation = null;
	private String mRearCameraRotationAngleConfig = "common_backcamera_rotation_config";
	private int mCameraId;
	private boolean mIsAutoFocus;
	private int mCamerAngle;

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

	public CameraPreview(Context context, int cameraId, boolean isAutoFocus, int camerAngle) {
		super(context);
		mCameraId = cameraId;
		mIsAutoFocus = isAutoFocus;
		mCamerAngle= camerAngle;
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}


	public void surfaceCreated(SurfaceHolder holder) {
		if(!Utils.checkCameraHardware(getContext())) {
			return;
		}
		camera = getCameraInstance(mCameraId);
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
		camera.release();
		camera = null;
	}

	public void surfaceChanged(final SurfaceHolder holder, int format, int w,
                               int h) {
		// stop preview before making changes
		try {
			camera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or
		// reformatting changes here
		updateCameraParameters();
		// start preview with new settings
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();

		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}

	public Camera getCameraInstance(int cameraId) {
		Camera c = null;
		ArrayList<Integer> camIds = new ArrayList<Integer>();
		try {
			int cameraCount = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras(); // get cameras number
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo); // get cameraInfo
				LogUtil.d("CameraPreview: id:" + camIdx + " cameraInfo.facing:" + cameraInfo.facing);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					camIds.add(camIdx);
				}
			}
			if (cameraId > camIds.size())
				return null;
			LogUtil.d("CameraPreview: id:" + cameraId + " cameraId:" + camIds.get(cameraId));
			try {
				c = Camera.open(camIds.get(cameraId));
			} catch (RuntimeException e) {
			}
			// if (c == null) {
			//     c = Camera.open(0); // attempt to get a Camera instance
			// }
		} catch (Exception e) {
		}
		Log.e(TAG,"c1111:"+c);
		return c;
	}

	private void updateCameraParameters() {
		if (camera != null) {
			Camera.Parameters p = camera.getParameters();
			try {//add by wangjinfeng for bug 16484
				setParameters(p);
				camera.setParameters(p);
			} catch (Exception e) {
				Camera.Size previewSize = findBestPreviewSize(p);
				p.setPreviewSize(previewSize.width, previewSize.height);
				p.setPictureSize(previewSize.width, previewSize.height);
				camera.setParameters(p);
			}
		}
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
		Camera.Size previewSize = findPreviewSizeByScreen(p);
		p.setPreviewSize(previewSize.width, previewSize.height);
		p.setPictureSize(previewSize.width, previewSize.height);
		if(mIsAutoFocus)
			p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		if (getContext().getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			camera.setDisplayOrientation(90);
			p.setRotation(90);
		}
		
		camera.setDisplayOrientation(mCamerAngle);
		p.setRotation(mCamerAngle);

		/*mRearCameraRotation = DataUtil.initConfig(Const.CIT_COMMON_CONFIG_PATH, mRearCameraRotationAngleConfig);
		if(!(mRearCameraRotation == null) && !(mRearCameraRotation.isEmpty())){
			camera.setDisplayOrientation(Integer.parseInt(mRearCameraRotation));
			p.setRotation(Integer.parseInt(mRearCameraRotation));
			Log.d("MM0415","mRearCameraRotation:"+Integer.parseInt(mRearCameraRotation));
		}else{


		}*/


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
			camera.startPreview();
		}
	}

	public void stop() {
		if (camera != null) {
			camera.stopPreview();
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
