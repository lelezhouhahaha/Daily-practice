package com.meigsmart.meigrs32.log;

import android.content.Context;
import android.util.Log;

import com.meigsmart.meigrs32.BuildConfig;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.util.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by chenMeng on 2018/4/20.
 */
public class LogUtil {
    private static final String TAG = "CITTEST";
    private static String className;            //所在的类名
    private static String methodName;            //所在的方法名
    private static int lineNumber;                //所在行号
    private static boolean isWriter;
    private static final String FILE_NAME_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    private static final String LOG_NAME_FORMAT = "yyyy-MM-dd HH:mm:ss:sss";
    private static String logPath = null;
    private static String logWifiPath = null;
    private static String logBtPath = null;

    private LogUtil() {
    }

    public static boolean isDebuggable() {
        return BuildConfig.DEBUG;
    }

    private static String createLog(String log) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(methodName);
        buffer.append(":");
        buffer.append(lineNumber);
        buffer.append("] ");
        buffer.append(log);
        return buffer.toString();
    }

    private static void getMethodNames(StackTraceElement[] sElements) {
        className = sElements[1].getFileName();
        methodName = sElements[1].getMethodName();
        lineNumber = sElements[1].getLineNumber();
    }

    public static void v(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.v(TAG, createLog(message));
        //writeToFile('e', className , message);
    }

    public static void v(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.v(tag, createLog(message));
        //writeToFile('v', tag , message);
    }

    public static void d(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.d(TAG, createLog(message));
        //writeToFile('d', className , message);
    }

    public static void _d(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.d(TAG, createLog(message));
        writeToFile('d', className , message);
    }

    public static void d(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.d(tag, createLog(message));
        //writeToFile('d', tag , message);
    }

    public static void wifi_d(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.d(tag, createLog(message));
        writeToWifiFile('d', tag , message);
    }

    public static void bt_d(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.d(tag, createLog(message));
        writeToBtFile('d', tag , message);
    }

    public static void i(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.i(TAG, createLog(message));
        //writeToFile('i', className , message);
    }

    public static void i(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
       // getMethodNames(new Throwable().getStackTrace());
        Log.i(tag, createLog(message));
        //writeToFile('i', tag , message);
    }

    public static void w(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.w(TAG, createLog(message));
        //writeToFile('w', className , message);
    }

    public static void w(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.w(tag, createLog(message));
        //writeToFile('w', tag , message);
    }

    public static void e(String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.e(TAG, createLog(message));
        //writeToFile('e', className , message);
    }

    public static void e(String tag, String message) {
        /*if (!isDebuggable()) {
            return;
        }*/
        //getMethodNames(new Throwable().getStackTrace());
        Log.e(tag, createLog(message));
        //writeToFile('e', tag , message);
    }




    public static final void initialize(Context appCtx, boolean isWriter) {
        String path = appCtx.getResources().getString(R.string.default_log_save_path);
        LogUtil.isWriter = isWriter;
        if (!LogUtil.isWriter) {//不保存日志到文件
            return;
        }
        File f_path = FileUtil.createRootDirectory(path);
        File file_path = FileUtil.mkDir(f_path);
        if(!file_path.exists()) {
            LogUtil.isWriter = false;
            return;
        }

        logPath = file_path + "/" + new SimpleDateFormat(FILE_NAME_FORMAT).format(System.currentTimeMillis()) + ".log";
        logWifiPath = file_path + "/wifi.log";
        logBtPath = file_path + "/bt.log";

    }


    /**
     * 将log信息写入文件中
     *
     * @param type
     * @param tag
     * @param msg
     */
    private static void writeToFile(char type, String tag, String msg) {

        if (null == logPath) {
            Log.e(TAG, "logPath == null ，未初始化LogToFile");
            return;
        }

        String log = new SimpleDateFormat(LOG_NAME_FORMAT).format(System.currentTimeMillis()) + " " + type + " " + tag + " " + msg + "\n";//log日志内容，可以自行定制


        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {
            fos = new FileOutputStream(logPath, true);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void writeToWifiFile(char type, String tag, String msg) {

        if (null == logWifiPath) {
            Log.e(TAG, "logPath == null ，未初始化LogToFile");
            return;
        }

        String log = new SimpleDateFormat(LOG_NAME_FORMAT).format(System.currentTimeMillis()) + " " + type + " " + tag + " " + msg + "\n";//log日志内容，可以自行定制


        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {
            fos = new FileOutputStream(logWifiPath, true);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private static void writeToBtFile(char type, String tag, String msg) {

        if (null == logBtPath) {
            Log.e(TAG, "logPath == null ，未初始化LogToFile");
            return;
        }

        String log = new SimpleDateFormat(LOG_NAME_FORMAT).format(System.currentTimeMillis()) + " " + type + " " + tag + " " + msg + "\n";//log日志内容，可以自行定制


        FileOutputStream fos = null;//FileOutputStream会自动调用底层的close()方法，不用关闭
        BufferedWriter bw = null;
        try {
            fos = new FileOutputStream(logBtPath, true);//这里的第二个参数代表追加还是覆盖，true为追加，flase为覆盖
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(log);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();//关闭缓冲流
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}
