该diff文件主要是在CameraService中添加新的API，目前调用openMeigScanDevice接口，可以让预览界面出图像。
其中app调用的用法是：
private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            setupCamera(width, height);
            Log.d(TAG, "mTextureListener openCamera start");
            //openCamera();
            if(mCameraManager != null){
                try {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "zll1 openCamera checkSelfPermission fail");
                        return;
                    }
                    Log.d(TAG, "zll1 openCamera 2 mCameraId: 0");
                    Surface previewSurface = new Surface(surface);
                    OutputConfiguration output = new OutputConfiguration(previewSurface);
                    mCameraManager.openMeigScanForUid("0", mStateCallback, CameraDeviceImpl.checkAndWrapHandler(mCameraHandler), -1, output);
                    Log.d(TAG, "zll1 openCamera 2 mCameraId: 1");
                    
                } catch (CameraAccessException e) {
                    Log.d(TAG, "zll1 openCamera 3");
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "mTextureListener openCamera end");
        }
previewSurface是来自于onSurfaceTextureAvailable函数中的surface
		