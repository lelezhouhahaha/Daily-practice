package com.test.camerademo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
/*import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
 */
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
//import com.test.camerademo.PermissionUtils;

public class MainActivity extends Activity {
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    private String mFolderPath;
    private File mFile;
    public LinearLayoutCompat mSettings;
    public Context mContext;
    String[] mResolutionRatio = new String[] {"1280*720", "1280*960", "1920*1080", "2408*1536", "1600*2500", "1280*960", "2560*1920", "3024*2016", "4080*2720", "4536*3024"};
    List<String> mResolutionList;
    public int mRefreshWidth = 2048;
    public int mRefreshHeight = 1536;
    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String mCameraId;
    private Size mPreviewSize;
    private Size mCaptureSize;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private MyHandler myHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private Button mStartPreviewButton;
    private Button mStopPreviewButton;
    private Button mCaptureButton;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder = null;
    private CaptureRequest mCaptureRequest = null;
    private CameraCaptureSession mCameraCaptureSession;
    private final String TAG = this.getClass().getSimpleName() + " zll";

    /**
     * 权限列表
     */
    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };

    //多个权限请求Code
    private final int REQUEST_CODE_PERMISSIONS = 2;

    public void requestPermissionsSuccessCallback() {
            myHandler.sendEmptyMessage(1000);
    }

    private void requestPermissions() {
        PermissionUtils.checkAndRequestMorePermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS, new PermissionUtils.PermissionRequestSuccessCallBack() {
            @Override
            public void onHasPermission() {
                //权限申请回调
                requestPermissionsSuccessCallback();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS:
                PermissionUtils.onRequestMorePermissionsResult(this, PERMISSIONS, new PermissionUtils.PermissionCheckCallBack() {
                    @Override
                    public void onHasPermission() {
                        requestPermissionsSuccessCallback();
                    }

                    @Override
                    public void onUserHasAlreadyTurnedDown(String... permission) {
                    }

                    @Override
                    public void onUserHasAlreadyTurnedDownAndDontAsk(String... permission) {
                    }
                });
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case 1000:
                Log.d(TAG, "zll 1000 openCamera start");
                openCamera();
                Log.d(TAG, "zll 1000 openCamera end");
                break;
        }
    }

}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        myHandler = new MyHandler();
        requestPermissions();
        mResolutionList = Arrays.asList(mResolutionRatio);
        Log.d(TAG, "zll onCreate mResolutionList:" + mResolutionList.toString());
        //全屏无状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mStartPreviewButton = (Button) findViewById(R.id.startPreviewButton);
        mStopPreviewButton = (Button) findViewById(R.id.stopPreviewButton);
        mCaptureButton = (Button) findViewById(R.id.photoButton);
        mStartPreviewButton.setVisibility(View.GONE);
        mSettings = (LinearLayoutCompat) findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setItems(R.array.setting_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "zll i:" + i);
                        switch (i) {
                            case 0:
                            case 1:
                            default:
                                Log.d(TAG, "zll 1 i:" + i);
                                String mStr = mResolutionList.get(i);
                                Log.d(TAG, "zll 1 mStr:" + mStr);
                                String[] mAll=mStr.split("\\*");
                                Log.d(TAG, "zll 1 mAll.length:" + mAll.length);
                                for(int j = 0; j < mAll.length; j++) {
                                    Log.d(TAG, "zll 1 mAll[" + j + "]:" + mAll[j]);
                                }
                                mRefreshWidth = Integer.parseInt(mAll[0]);
                                mRefreshHeight = Integer.parseInt(mAll[1]);
                                setupImageReader();
                                Log.d(TAG, "zll 1 mRefreshWidth:" + mRefreshWidth);
                                Log.d(TAG, "zll 1 mRefreshHeight:" + mRefreshHeight);
                                break;
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "zll onResume");
        startCameraThread();
        if (!mTextureView.isAvailable()) {
        mTextureView.setSurfaceTextureListener(mTextureListener);
        }
        super.onResume();
    }

    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCamera(width, height);
            /*Log.d(TAG, "mTextureListener openCamera start");
            openCamera();
            Log.d(TAG, "mTextureListener openCamera end");*/
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void setupCamera(int width, int height) {
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //此处默认打开后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //根据TextureView的尺寸设置预览尺寸
                Log.d(TAG, "zll width:" + width);
                Log.d(TAG, "zll height:" + height);
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                //获取相机支持的最大拍照尺寸
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getHeight() * rhs.getWidth());
                    }
                });
                //此ImageReader用于拍照所需
                Log.d(TAG, "zll 2 width:" + mCaptureSize.getWidth());
                Log.d(TAG, "zll 2 height:" + mCaptureSize.getHeight());
                setupImageReader();
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }


    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "openCamera 1");
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "openCamera checkSelfPermission fail");
                return;
            }
            Log.d(TAG, "openCamera 2");
            manager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "openCamera 3");
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            Log.d(TAG, "onOpened startPreview start");
            CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                Log.d(TAG, "zll sizes.length:" + sizes.length);
                for (int i = 0; i < sizes.length; i++) { //遍历所有Size
                    Size itemSize = sizes[i];
                    if(itemSize == null){
                        break;
                    }
                    Log.e(TAG, "当前itemSize 宽=" + itemSize.getWidth() + "高=" + itemSize.getHeight());
                }

            } catch (CameraAccessException e){
                Log.d(TAG, "zll CameraAccessException");
            }


            startPreview();
            Log.d(TAG, "onOpened startPreview end");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        //mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //mSurfaceTexture.setDefaultBufferSize(320, 240);
        //mSurfaceTexture.setDefaultBufferSize(240, 320);
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        Log.d(TAG, "zll mPreviewSize.getWidth():" + mPreviewSize.getWidth());
        Log.d(TAG, "zll mPreviewSize.getHeight():" + mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            //mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        try{
            mCameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void restartPreview() {
        try{
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture_onClick(View view) {
        Log.d(TAG, "takePicture start");
        lockFocus();
        Log.d(TAG, "takePicture end");
    }

    public void startPreview_onClick(View view){
        Log.d(TAG, "startPreview start");
        mStartPreviewButton.setVisibility(View.GONE);
        if( (mCaptureRequest != null) && ( mCameraCaptureSession != null )) {
            restartPreview();
        }else{
            startPreview();
        }
        Log.d(TAG, "startPreview end");
    }

    public void stopPreview_onClick(View view){
        Log.d(TAG, "stopPreview start");
        mStartPreviewButton.setVisibility(View.VISIBLE);
        stopPreview();
        Log.d(TAG, "stopPreview end");
    }

    public void setFolderPath(String path) {
        mFolderPath = path;
        File mFolder = new File(path);
        if (!mFolder.exists()) {
            mFolder.mkdirs();
            Log.d(TAG, "文件夹不存在去创建");
        } else {
            Log.d(TAG, "文件夹已创建");
        }
    }

    private String getNowDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    private void lockFocus() {
        String BASE_PATH = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
        setFolderPath(BASE_PATH);
        mFile = new File(mFolderPath + "/" + getNowDate() + ".jpg");
        //mFile = new File(getFolderPath() + "/" + getNowDate() + ".jpg");
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            capture();
        }
    };

    private void capture() {
        try {
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Toast.makeText(getApplicationContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
                    unLockFocus();
                }
            };
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(mCaptureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null, mCameraHandler);
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "zll onPause");
        super.onPause();
        /*if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }*/
    }

    private void setupImageReader() {
        //2代表ImageReader中最多可以获取两帧图像流
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        //mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
        //        ImageFormat.JPEG, 2);

        //mImageReader = ImageReader.newInstance(320, 240,
        //        ImageFormat.JPEG, 2);
        //mImageReader = ImageReader.newInstance(1536, 2048,
        //        ImageFormat.JPEG, 2);
        Log.d(TAG, "zll setupImageReader mRefreshHeight:" + mRefreshHeight);
        Log.d(TAG, "zll setupImageReader mRefreshWidth:" + mRefreshWidth);
        mImageReader = ImageReader.newInstance(mRefreshHeight, mRefreshWidth,
                ImageFormat.JPEG, 2);
        Log.d(TAG, "zll setupImageReader mCaptureSize.getWidth():" + mCaptureSize.getWidth());
        Log.d(TAG, "zll setupImageReader mCaptureSize.getHeight():" + mCaptureSize.getHeight());
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "zll setupImageReader onImageAvailable");
                mCameraHandler.post(new imageSaver(reader.acquireNextImage(), mFile));
            }
        }, mCameraHandler);
    }

    public static class imageSaver implements Runnable {

        private Image mImage;
        private File mFile;

        public imageSaver(Image image, File file) {
            Log.d("imageSaver", "zll onImageAvailable");
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            /*
            String path = Environment.getExternalStorageDirectory() + "/DCIM/CameraV2/";
            Log.d("imageSaver", "zll path:" + path);
            File mImageFile = new File(path);
            if (!mImageFile.exists()) {
                Log.d("imageSaver", "zll 1");
                mImageFile.mkdir();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = path + "IMG_" + timeStamp + ".jpg";*/
            /*FileOutputStream fos = null;
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Log.d("imageSaver", "zll data:" + buffer.toString())
            try {
                fos = new FileOutputStream(mFile);
                fos.write(data, 0, data.length);
                Log.d("imageSaver", "zll 2");
            } catch (IOException e) {
                Log.d("imageSaver", "zll 3");
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }*/

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            Log.d("", "zll data:" + bytes.toString());
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}