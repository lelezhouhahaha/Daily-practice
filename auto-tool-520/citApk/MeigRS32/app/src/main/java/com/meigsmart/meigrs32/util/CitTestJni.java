package com.meigsmart.meigrs32.util;

public class CitTestJni{
    private String mPiccRspText = "";
    private String mIccRspText = "";
    private String mMsrRspText = "";
    private String mTrigerRspText = "";
    private String mSeVersion = "";
    private String mPsamRspText = "";

    static {
        System.loadLibrary("CitTestJni");
    }

    public String getPiccRspText() {
        return mPiccRspText;
    }

    public void setPiccRspText(String rspText) {
        mPiccRspText = rspText;
    }

    public String getIccRspText() {
        return mIccRspText;
    }

    public void setIccRspText(String rspText) {
        mIccRspText = rspText;
    }

    public String getMsrRspText() {
        return mMsrRspText;
    }

    public void setMsrRspText(String rspText) {
        mMsrRspText = rspText;
    }

    public String getPsamRspText() {
        return mPsamRspText;
    }

    public void setPsamRspText(String rspText) {
        mPsamRspText = rspText;
    }

    public String getTrigerRspText() {
        return mTrigerRspText;
    }

    public void setTrigerRspText(String rspText) {
        mTrigerRspText = rspText;
    }

    public String getSeVersion() {
        return mSeVersion;
    }

    public void setSeVersion(String rspText) {
        mSeVersion = rspText;
    }

    public native int testLed();
    public native int testPicc();
    public native int testIcc();
    public native int testMsr();
    public native int testPrinter();
    public native int testBeeper();
    public native int testTriger();
    public native int testSeVersion();
    public native int testPsam(int slot);

}