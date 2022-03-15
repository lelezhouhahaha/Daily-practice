package com.meigsmart.meigrs32.util;

public class DiagCommand {
    public static final int FTM_SUBCMD_BASE = 1000;
    public static final int FTM_SUBCMD_QUERY_BASE = 3000;
    public static final int FTM_SUBCMD_SET_RESULT_BASE = 5000;
    public static final int FTM_SUBCMD_START = 0;
    public static final int FTM_SUBCMD_END = 1;
    public static final int FTM_SUBCMD_SOFTWAREINFO = 2;
    public static final int FTM_SUBCMD_SIMCARD = 3;
    public static final int FTM_SUBCMD_TCARD = 4;
    public static final int FTM_SUBCMD_BLUETOOTH = 5;
    public static final int FTM_SUBCMD_WIFI = 6;
    public static final int FTM_SUBCMD_GPS = 7;
    public static final int FTM_SUBCMD_GSENSOR = 8;
    public static final int FTM_SUBCMD_COMPASS = 9;
    public static final int FTM_SUBCMD_GYROSCOPE = 10;
    public static final int FTM_SUBCMD_LIGHTSENSOR = 11;
    public static final int FTM_SUBCMD_PROXIMITYSENSOR = 12;
    public static final int FTM_SUBCMD_AIRPRESSURESENSOR = 13;
    public static final int FTM_SUBCMD_KEYS = 14;
    public static final int FTM_SUBCMD_HALL = 15;
    public static final int FTM_SUBCMD_NFC = 16;
    public static final int FTM_SUBCMD_SCANER = 17;
    public static final int FTM_SUBCMD_SCANER_NEAR = 18;
    public static final int FTM_SUBCMD_SCANER_FAR = 19;
    public static final int FTM_SUBCMD_FINGERPRINT = 20;
    public static final int FTM_SUBCMD_BATTERYCHARGER = 21;
    public static final int FTM_SUBCMD_STROBE = 22;
    public static final int FTM_SUBCMD_VIBRATOR = 23;
    public static final int FTM_SUBCMD_FRONTCAMERA = 24;
    public static final int FTM_SUBCMD_BACKCAMERA = 25;
    public static final int FTM_SUBCMD_TOUCH = 26;

    public static final int FTM_SUBCMD_HEADSET = 100;
    public static final int FTM_SUBCMD_HEADSETLEFTLOOP = 101;
    public static final int FTM_SUBCMD_HEADSETRIGHTLOOP = 102;

    public static final int FTM_SUBCMD_LCM = 120;
    public static final int FTM_SUBCMD_BACKLIGHT150 = 121;
    public static final int FTM_SUBCMD_BACKLIGHT100 = 122;
    public static final int FTM_SUBCMD_BACKLIGHT50 = 123;

    public static final int FTM_SUBCMD_LED = 130;
    public static final int FTM_SUBCMD_LEDRED = 131;
    public static final int FTM_SUBCMD_LEDGREEN = 132;
    public static final int FTM_SUBCMD_LEDBLUE = 133;

    public static final int FTM_SUBCMD_LCD = 140;
    public static final int FTM_SUBCMD_LCDRED = 141;
    public static final int FTM_SUBCMD_LCDGREEN = 142;
    public static final int FTM_SUBCMD_LCDBLUE = 143;
    public static final int FTM_SUBCMD_LCDGRAY = 144;
    public static final int FTM_SUBCMD_LCDBLACK = 145;
    public static final int FTM_SUBCMD_LCDWHITE = 146;


    public static final int FTM_SUBCMD_MAINMIC = 500;
    public static final int FTM_SUBCMD_SUBMIC = 501;
    public static final int FTM_SUBCMD_MICLOOP = 502;
    public static final int FTM_SUBCMD_SPEAKER = 503;
    public static final int FTM_SUBCMD_RECEIVER = 504;
    public static final int FTM_SUBCMD_CTYPECOTG = 505;
    public static final int FTM_SUBCMD_POGOPINCHARGER2 = 506;
    public static final int FTM_SUBCMD_POGOPINCHARGER8 = 507;
    public static final int FTM_SUBCMD_POGOPINOTG = 508;
    public static final int FTM_SUBCMD_BATTERYSWITCHING = 509;
    public static final int FTM_SUBCMD_UART = 510;
    public static final int FTM_SUBCMD_I2C = 511;

    public static final int FTM_SUBCMD_MAX = 2000;

   //1002~2000: PC send command and software can judge result by self.
    //2001~4000:PC query result
    //4001~6000: set result to software.


    public static final String FTM_SUBCMD_CMD_KEY = "diag_command";
    public static final String FTM_SUBCMD_RESULT_KEY = "diag_result";
    public static final String FTM_SUBCMD_DATA_KEY = "diag_data";
    public static final String FTM_SUBCMD_DATA_SIZE_KEY = "diag_data_size";

    public final static int SERVICEID = 0x0001; //server
    public final static int ACK_SERVICEID = 0X0002; //ack_server
    public final static int ACTIVITYID = 0X0003; //client
    public final static int ACK_ACTIVITYID = 0X0004; //ack_client
    public final static int SAY_HELLO = 0x0005; //server only for handshark
    public final static int ACK_SAY_HELLO = 0X0006; //client only for handshark
    public final static int SERVICEID_SET_RESULT = 0X0007; //
    public final static int ACK_SERVICEID_SET_RESULT = 0X0008; //
}
