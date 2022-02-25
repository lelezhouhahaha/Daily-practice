package com.meigsmart.meigrs32.util;

import android.util.Log;

import com.meigsmart.meigrs32.log.LogUtil;

public class SerialPort {

    public boolean status = false;
    public boolean StatusTwice = false;

    public boolean isStatus() {
        return status;
    }

    public boolean isStatusTwice() {
        return StatusTwice;
    }

    public void setStatus(boolean status) {
        this.status = status;
        Log.d("SerialPort","SerialPort status = " +status);
    }

    public void setStatusTwice(boolean status) {
        this.StatusTwice = status;
        Log.d("SerialPort","SerialPort StatusTwice = " +status);
    }


	public native void test(String port, int baud, int[] req, int[] resp);
	public native void uhf_test(String port, int baud, String inputValue);
	public native void pin_test(String port, int baud, String inputValue);//for ninepin uart only
    public native void serialLoopTest(String port, int baud, String inputValue);
    public native int openSerial(int portno, String port, int baud);
    public native int readSerial(int portno);
    public native int writeSerial(int portno, char[] req);
    public native int closeSerial(int portno);

}
