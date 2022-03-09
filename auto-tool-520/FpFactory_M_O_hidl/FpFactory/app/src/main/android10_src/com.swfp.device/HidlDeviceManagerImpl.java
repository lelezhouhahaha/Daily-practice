package com.swfp.device;

import android.os.Handler;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.swfp.exception.SunwaveConnectException;
import com.swfp.utils.MessageType;
import com.swfp.utils.MyDataInputStream;
import com.swfp.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import vendor.sw.swfingerprint.V1_0.ISwfingerprint;
import vendor.sw.swfingerprint.V1_0.ISwfingerprintCallback;

import static android.R.attr.vendor;

public class HidlDeviceManagerImpl implements DeviceManager, IHwBinder.DeathRecipient{
	private static final String TAG = "sw-HidDeviceManagerImpl";

	public static final int FP_MSG_TEST_RESULT_ON_RETURN = 0x1234; //4660
	public static final int FP_MSG_TEST_RESULT_ON_CALLBACK = 0x4321; //17185
	private ISwfingerprint mDaemon;
	private MessageCallBack mCallBack;
	private Handler mHandler = new Handler();

	private byte[] mBuf = new byte[57600];
	private int mLen = 0;
	private int ret;
	private static Object mylock = new Object();

	public HidlDeviceManagerImpl() {
		getSwFingerprintDaemon();
	}
	
	public void getSwFingerprintDaemon() {
		if (mDaemon == null) {
			Log.v(TAG, "mDaemon was null, reconnect to fingerprint");
			try {
				mDaemon = ISwfingerprint.getService();
			} catch (java.util.NoSuchElementException e) {
				// Service doesn't exist or cannot be opened. Logged below.
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to get swfingerprint interface", e);
			}
			if (mDaemon == null) {
				Log.e(TAG, "ISwfingerprint HIDL not available");
				return;
			}
			mDaemon.asBinder().linkToDeath(this, 0);
			Log.d(TAG, "getSwFingerprintDaemon ok !");
		}
	}
	
	private void setServiceCallback(ISwfingerprintCallback callback) {
		try {
			getSwFingerprintDaemon();
			if(mDaemon!=null){
				mDaemon.setNotify(callback);
			}else{
				Log.e(TAG,"android10 setServiceCallback mDaemon is null");
			}
         } catch (java.util.NoSuchElementException e) {
            // Service doesn't exist or cannot be opened. Logged below.
        } catch (RemoteException e) {
            Log.i(TAG, "Failed to setCallback", e);
        }
	}
	
	@Override
	public void connect() throws SunwaveConnectException {
		Log.d(TAG, "android10 connect");
		setServiceCallback(mDaemonCallback);
	}

	@Override
	public void disConnect() {
		Log.d(TAG, "android10 disconnect");
        setServiceCallback(null);
	}

	@Override
	public void reset() {
		sendCmd(MessageType.FP_MSG_TEST_CMD_CANCEL, 0);
	}

	@Override
	public int getICSize(int[] w, int[] h) {
		int ret = -1;
        byte[] buf = new byte[8];
        int[] len = new int[1];
        len[0] = buf.length;
        ret = sendCmd(MessageType.FP_MSG_TEST_CMD_IC_SIZE, 0, buf, len);
        if (ret == 0) {
            if (len[0] != 8) {
                return -1;
            } else {
                int[] arr = Utils.byteArray2IntArray(buf, len[0]);
                w[0] = arr[0];
                h[0] = arr[1];
            }
        }
        return ret;
	}

	@Override
	public String getVersionInfo() {
		String ver = null;
        int ret = -1;
        byte[] buf = new byte[1024];
        int[] len = new int[1];
        len[0] = buf.length;
        ret = sendCmd(MessageType.FP_MSG_TEST_CMD_LIB_VER, 0, buf, len);
        if (ret == 0 && len[0] > 0) {
            ver = new String(buf, 0, len[0]);
        }
        return ver;
	}

	@Override
	public int waitLeave() {
		return 0;
	}

	@Override
	public synchronized int sendCmd(int cmd, int param, byte[] buf, int[] len) {
		Log.d(TAG, "sendCmd cmd: 0x" + Integer.toHexString(cmd));
		System.arraycopy(buf, 0, mBuf, 0, buf.length > mBuf.length ? mBuf.length : buf.length);
		mLen = 0;
		try {
			getSwFingerprintDaemon();
			if(mDaemon!=null){
				ret = mDaemon.sendCmd(cmd, param, len[0]);
				synchronized (mylock) {
					mylock.wait(300);
				}
			}else{
				Log.e(TAG,"android10 sendCmd mDaemon is null");
			}
		} catch (RemoteException e) {
			Log.e(TAG, "mDaemon failed to setNotify", e);
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			SystemClock.sleep(50);
		}
		//SystemClock.sleep(50);
		len[0] = mLen;
		System.arraycopy(mBuf, 0, buf, 0, buf.length > mBuf.length ? mBuf.length : buf.length);
		return ret;
	}

	@Override
	public synchronized int sendCmd(int cmd, int param) {
		byte[] buf = new byte[16];
        int[] len = {buf.length};
        return sendCmd(cmd, param, buf, len);
	}

	@Override
	public void registerCallBack(MessageCallBack callBack) {
		mCallBack = callBack;
	}

	@Override
	public void scanImage() {
		sendCmd(MessageType.FP_MSG_TEST_IMG_QUALITY, 0);
	}

	@Override
	public int readImage(byte[] img, int w, int h) {
		int[] rLen = {w * h};
        return sendCmd(MessageType.FP_MSG_TEST_READ_IMG, 0, img, rLen);
	}

	@Override
	public void serviceDied(long cookie) {
		Log.v(TAG, "swfingerprint HAL died");
		mDaemon = null;
	}

	private ISwfingerprintCallback mDaemonCallback = new ISwfingerprintCallback.Stub() {

		@Override
		public void onSendCmd(final int cmd, final int arg, final int len, ArrayList<Byte> data) {
			Log.d(TAG, "onSendCmd: cmd = " + cmd +"(0x"+Integer.toHexString(cmd)+")"+" arg = "+arg+" len = "+len);
			final byte[] buffer = new byte[data.size()];
			for (int i = 0; i < data.size(); i++) {
				buffer[i] = data.get(i);
			}
			if (arg == FP_MSG_TEST_RESULT_ON_RETURN) {
				handleOnReturn(cmd, len, buffer);
			} else if (arg == FP_MSG_TEST_RESULT_ON_CALLBACK) {
				handleOnCallback(cmd, len, buffer);
			}
		}

		private void handleOnReturn(int cmd, int len, byte[] buffer) {
			synchronized (mylock) {
				System.arraycopy(buffer, 0, mBuf, 0, len);
				mLen = len;
				mylock.notify();
			}
		}

		private void handleOnCallback(int cmd, int len, byte[] buffer) {
			MyDataInputStream dis = new MyDataInputStream(new ByteArrayInputStream(buffer));
			switch (cmd) {
				case MessageType.FP_MSG_TEST_RESULT_ENROLL:
					try {
						long deviceId = dis.readLong();
						int fingerId = dis.readInt();
						int groupId = dis.readInt();
						int remaining = dis.readInt();
						Log.d(TAG, "onEnroll deviceId: " + deviceId + " fingerId:" + fingerId + " groupId:" + groupId + " remaining:" + remaining);
//                        mFingerprintDaemonCallback.onEnrollResult(deviceId, fingerId, groupId, remaining * 100 / 15);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ENROLL, fingerId, remaining/* * 100 / 15*/);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent enroll data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST_RESULT_ON_ACQUIRED:
					try {
						long deviceId = dis.readLong();
						int acquiredInfo = dis.readInt();
						Log.d(TAG, "onAcquired deviceId: " + deviceId + " acquiredInfo:" + acquiredInfo);
//                        mFingerprintDaemonCallback.onAcquired(deviceId, acquiredInfo);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ON_ACQUIRED, acquiredInfo, 0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent acquired data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST_RESULT_ON_AUTHENTICATED:
					try {
						long deviceId = dis.readLong();
						int fingerId = dis.readInt();
						int groupId = dis.readInt();
						Log.d(TAG, "onAuthenticated deviceId: " + deviceId + " fingerId:" + fingerId + " groupId:" + groupId);
//                        mFingerprintDaemonCallback.onAuthenticated(deviceId, fingerId, groupId);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ON_AUTHENTICATED, (fingerId != 0 ? 1 : 0), fingerId);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent authenticated data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST_RESULT_ON_ERROR:
					try {
						long deviceId = dis.readLong();
						int error = dis.readInt();
						Log.d(TAG, "onError deviceId: " + deviceId + " error:" + error);
//                        mFingerprintDaemonCallback.onError(deviceId, error);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ON_ERROR, error, 0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent error data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST_RESULT_ON_REMOVED:
					try {
						long deviceId = dis.readLong();
						int fingerId = dis.readInt();
						int groupId = dis.readInt();
						Log.d(TAG, "onRemoved deviceId: " + deviceId + " fingerId:" + fingerId + " groupId:" + groupId);
//                        mFingerprintDaemonCallback.onRemoved(deviceId, fingerId, groupId);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ON_REMOVED, fingerId, 0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent removed data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST_RESULT_ON_ENUMERATE:
					try {
						long deviceId = dis.readLong();
						int fingerId = dis.readInt();
						int groupId = dis.readInt();
						Log.d(TAG, "onEnumerate: " + deviceId + " fingerId:" + fingerId + " groupId:" + groupId);
//                        mFingerprintDaemonCallback.onEnumerate(deviceId, fingerId, groupId);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST_RESULT_ON_ENUMERATE, fingerId, 0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent enumerate data is bad");
					}
					break;
				case MessageType.FP_MSG_TEST:
					try {
						long deviceId = dis.readLong();
						int mode = dis.readInt();
						int value = dis.readInt();
						Log.d(TAG, "fp_msg_test: deviceId: " + deviceId + " mode: 0x" + Integer.toHexString(mode) + " value:" + value);
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_TEST, mode, value);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent FP_MSG_TEST data is bad");
					}
					break;
				case MessageType.FP_MSG_FINGER:
					try {
						long deviceId = dis.readLong();
						int arg1 = dis.readInt();
						Log.d(TAG, "fp_msg_finger: deviceId: " + deviceId + " arg1: 0x" + Integer.toHexString(arg1));
						if (mCallBack != null) {
							mCallBack.handMessage(MessageType.FP_MSG_FINGER, arg1, 0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("onEvent FP_MSG_FINGER data is bad");
					}
					break;
				default:
					Log.e(TAG, "default: unknow onEvent eventId = 0x" + Integer.toHexString(cmd));
					break;
			}
		}
	};
}
