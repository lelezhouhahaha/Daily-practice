package com.sunmi.scannercitmmi1.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class LicenseFileUtils {

    private static final String TAG = "file_utils";

    public static String readScanDeviceInfoFromFile(String path) {
        File file = new File(path);
        if ((file != null) && file.exists()) {
            try {
                FileInputStream fin = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                String value = reader.readLine();
                Log.i(TAG, "-ReadFromFile-value=" + value);
                fin.close();
                return value;
            } catch (IOException e) {
                Log.i(TAG, "read error:" + e);
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean deleteLicenseFile(String rootPath, String fileName) {
        File licenseFile = new File(rootPath + fileName);
        if (licenseFile.exists()) {
            Log.d(TAG, "deleteLicenseFile: " + licenseFile.toString() + " licenseFile.exists");
        }
        if (licenseFile.canRead()) {
            Log.d(TAG, "deleteLicenseFile: " + licenseFile.toString() + " licenseFile.canRead ");
        }
        if (licenseFile.exists() && licenseFile.canRead()) {
            licenseFile.delete();
        }
        return true;
    }


    public static boolean grantPermission(String pathName, boolean isDir) {
        try {
            //Log.d(TAG, "grantPermission: pathName = " + pathName);
            File oldFile = new File(pathName);
            if (!oldFile.exists()) {
                Log.d(TAG, "grantPermission: old file is not exist");
                return false;
            } else if (!isDir && !oldFile.isFile()) {
                Log.d(TAG, "grantPermission: old file is not file");
                return false;
            } else if (!oldFile.canRead()) {
                Log.d(TAG, "grantPermission: old file cannot read");
                return false;
            }
            File newFile = new File(pathName);
            newFile.setReadable(true, false);
            newFile.setWritable(true, false);
            newFile.setExecutable(true, false);
            Log.d(TAG, "grantPermission: permission success "+ pathName);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "grantPermission: copy fail");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean initFileDirs(String fileDir) {

        File dirPath = new File(fileDir);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        grantPermission(fileDir, true);

        return true;
    }
}
