CameraDemo-2022-0705:--该app主要是实现使用camera2 接口来实现开启预览，停止预览功能，在后面的重启预览和停止预览中，分别使用了接口restartPreview()，stopPreview()
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
该app目前的缺陷是，没有在onDestroy的时候去关闭camera，释放camera资源。这一点需要后续晚上。