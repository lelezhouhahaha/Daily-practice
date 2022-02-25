package com.meigsmart.meigrs32.util;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import com.meigsmart.meigrs32.application.MyApplication;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author chenmeng
 * @Description 商米触发检测工具类，分不同量级进行提示
 * @date create at 2021年9月8日 下午4:19:07
 */
public class TamperUtil {

	public static long MASK_DYNAMIC1 =  0x000001;
	public static long MASK_DYNAMIC2 =  0x000002;
	public static long MASK_STAIC1 =  0x001004;
	public static long MASK_STAIC2 =  0x002004;
	public static long MASK_STAIC3 =  0x004008;
	public static long MASK_STAIC4 =  0x008008;
	public static long MASK_ROOTKEY_UNNORMAL=  0x000010;

	public static long CT_DYNAMIC1 =  0x000002;
	public static long CT_DYNAMIC2 =  0x000008;
	public static long CT_STAIC1 =  0x000010;
	public static long CT_STAIC2 =  0x000020;
	public static long CT_STAIC3 =  0x000040;
	public static long CT_STAIC4 =  0x000080;


	public static long MASK_ROOTKEY_ERROR =  0x000002;
	public static long MASK_ROOTKEY_LOST =  0x000001;
	public static long CONTACT_TRIGGER = 0x000000;

	public static String convert(String str) {
		StringBuilder builder = new StringBuilder(str);
		str = builder.reverse().toString();
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			if (i % 32 == 0) {
				if (i + 32 > str.length()) {
					stringBuilder.append(str.substring(i));
					break;
				}
				stringBuilder.append(str.substring(i, i + 32) + ",");
			}
		}
		str = stringBuilder.reverse().toString();
		if (str.charAt(0) == ',') {
			str = str.substring(1);
		}
		return str;
	}

	public static String strToAscii(String hexstr) {
		StringBuilder output = new StringBuilder();

		for (int i = 0; i < hexstr.length(); i+=2) {
			String str = hexstr.substring(i, i+2);

			output.append((char)Integer.parseInt(str, 16));

		}
		return output.toString();
	}

	public static void setClearlog(){
		try {
			MyApplication.getInstance().basicOptV2.setSysParam(AidlConstants.SysParam.TERM_STATUS, AidlConstantsV2.SysParam.CLEAR_TAMPER_LOG);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static String getTamperlog(){
		String log = "";
		try {
			log = MyApplication.getInstance().basicOptV2.getSysParam(AidlConstants.SysParam.TAMPER_LOG);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return log;
	}

	public static int getTamperState(){
		int status = -1;
		try {
			status = MyApplication.getInstance().securityOptV2.getSecStatus();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return status;
	}

	public static List<String> SplitString(String ppp) {
		List<String> result = new ArrayList<String>();
		String[] strs = ppp.split(",");
		for (int j = 0; j < strs.length; j++) {
			if(j!=0)
			result.add(strs[j]);
		}
		return result;
	}
}
