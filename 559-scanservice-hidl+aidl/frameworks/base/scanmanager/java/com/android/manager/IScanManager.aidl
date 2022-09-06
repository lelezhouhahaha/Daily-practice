package com.android.manager;
import com.android.manager.IMeigScanServiceListener;
/**
 * System private API for test.
 *
 * {@hide}
 */
interface IScanManager {
	void Open(int cameraId, int width, int height, int format);
	void Close(int CameraId);
	void Resume(int CameraId);
	void Suspend(int CameraId);
	void Capture(int CameraId);
	void SetParameters(int CameraId,int type,int value);
	void MoveFocus(int CameraId,float value);
    void registerListener(IMeigScanServiceListener listener);
    void unregisterListener(IMeigScanServiceListener listener);
}
