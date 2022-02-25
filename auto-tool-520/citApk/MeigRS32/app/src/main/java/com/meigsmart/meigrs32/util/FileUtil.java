package com.meigsmart.meigrs32.util;


import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Xml;

import com.meigsmart.meigrs32.log.LogUtil;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chenMeng on 2018/1/26.
 */

public class FileUtil {

    /**
     * 获取内置SD卡路径
     * @return
     */
    public static String getStoragePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * 获取内部data更目录
     * @return
     */
    private static String getSystemRoot(){
        return Environment.getDataDirectory().getPath();
    }

    private static String getDataFile(Context context){
        return context.getFilesDir().getAbsolutePath();
    }

    public static String createSDPath(String name){
        File file = new File(getStoragePath(),name);
        return file.getPath();
    }

    public static String createInnerPath(Context context,String name){
        File file = new File(getDataFile(context),name);
        return file.getPath();
    }

    public static String createInnerPath(Context context){
        File file = new File(getDataFile(context));
        return file.getPath();
    }


    /**
     * 读取文件的大小
     */

    public static long getFileSize(File f) {
        long l = 0;
        try {
            if (f.exists()) {
                FileInputStream is = new FileInputStream(f);
                l = is.available();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return l;
    }


    /**
     * 转换文件大小
     *
     * @param fileS
     * @return
     */
    public static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }


    /**
     * 获取sd卡目录  没有 就返回null
     * @param context
     * @param is_removale false 内置sd卡路径   true 外置sd卡路径
     * @return
     */
    public static String getSDPath(Context context, boolean is_removale) {
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建根目录 自动区分是否在sd卡 还是内部储存
     * @param fileName 根目录 名称
     * @return
     */
    public static File createRootDirectory(String fileName){
        String filePath =  Environment.getExternalStorageDirectory().toString() + "/";
        File file = new File(filePath +  fileName);
        file.mkdir();
        return file;
    }

    /**
     * 创建文件夹
     * @param filePath
     * @return
     */
    public static File createFolder(String filePath){
        File file = new File(filePath);
        if (file.exists()){
            return file;
        }else{
            file.mkdir();
            return file;
        }
    }

    public static File mkDir(File file) {
        if (file.getParentFile().exists()) {
            file.mkdir();
        } else {
            mkDir(file.getParentFile());
            file.mkdir();
        }
        return file;
    }


    /**
     * 保存写入的文件
     *
     * @param fileName
     * @param txt
     */
    public static String writeFile(File path, String fileName, String txt) {
        try {
            File file = new File(path, fileName);
            LogUtil.d("write file path:" + file.getPath());
            LogUtil.d("write file txt:" + txt);
            FileOutputStream fos = new FileOutputStream(file, false);//false每次写入都会替换内容
            byte[] b = txt.trim().getBytes();
            LogUtil.d("write file b.length:" + b.length);
            fos.write(b);
            fos.flush();
            fos.close();
            return file.getPath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String writeFileWithPermission(File path, String fileName, String txt, String perm) {
        try {
            String file = writeFile(path, fileName, txt);
            Runtime.getRuntime().exec("chmod " + perm + " " + file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readFile(String path) {
        return readFile(path.substring(0, path.lastIndexOf('/')), path.substring(path.lastIndexOf('/')+1));
    }
    /**
     * 读取文件的内容
     *
     * @param filePath 文件路径
     * @param fileName 文件名称
     * @return
     */
    public static String readFile(String filePath, String fileName) {
        String result = "";
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream bos = null;
        try {
            LogUtil.d("read file path:" + filePath + " filename:" + fileName);
            File file = new File(filePath, fileName);
            if (!file.exists()) {
                return null;
            }
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len = bis.read(b);
            while (len != -1) {
                bos.write(b, 0, len);
                len = bis.read(b);
                // LogUtil.d("read bytes: " + bytes2hex(b, len));
            }
            result = new String(bos.toByteArray());
            // LogUtil.d("read file data:" + result);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null)bos.close();
                if (bis != null)bis.close();
                if (fis != null)fis.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String parserXMLTag(String xmlFilePath, String tag){
        File configFile = new File(xmlFilePath);
        if (!configFile.exists()) {
            return "";
        }
        try {
            InputStream inputStream = new FileInputStream(xmlFilePath);
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(inputStream, "UTF-8");
            int type = xmlPullParser.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                switch (type) {
                    case XmlPullParser.START_TAG:
                        if(tag.equals(xmlPullParser.getName())) {
                            return xmlPullParser.nextText();
                        }
                        break;
                }
                type = xmlPullParser.next();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void writeToFile(final String path, final String value){
        try {
            LogUtil.d("writeToFile path:<" + path + "> value:<" +value+ ">.");
            File file = new File(path);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fRed = new FileOutputStream(path);
            fRed.write(value.getBytes());
            fRed.flush();
            fRed.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String bytes2hex(byte[] bytes, int size) {
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        byte b;
        //for (byte b : bytes) {
        for (int i = 0; i < size; i++) {
            b = bytes[i];
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

    public static String readFromFile(String path) {
        String result = "";
        try {
            int line;
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                result = sb.toString();
            }
            dis.close();
            fis.close();
        } catch (Exception e) {
        }
        return result;
    }

    /** 将null转换成空串 */
    public static String null2String(String str) {
        return str == null ? "" : str;
    }

    /**
     * 16进制字符串转byte[]
     *
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] hexString2Bytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return "".getBytes();
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (char2Byte(hexChars[pos]) << 4 | char2Byte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static int char2Byte(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }

    /**
     * Convert byte[] to hex string.将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
     *
     * @param src byte[] data
     * @return hex string
     */
    public static String bytes2HexString(byte... src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }

    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

}
